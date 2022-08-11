package io.harness.migrations.timescaledb;

public class AddParentPipelineToDeployment extends AbstractTimeScaleDBMigration {
    @Override
    public String getFileName() {
        return "timescaledb/add_parent_pipeline_to_deployment.sql";
    }
}
