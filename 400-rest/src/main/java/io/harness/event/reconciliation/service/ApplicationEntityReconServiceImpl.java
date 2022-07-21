/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.reconciliation.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.time.Duration.ofMinutes;

import io.harness.event.reconciliation.DetectionStatus;
import io.harness.event.reconciliation.ReconcilationAction;
import io.harness.event.reconciliation.ReconciliationStatus;
import io.harness.event.reconciliation.deployment.DeploymentReconRecord;
import io.harness.event.reconciliation.looker.LookerEntityReconRecord;
import io.harness.event.reconciliation.looker.LookerEntityReconRecordRepository;
import io.harness.lock.AcquiredLock;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
public class ApplicationEntityReconServiceImpl implements LookerEntityReconService {
  @Inject TimeScaleDBService timeScaleDBService;

  @Inject LookerEntityReconRecordRepository lookerEntityReconRecordRepository;

  protected static final long COOL_DOWN_INTERVAL = 15 * 60 * 1000; /* 15 MINS COOL DOWN INTERVAL */

  private static final String CHECK_MISSING_DATA_QUERY =
      "SELECT COUNT(DISTINCT(ID)) FROM CG_APPLICATIONS WHERE ACCOUNT_ID=? AND ((CREATED_AT>=? AND CREATED_AT<=?);";

  private static final String DELETE_DUPLICATE = "DELETE FROM CG_APPLICATIONS WHERE ID = ANY (?);";

  private static final String FIND_DEPLOYMENT_IN_TSDB = "SELECT ID,CREATED_AT FROM CG_APPLICATIONS WHERE ID=?";

  @Override
  public ReconciliationStatus performReconciliation(
      String accountId, long durationStartTs, long durationEndTs, Class sourceEntityClass) {
    if (!timeScaleDBService.isValid()) {
      log.info("TimeScaleDB is not valid, skipping reconciliation for accountID:[{}] in duration:[{}-{}]", accountId,
          new Date(durationStartTs), new Date(durationEndTs));
      return ReconciliationStatus.SUCCESS;
    }
    LookerEntityReconRecord record = lookerEntityReconRecordRepository.getLatestLookerEntityReconRecord(
        accountId, sourceEntityClass.getCanonicalName());
    if (record == null
        || shouldPerformReconciliation(record, durationEndTs)) { /*
try (AcquiredLock ignore = persistentLocker.waitToAcquireLock(
DeploymentReconRecord.class, "AccountID-" + accountId, ofMinutes(1), ofMinutes(5))) {
record = getLatestDeploymentReconRecord(accountId);

if (record != null && !shouldPerformReconciliation(record, durationEndTs)) {
if (record.getReconciliationStatus() == ReconciliationStatus.IN_PROGRESS) {
log.info("Reconciliation is in progress, not running it again for accountID:[{}] in duration:[{}-{}]",
      accountId, new Date(durationStartTs), new Date(durationEndTs));
} else {
log.info(
      "Reconciliation was performed recently at [{}], not running it again for accountID:[{}] in duration:[{}-{}]",
      accountId, new Date(durationStartTs), new Date(durationEndTs));
}
return ReconciliationStatus.SUCCESS;
}

record = DeploymentReconRecord.builder()
  .accountId(accountId)
  .reconciliationStatus(ReconciliationStatus.IN_PROGRESS)
  .reconStartTs(System.currentTimeMillis())
  .durationStartTs(durationStartTs)
  .durationEndTs(durationEndTs)
  .build();
String id = persistence.save(record);
log.info("Inserted new deploymentReconRecord for accountId:[{}],uuid:[{}]", accountId, id);
record = fetchRecord(id);

boolean duplicatesDetected = false;
boolean missingRecordsDetected = false;
boolean statusMismatchDetected;

List<String> executionIDs = checkForDuplicates(accountId, durationStartTs, durationEndTs);
if (isNotEmpty(executionIDs)) {
duplicatesDetected = true;
log.warn("Duplicates detected for accountId:[{}] in duration:[{}-{}], executionIDs:[{}]", accountId,
    new Date(durationStartTs), new Date(durationEndTs), executionIDs);
deleteDuplicates(accountId, durationStartTs, durationEndTs, executionIDs);
}

long primaryCount = getWFExecCountFromMongoDB(accountId, durationStartTs, durationEndTs);
long secondaryCount = getWFExecutionCountFromTSDB(accountId, durationStartTs, durationEndTs);
if (primaryCount > secondaryCount) {
missingRecordsDetected = true;
insertMissingRecords(accountId, durationStartTs, durationEndTs);
} else if (primaryCount == secondaryCount) {
log.info("Everything is fine, no action required for accountID:[{}] in duration:[{}-{}]", accountId,
    new Date(durationStartTs), new Date(durationEndTs));
} else {
log.error("Duplicates found again for accountID:[{}] in duration:[{}-{}]", accountId,
    new Date(durationStartTs), new Date(durationEndTs));
}

Map<String, String> tsdbRunningWFs = getRunningWFsFromTSDB(accountId, durationStartTs, durationEndTs);
statusMismatchDetected = isStatusMismatchedAndUpdated(tsdbRunningWFs);

DetectionStatus detectionStatus;
ReconcilationAction action;

if (!statusMismatchDetected) {
if (missingRecordsDetected && duplicatesDetected) {
detectionStatus = DetectionStatus.DUPLICATE_DETECTED_MISSING_RECORDS_DETECTED;
action = ReconcilationAction.DUPLICATE_REMOVAL_ADD_MISSING_RECORDS;
} else if (duplicatesDetected) {
detectionStatus = DetectionStatus.DUPLICATE_DETECTED;
action = ReconcilationAction.DUPLICATE_REMOVAL;
} else if (missingRecordsDetected) {
detectionStatus = DetectionStatus.MISSING_RECORDS_DETECTED;
action = ReconcilationAction.ADD_MISSING_RECORDS;
} else {
detectionStatus = DetectionStatus.SUCCESS;
action = ReconcilationAction.NONE;
}
} else {
if (missingRecordsDetected && duplicatesDetected) {
detectionStatus = DetectionStatus.DUPLICATE_DETECTED_MISSING_RECORDS_DETECTED_STATUS_MISMATCH_DETECTED;
action = ReconcilationAction.DUPLICATE_REMOVAL_ADD_MISSING_RECORDS_STATUS_RECONCILIATION;
} else if (duplicatesDetected) {
detectionStatus = DetectionStatus.DUPLICATE_DETECTED_STATUS_MISMATCH_DETECTED;
action = ReconcilationAction.DUPLICATE_REMOVAL_STATUS_RECONCILIATION;
} else if (missingRecordsDetected) {
detectionStatus = DetectionStatus.MISSING_RECORDS_DETECTED_STATUS_MISMATCH_DETECTED;
action = ReconcilationAction.ADD_MISSING_RECORDS_STATUS_RECONCILIATION;
} else {
detectionStatus = DetectionStatus.STATUS_MISMATCH_DETECTED;
action = ReconcilationAction.STATUS_RECONCILIATION;
}
}

UpdateOperations updateOperations = persistence.createUpdateOperations(DeploymentReconRecord.class);
updateOperations.set(DeploymentReconRecord.DeploymentReconRecordKeys.detectionStatus, detectionStatus);
updateOperations.set(DeploymentReconRecord.DeploymentReconRecordKeys.reconciliationStatus,
ReconciliationStatus.SUCCESS); updateOperations.set(DeploymentReconRecord.DeploymentReconRecordKeys.reconcilationAction,
action); updateOperations.set(DeploymentReconRecord.DeploymentReconRecordKeys.reconEndTs, System.currentTimeMillis());
persistence.update(record, updateOperations);

} catch (Exception e) {
log.error("Exception occurred while running reconciliation for accountID:[{}] in duration:[{}-{}]", accountId,
  new Date(durationStartTs), new Date(durationEndTs), e);
if (record != null) {
UpdateOperations updateOperations = persistence.createUpdateOperations(DeploymentReconRecord.class);
updateOperations.set(DeploymentReconRecord.DeploymentReconRecordKeys.reconciliationStatus, ReconciliationStatus.FAILED);
updateOperations.set(DeploymentReconRecord.DeploymentReconRecordKeys.reconEndTs, System.currentTimeMillis());
persistence.update(record, updateOperations);
return ReconciliationStatus.FAILED;
}
}*/
    } else {
      log.info("Reconciliation task not required for accountId:[{}], durationStartTs: [{}], durationEndTs:[{}]",
          accountId, new Date(durationStartTs), new Date(durationEndTs));
    }
    return ReconciliationStatus.SUCCESS;
  }

