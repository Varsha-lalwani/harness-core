package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

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
@RecasterAlias("io.harness.cdng.provision.azure.ARMDeploymentSpec")
public class AzureARMDeploymentSpec implements AzureDeploymentType {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    String uuid;

    @NotNull String connectorRef;
    @NotNull AzureCreateTemplateFile templateFile;

    AzureCreateParameterFile parameters;

    @NotNull
    AzureCreateStepScope scope;

    @Override
    public void validateParams() {
        Validator.notNullCheck("Template file can't be empty", templateFile);
        Validator.notNullCheck("Connector ref can't be empty", connectorRef);
        Validator.notNullCheck("Scope can't be empty", scope);
        scope.getSpec().validateParams();
    }

    @Override
    public AzureCreateTemplateFile getTemplateSpecs() {
        return templateFile;
    }

    @Override
    public String getConnectorRef() {
        return connectorRef;
    }

    @Override
    public String getType() {
        return AzureDeploymentTypes.ARM;
    }
}
