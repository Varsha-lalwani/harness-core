/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.timescaledb.data;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.beans.ExecutionStatus;
import io.harness.migrations.TimeScaleDBDataMigration;
import io.harness.persistence.HIterator;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.Utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.ReadPreference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Sort;

/**
 * This will migrate the last 30 days of top level executions to TimeScaleDB
 */
@Slf4j
@Singleton
public class AddWorkflowExecutionFailureDetails implements TimeScaleDBDataMigration {
  @Inject TimeScaleDBService timeScaleDBService;

  @Inject WingsPersistence wingsPersistence;
  @Inject WorkflowExecutionService workflowExecutionService;

  private static final int MAX_RETRY = 5;

  private static final String update_statement =
      "UPDATE DEPLOYMENT SET FAILURE_DETAILS=?,FAILED_STEP_NAMES=?,FAILED_STEP_TYPES=? WHERE EXECUTIONID=?";

  private static final String query_statement = "SELECT * FROM DEPLOYMENT WHERE EXECUTIONID=?";

  @Override
  public boolean migrate() {
    if (!timeScaleDBService.isValid()) {
      log.info("TimeScaleDB not found, not migrating deployment data to TimeScaleDB");
      return false;
    }
    int count = 0;
    try {
      FindOptions findOptions = new FindOptions();
      findOptions.readPreference(ReadPreference.secondaryPreferred());
      try (HIterator<WorkflowExecution> iterator =
               new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
                                   .field(WorkflowExecutionKeys.createdAt)
                                   .greaterThanOrEq(System.currentTimeMillis() - (180 * 24 * 3600 * 1000L))
                                   .field(WorkflowExecutionKeys.startTs)
                                   .exists()
                                   .field(WorkflowExecutionKeys.endTs)
                                   .exists()
                                   .field(WorkflowExecutionKeys.status)
                                   .in(ExecutionStatus.resumableStatuses)
                                   .field(WorkflowExecutionKeys.accountId)
                                   .exists()
                                   .order(Sort.descending(WorkflowExecutionKeys.createdAt))
                                   .fetch(findOptions))) {
        while (iterator.hasNext()) {
          WorkflowExecution workflowExecution = iterator.next();
          updateWorkflowExecutionWithFailureDetails(workflowExecution);
          count++;
          if (count % 100 == 0) {
            log.info("Completed migrating workflow execution [{}] records", count);
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to complete rollback duration migration", e);
      return false;
    } finally {
      log.info("Completed updating [{}] records", count);
    }
    return true;
  }

  private void updateWorkflowExecutionWithFailureDetails(WorkflowExecution workflowExecution) {
    long startTime = System.currentTimeMillis();
    boolean successful = false;
    int retryCount = 0;

    final WorkflowExecution workflowExecutionWithFailureDetails =
        workflowExecutionService.getWorkflowExecutionWithFailureDetails(
            workflowExecution.getAppId(), workflowExecution.getUuid());
    String failureDetails = Utils.emptyIfNull(workflowExecutionWithFailureDetails.getFailureDetails());
    String failedStepNames = Utils.emptyIfNull(workflowExecutionWithFailureDetails.getFailedStepNames());
    String failedStepTypes = Utils.emptyIfNull(workflowExecutionWithFailureDetails.getFailedStepTypes());

    while (!successful && retryCount < MAX_RETRY) {
      ResultSet queryResult = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement queryStatement = connection.prepareStatement(query_statement);
           PreparedStatement updateStatement = connection.prepareStatement(update_statement)) {
        queryStatement.setString(1, workflowExecution.getUuid());
        queryResult = queryStatement.executeQuery();
        if (queryResult != null && queryResult.next()) {
          log.info("WorkflowExecution found:[{}],updating it", workflowExecution.getUuid());
          updateDataInTimescaleDB(workflowExecution, updateStatement, failureDetails, failedStepNames, failedStepTypes);
        }

        successful = true;
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          log.error("Failed to update workflowExecution,[{}]", workflowExecution.getUuid(), e);
        } else {
          log.info("Failed to update workflowExecution,[{}],retryCount=[{}]", workflowExecution.getUuid(), retryCount);
        }
        retryCount++;
      } catch (Exception e) {
        log.error("Failed to update workflowExecution,[{}]", workflowExecution.getUuid(), e);
        retryCount = MAX_RETRY + 1;
      } finally {
        DBUtils.close(queryResult);
        log.info("Total update time =[{}] for workflowExecution:[{}]", System.currentTimeMillis() - startTime,
            workflowExecution.getUuid());
      }
    }
  }

  private void updateDataInTimescaleDB(WorkflowExecution workflowExecution, PreparedStatement updateStatement,
      String failureDetails, String failedStepNames, String failedStepTypes) throws SQLException {
    updateStatement.setString(1, failureDetails);
    updateStatement.setString(2, failedStepNames);
    updateStatement.setString(3, failedStepTypes);
    updateStatement.setString(4, workflowExecution.getUuid());
    updateStatement.execute();
  }
}
