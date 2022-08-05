/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.timescaledb.data;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;
import io.harness.migrations.TimeScaleDBDataMigration;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.beans.Application;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.Utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
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
  @Inject FeatureFlagService featureFlagService;

  private static final int MAX_RETRY = 5;

  private static final String update_statement =
      "UPDATE DEPLOYMENT SET FAILURE_DETAILS=?,FAILED_STEP_NAMES=?,FAILED_STEP_TYPES=? WHERE EXECUTIONID=?";

  private static final String query_statement = "SELECT * FROM DEPLOYMENT WHERE EXECUTIONID=?";

  private String debugLine = "EXECUTION_FAILURE_TIMESCALE MIGRATION: ";

  @Override
  public boolean migrate() {
    if (!timeScaleDBService.isValid()) {
      log.info("TimeScaleDB not found, not migrating deployment data to TimeScaleDB");
      return false;
    }
    try {
      log.info(debugLine + "Migration of stateExecutionInstances started");
      List<String> accountIds =
          featureFlagService.getAccountIds(FeatureName.TIME_SCALE_CG_SYNC).stream().collect(Collectors.toList());
      Map<String, Set<String>> accountIdToAppIdMap = new HashMap<>();
      for (String accountId : accountIds) {
        try {
          List<Key<Application>> appIdKeyList = wingsPersistence.createQuery(Application.class)
                                                    .filter(Application.ApplicationKeys.accountId, accountId)
                                                    .asKeyList();

          if (isNotEmpty(appIdKeyList)) {
            Set<String> appIdSet = appIdKeyList.stream()
                                       .map(applicationKey -> (String) applicationKey.getId())
                                       .collect(Collectors.toSet());
            accountIdToAppIdMap.put(accountId, appIdSet);
          }
        } catch (Exception e) {
          log.error(
              debugLine + "Exception occurred migrating parent pipeline id to timescale deployments for accountId {}",
              accountId, e);
        }
      }
      for (Map.Entry<String, Set<String>> entry : accountIdToAppIdMap.entrySet()) {
        for (String appId : entry.getValue()) {
          bulkSetFailureDetails(entry.getKey(), "workflowExecutions", appId);
        }
      }
      log.info(debugLine + "Migration to populate parent pipeline id to timescale deployments successful");
      return true;
    } catch (Exception e) {
      log.error(debugLine + "Exception occurred migrating parent pipeline id to timescale deployments", e);
      return false;
    }
  }

  private void bulkSetFailureDetails(String accountId, String collectionName, String appId) {
    log.info(debugLine + "Migrating all workflowExecutions for account " + accountId);
    long executionsCount = createQuery(accountId, appId).count();
    List<WorkflowExecution> workflowExecutions = createQuery(accountId, appId).limit(1000).asList();
    List<WorkflowExecution> workflowExecutionsWithFailureDetails =
        workflowExecutionService.getWorkflowExecutionsWithFailureDetails(appId, workflowExecutions);

    int updated = 0;
    int batched = 0;
    int offset = 0;
    try (Connection connection = timeScaleDBService.getDBConnection();
         PreparedStatement updateStatement = connection.prepareStatement(update_statement)) {
      while (workflowExecutionsWithFailureDetails.iterator().hasNext()) {
        WorkflowExecution workflowExecution = workflowExecutionsWithFailureDetails.iterator().next();

        updateStatement.setString(1, workflowExecution.getFailureDetails());
        updateStatement.setString(2, workflowExecution.getFailedStepNames());
        updateStatement.setString(3, workflowExecution.getFailedStepTypes());
        updateStatement.setString(4, workflowExecution.getUuid());
        updateStatement.addBatch();
        updated++;
        batched++;

        if (updated != 0 && updated % 1000 == 0 && updated < executionsCount) {
          int[] affectedRecords = updateStatement.executeBatch();
          sleep(Duration.ofMillis(100));
          workflowExecutionsWithFailureDetails = createQuery(accountId, appId).limit(1000).offset(updated).asList();
          batched = 0;
          log.info(debugLine + "Number of records updated for {} is: {}", collectionName, updated);
        }
      }

      if (batched != 0) {
        int[] affectedRecords = updateStatement.executeBatch();
        log.info(debugLine + "Number of records updated for {} is: {}", collectionName, updated);
      }
    } catch (Exception e) {
      log.error(debugLine
              + "Exception occurred migrating parent pipeline id to timescale deployments for accountId {}, appId {}",
          accountId, appId, e);
    }
  }

  private Query<WorkflowExecution> createQuery(String accountId, String appId) {
    return wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
        .filter(WorkflowExecutionKeys.accountId, accountId)
        .filter(WorkflowExecutionKeys.appId, appId)
        .field(WorkflowExecutionKeys.status)
        .in(ExecutionStatus.resumableStatuses)
        .order(Sort.descending(WorkflowExecutionKeys.createdAt));
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
