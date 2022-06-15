package io.harness.cdng.provision.azure;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@RecasterAlias("io.harness.cdng.provision.azure.CreateStepParameters")

public class AzureCreateStepParameters extends AzureCreateBaseStepInfo implements SpecParameters {
    @NonNull AzureCreateStepConfigurationParameters configuration;
    @Builder(builderMethodName = "infoBuilder")
    public AzureCreateStepParameters(ParameterField<String> provisionerIdentifier,
                                     ParameterField<List<TaskSelectorYaml>> delegateSelectors,
                                     String uuid,
                                     @NonNull AzureCreateStepConfigurationParameters configuration) {
        super(provisionerIdentifier, delegateSelectors, uuid);
        this.configuration = configuration;
    }
}
