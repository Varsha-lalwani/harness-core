package io.harness.migrations.timescaledb.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.beans.FeatureName;
import io.harness.event.reconciliation.service.DeploymentReconService;
import io.harness.ff.FeatureFlagService;
import io.harness.migrations.TimeScaleDBDataMigration;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class SyncWorkflowExecutionDataFromMongo implements TimeScaleDBDataMigration {
    @Inject
    TimeScaleDBService timeScaleDBService;
    @Inject
    DeploymentReconService deploymentReconService;
    @Inject
    FeatureFlagService featureFlagService;
    private String debugLine = "MISSING_DEPLOYMENT_TIMESCALE MIGRATION: ";
    private final long START_TS = 1659571200000L;
    private final long END_TS = 1659916800000L;

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
            for (String accountId : accountIds) {
                try {
                    bulkPerformReconciliation(accountId);
                } catch (Exception e) {
                    log.error(
                            debugLine + "Exception occurred migrating parent pipeline id to timescale deployments for accountId {}",
                            accountId, e);
                }
            }
            log.info(debugLine + "Migration to populate parent pipeline id to timescale deployments successful");
            return true;
        } catch (Exception e) {
            log.error(debugLine + "Exception occurred migrating parent pipeline id to timescale deployments", e);
            return false;
        }
    }

    private void bulkPerformReconciliation(String accountId) {
        log.info(debugLine + "Migrating all workflowExecutions for account " + accountId);
        deploymentReconService.performReconciliation(accountId, START_TS, END_TS);
    }
}
