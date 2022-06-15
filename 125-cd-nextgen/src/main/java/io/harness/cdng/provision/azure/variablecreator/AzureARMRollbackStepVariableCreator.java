package io.harness.cdng.provision.azure.variablecreator;

import io.harness.cdng.provision.azure.AzureARMRollbackStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import java.util.Collections;
import java.util.Set;

public class AzureARMRollbackStepVariableCreator extends GenericStepVariableCreator<AzureARMRollbackStepNode> {
    @Override
    public Set<String> getSupportedStepTypes() {
        return Collections.singleton(StepSpecTypeConstants.AZURE_ROLLBACK_ARM_RESOURCE);
    }

    @Override
    public Class<AzureARMRollbackStepNode> getFieldClass() {
        return AzureARMRollbackStepNode.class;
    }
}
