package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

import javax.validation.constraints.NotNull;
import java.util.List;

import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;


@Data
@NoArgsConstructor
@OwnedBy(HarnessTeam.CDP)
@JsonTypeName(StepSpecTypeConstants.AZURE_ROLLBACK_ARM_RESOURCE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@RecasterAlias("io.harness.cdng.provision.azure.AzureARMRollbackStepInfo")
public class AzureARMRollbackStepInfo extends AzureARMRollbackBaseStepInfo implements CDStepInfo{
    @NotNull
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    ParameterField<String> provisionerIdentifier;

    @Override
    public ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
        return getDelegateSelectors();
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
        return AzureARMRollbackStepParameters.infoBuilder()
                .delegateSelectors(getDelegateSelectors())
                .build();
    }
}
