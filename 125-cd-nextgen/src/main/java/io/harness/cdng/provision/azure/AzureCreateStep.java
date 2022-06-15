package io.harness.cdng.provision.azure;

import io.harness.cdng.provision.azure.beans.CreatePassThroughData;
import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

public class AzureCreateStep extends TaskChainExecutableWithRollbackAndRbac implements AzureCreateStepExecutor {
    public static final StepType STEP_TYPE = StepType.newBuilder()
            .setType(ExecutionNodeType.AZURE_CREATE_RESOURCE.getYamlType())
            .setStepCategory(StepCategory.STEP)
            .build();

    @Override
    public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {

    }

    @Override
    public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier) throws Exception {
        return null;
    }

    @Override
    public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
        return null;
    }

    @Override
    public TaskChainResponse startChainLinkAfterRbac(Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
        return null;
    }

    @Override
    public Class<StepElementParameters> getStepParametersClass() {
        return null;
    }

    @Override
    public TaskChainResponse executeCreateTask(Ambiance ambiance, StepElementParameters stepParameters, AzureARMTaskParameters parameters, CreatePassThroughData passThroughData) {
        return null;
    }
}