  protected boolean shouldPerformReconciliation(@NotNull LookerEntityReconRecord record, Long durationEndTs) {
    if (record.getReconciliationStatus() == ReconciliationStatus.IN_PROGRESS) {
      /***
       * If the latest record in db is older than COOL_DOWN_INTERVAL, mark that reconciliation as failed and move on.
       * This is to prevent a bad record from blocking all further reconciliations
       */
      if (System.currentTimeMillis() - record.getDurationEndTs() > COOL_DOWN_INTERVAL) {
        log.warn("Found an old record in progress: record: [{}] for accountID:[{}] in duration:[{}-{}]",
            record.getUuid(), record.getAccountId(), new Date(record.getDurationStartTs()),
            new Date(record.getDurationEndTs()));
        lookerEntityReconRecordRepository.updateReconStatus(record, ReconciliationStatus.FAILED);
        return true;
      }

      /**
       * If a reconciliation is in progress, do not kick off another reconciliation.
       * This is to prevent managers from stamping on each other
       */

      return false;
    }

    /**
     * If reconciliation was run recently AND if the duration for which it was run was in the recent time interval,
     * lets not run it again.
     */

    final long currentTime = System.currentTimeMillis();
    if (((currentTime - record.getReconEndTs()) < COOL_DOWN_INTERVAL)
        && (durationEndTs < currentTime && durationEndTs > (currentTime - COOL_DOWN_INTERVAL))) {
      log.info("Last recon for accountID:[{}] was run @ [{}], hence not rerunning it again", record.getAccountId(),
          new Date(record.getReconEndTs()));
      return false;
    }

    return true;
  }
}
