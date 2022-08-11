/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.timescaledb.data;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.threading.Morpheus.sleep;

import io.harness.beans.CreatedByType;
import io.harness.beans.ExecutionCause;
import io.harness.beans.FeatureName;
import io.harness.beans.WorkflowType;
import io.harness.data.structure.CollectionUtils;
import io.harness.ff.FeatureFlagService;
import io.harness.migrations.TimeScaleDBDataMigration;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.beans.Application;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.WorkflowExecutionServiceHelper;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.Utils;

import com.google.inject.Inject;
import com.mongodb.*;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;

@Slf4j
public class AddParentPipelineDataToDeployment implements TimeScaleDBDataMigration {
  @Inject TimeScaleDBService timeScaleDBService;

  @Inject WingsPersistence wingsPersistence;
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject FeatureFlagService featureFlagService;

  private static final int MAX_RETRY = 5;

  private static final String update_statement =
      "UPDATE DEPLOYMENT SET PARENT_PIPELINE_ID=?, WORKFLOWS=?, CAUSE=? WHERE EXECUTIONID=?";

  private static final String query_statement = "SELECT * FROM DEPLOYMENT WHERE EXECUTIONID=?";

  private String debugLine = "PARENT_PIPELINE_TIMESCALE MIGRATION: ";

  @Override
  public boolean migrate() {
    if (!timeScaleDBService.isValid()) {
      log.info("TimeScaleDB not found, not migrating deployment data to TimeScaleDB");
      return false;
    }
    try {
      log.info(debugLine + "Migration of deployments table started");
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
          bulkSetParentPipelineId(entry.getKey(), "workflowExecutions", appId);
        }
      }
      log.info(debugLine + "Migration to populate parent pipeline id to timescale deployments successful");
      return true;
    } catch (Exception e) {
      log.error(debugLine + "Exception occurred migrating parent pipeline id to timescale deployments", e);
      return false;
    }
  }

  private void bulkSetParentPipelineId(String accountId, String collectionName, String appId) {
    log.info(debugLine + "Migrating all workflowExecutions for account " + accountId);
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, collectionName);

    BasicDBObject objectsToBeUpdated =
        new BasicDBObject("accountId", accountId)
            .append("appId", appId)
            .append(WorkflowExecutionKeys.pipelineSummary, new BasicDBObject("$exists", Boolean.TRUE));
    BasicDBObject projection = new BasicDBObject("_id", Boolean.TRUE)
                                   .append(WorkflowExecutionKeys.pipelineSummary, Boolean.TRUE)
                                   .append(WorkflowExecutionKeys.workflowIds, Boolean.TRUE)
                                   .append(WorkflowExecutionKeys.workflowType, Boolean.TRUE)
    .append(WorkflowExecutionKeys.pipelineExecutionId, Boolean.TRUE)
    .append(WorkflowExecutionKeys.createdByType, Boolean.TRUE)
    .append(WorkflowExecutionKeys.deploymentTriggerId, Boolean.TRUE)
            .append(WorkflowExecutionKeys.triggeredBy, Boolean.TRUE);

    DBCursor dataRecords = collection.find(objectsToBeUpdated, projection)
                               .sort(new BasicDBObject().append(WorkflowExecutionKeys.createdAt, -1))
                               .limit(5);

    int updated = 0;
    int batched = 0;
    try (Connection connection = timeScaleDBService.getDBConnection();
         PreparedStatement updateStatement = connection.prepareStatement(update_statement)) {
      while (dataRecords.hasNext()) {
        DBObject record = dataRecords.next();

        String uuId = (String) record.get("_id");
        List<String> workflowIds = (List<String>) record.get(WorkflowExecutionKeys.workflowIds);
        String workflowType = (String) record.get(WorkflowExecutionKeys.workflowType);
        String parentPipelineId = null;
        DBObject pipelineSummary = (DBObject) record.get(WorkflowExecutionKeys.pipelineSummary);
        if (pipelineSummary != null && WorkflowType.ORCHESTRATION.name().equals(workflowType)) {
          parentPipelineId = (String) pipelineSummary.get("pipelineId");
        }

        updateStatement.setString(1, parentPipelineId);
        Array array = null;
        if(workflowIds!=null && WorkflowType.PIPELINE.name().equals(workflowType)){
          array = connection.createArrayOf("text", workflowIds.toArray());
        }
        updateStatement.setArray(2, array);
        String cause = getCause(record);
        updateStatement.setString(3,cause);
        updateStatement.setString(4, uuId);
        updateStatement.addBatch();
        updated++;
        batched++;

        if (updated != 0 && updated % 5 == 0) {
          int[] affectedRecords = updateStatement.executeBatch();
          sleep(Duration.ofMillis(100));
          dataRecords = collection.find(objectsToBeUpdated, projection).sort(new BasicDBObject().append(WorkflowExecutionKeys.createdAt, -1)).skip(updated).limit(5);
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
    } finally {
      dataRecords.close();
    }
  }

  public static String getCause(DBObject record) {

    if (record.get(WorkflowExecutionKeys.pipelineExecutionId) != null) {
      return ExecutionCause.ExecutedAlongPipeline.name();
    } else {
      String createdByType = (String)record.get(WorkflowExecutionKeys.createdByType);
      if (CreatedByType.API_KEY.name().equals( createdByType)) {
        return ExecutionCause.ExecutedByAPIKey.name();
      } else if (record.get(WorkflowExecutionKeys.deploymentTriggerId) != null) {
        return ExecutionCause.ExecutedByTrigger.name();
      } else if (record.get(WorkflowExecutionKeys.triggeredBy) != null) {
        return ExecutionCause.ExecutedByUser.name();
      }
    }
    return null;
  }
}
