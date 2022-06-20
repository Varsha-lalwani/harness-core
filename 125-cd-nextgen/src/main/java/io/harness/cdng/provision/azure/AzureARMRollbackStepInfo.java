package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.filters.WithConnectorRef;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.validation.Validator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;
import io.harness.walktree.visitor.Visitable;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
@TypeAlias("azureARMRollbackStepInfo")
@JsonTypeName(StepSpecTypeConstants.AZURE_ROLLBACK_ARM_RESOURCE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@RecasterAlias("io.harness.cdng.provision.azure.AzureARMRollbackStepInfo")
public class AzureARMRollbackStepInfo extends AzureARMRollbackBaseStepInfo implements CDStepInfo, Visitable, WithConnectorRef {
    @NotNull @JsonProperty("configuration")
    AzureARMRollbackStepConfiguration configuration;

    @Builder(builderMethodName = "infoBuilder")
    public AzureARMRollbackStepInfo(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
                                    AzureARMRollbackStepConfiguration azureARMRollbackStepConfiguration) {
        super(delegateSelectors);
        this.configuration = azureARMRollbackStepConfiguration;
    }

    @Override
    public ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
        return getDelegateSelectors();
    }

    @Override
    public Map<String, ParameterField<String>> extractConnectorRefs() {
        return new HashMap<>();
    }

    @Override
    public StepType getStepType() {
        return AzureARMRollbackStep.STEP_TYPE;
    }

    @Override
    public String getFacilitatorType() {
        return OrchestrationFacilitatorType.TASK;
    }

    @Override
    public SpecParameters getSpecParameters() {
        validateSpecParameters();
        return AzureARMRollbackStepParameters.infoBuilder()
                .delegateSelectors(getDelegateSelectors())
                .configuration(configuration)
                .build();
    }

    private void validateSpecParameters() {
        Validator.notNullCheck("Azure Rollback configuration is null", configuration);
        configuration.validateParams();
    }
}
