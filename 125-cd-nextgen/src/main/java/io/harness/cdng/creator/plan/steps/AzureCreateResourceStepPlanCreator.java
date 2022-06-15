package io.harness.cdng.creator.plan.steps;

import com.google.common.collect.Sets;
import io.harness.cdng.provision.azure.AzureCreateStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import java.util.Set;

public class AzureCreateResourceStepPlanCreator extends CDPMSStepPlanCreatorV2<AzureCreateStepNode> {
    @Override
    public Set<String> getSupportedStepTypes() {
        return Sets.newHashSet(StepSpecTypeConstants.AZURE_CREATE_RESOURCE);
    }

    @Override
    public Class<AzureCreateStepNode> getFieldClass() {
        return AzureCreateStepNode.class;
    }

    @Override
    public PlanCreationResponse createPlanForField(
            PlanCreationContext ctx, AzureCreateStepNode stepElement) {
        return super.createPlanForField(ctx, stepElement);
    }
}
