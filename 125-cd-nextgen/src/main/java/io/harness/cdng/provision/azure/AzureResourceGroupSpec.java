package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.validation.Validator;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.provision.azure.AzureResourceGroupSpec")
public class AzureResourceGroupSpec implements AzureScopeType{
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

    @Override
    public void validateParams() {
        Validator.notNullCheck("subscription can't be null", subscription);
        Validator.notNullCheck("resourceGroup can't be null", resourceGroup);
        Validator.notNullCheck("mode can't be null", mode);
    }
}
