package io.harness.cdng.provision.azure;

import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.supplier.ThrowingSupplier;

public class AzureARMRollbackStep extends TaskExecutableWithRollbackAndRbac<AzureTaskExecutionResponse> {

    public static final StepType STEP_TYPE = StepType.newBuilder()
            .setType(ExecutionNodeType.AZURE_ROLLBACK_ARM_RESOURCE.getYamlType())
            .setStepCategory(StepCategory.STEP)
            .build();

    @Override
    public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {

    }

    @Override
    public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters, ThrowingSupplier<AzureTaskExecutionResponse> responseDataSupplier) throws Exception {
        return null;
    }


    @Override
    public TaskRequest obtainTaskAfterRbac(Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
        return null;
    }

    @Override
    public Class<StepElementParameters> getStepParametersClass() {
        return null;
    }
}
