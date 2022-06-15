package io.harness.cdng.provision.azure;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.provision.azure.beans.CreatePassThroughData;
import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
public interface AzureCreateStepExecutor {
    TaskChainResponse executeCreateTask(Ambiance ambiance, StepElementParameters stepParameters,
                                        AzureARMTaskParameters parameters, CreatePassThroughData passThroughData);
}
