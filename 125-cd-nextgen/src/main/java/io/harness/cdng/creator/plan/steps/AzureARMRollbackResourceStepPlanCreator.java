package io.harness.cdng.creator.plan.steps;

import com.google.common.collect.Sets;
import io.harness.cdng.provision.azure.AzureARMRollbackStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import java.util.Set;

public class AzureARMRollbackResourceStepPlanCreator extends CDPMSStepPlanCreatorV2<AzureARMRollbackStepNode> {
    @Override
    public Set<String> getSupportedStepTypes() {
        return Sets.newHashSet(StepSpecTypeConstants.AZURE_ROLLBACK_ARM_RESOURCE);
    }

    @Override
    public Class<AzureARMRollbackStepNode> getFieldClass() {
        return AzureARMRollbackStepNode.class;
    }

    @Override
    public PlanCreationResponse createPlanForField(
            PlanCreationContext ctx, AzureARMRollbackStepNode stepElement) {
        return super.createPlanForField(ctx, stepElement);
    }
}
