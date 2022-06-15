package io.harness.cdng.provision.azure;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.yaml.YamlSchemaTypes;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import javax.validation.constraints.NotNull;

import java.util.List;

import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("createBaseStepInfo")
public class AzureCreateBaseStepInfo {
    @NotNull
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    private ParameterField<String> provisionerIdentifier;
    @YamlSchemaTypes(value = {runtime})
    @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
    ParameterField<List<TaskSelectorYaml>> delegateSelectors;
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    String uuid;
}