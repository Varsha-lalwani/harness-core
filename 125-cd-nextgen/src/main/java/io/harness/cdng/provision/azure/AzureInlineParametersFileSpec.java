package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@RecasterAlias("io.harness.cdng.provision.azure.AzureInlineParametersFileSpec")
public class AzureInlineParametersFileSpec implements AzureARMParametersFileSpec {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    String uuid;
    @NotNull
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    ParameterField<String> templateBody;

    @Override
    public String getType() {
        return AzureCreateTemplateFileTypes.Inline;
    }
}