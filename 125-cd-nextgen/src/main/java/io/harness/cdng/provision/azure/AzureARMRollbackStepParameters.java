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
@RecasterAlias("io.harness.cdng.provision.azure.AzureARMRollbackStepParameters")
public class AzureARMRollbackStepParameters extends AzureARMRollbackBaseStepInfo implements SpecParameters {
    @NonNull
    AzureARMRollbackStepConfiguration configuration;
    @Builder(builderMethodName = "infoBuilder")
    public AzureARMRollbackStepParameters(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
                                          @NonNull AzureARMRollbackStepConfiguration configuration) {
        super(delegateSelectors);
        this.configuration = configuration;

    }
}
