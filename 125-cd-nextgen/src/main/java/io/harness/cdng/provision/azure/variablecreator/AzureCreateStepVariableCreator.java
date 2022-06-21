package io.harness.cdng.provision.azure.variablecreator;

import io.harness.cdng.provision.azure.AzureCreateStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import java.util.Collections;
import java.util.Set;

public class AzureCreateStepVariableCreator extends GenericStepVariableCreator<AzureCreateStepNode> {
    @Override
    public Set<String> getSupportedStepTypes() {
        return Collections.singleton(StepSpecTypeConstants.AZURE_CREATE_RESOURCE);
    }

    @Override
    public Class<AzureCreateStepNode> getFieldClass() {
        return AzureCreateStepNode.class;
    }
}
