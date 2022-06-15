package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.provision.azure.ARMDeploymentSpec")
public class AzureARMDeploymentSpec implements AzureCreateDeploymentSpec {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    String uuid;
    @NotNull
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    ParameterField<String> subscription;
    @NotNull
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    ParameterField<String> resourceGroup;
    @NotNull
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    ParameterField<String> mode;
    @NotNull
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    ParameterField<String> scope;
    @NotNull AzureCreateTemplateFile templateFile;
    @JsonProperty("parameters")
    List<AzureARMParametersFileSpec> parametersFilesSpecs;

    @Override
    public String getType() {
        return AzureAzureDeploymentTypes.ARM;
    }

    @Override
    public void validateParams() {
    }
}
