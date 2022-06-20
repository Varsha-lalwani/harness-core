package io.harness.cdng.provision.azure;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.validation.Validator;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.provision.azure.AzureARMRollbackStepConfiguration")
public class AzureARMRollbackStepConfiguration {
    @NotNull
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
    ParameterField<String> provisionerIdentifier;

    void validateParams() {
        Validator.notNullCheck("Provisioner identifier is null", provisionerIdentifier);
    }
}
