package io.harness.cdng.provision.azure;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.provision.azure.CreateStepConfigurationParameters")
public class AzureCreateStepConfigurationParameters {
    ParameterField<String> connectorRef;
    AzureCreateDeployment azureCreateDeployment;
}
