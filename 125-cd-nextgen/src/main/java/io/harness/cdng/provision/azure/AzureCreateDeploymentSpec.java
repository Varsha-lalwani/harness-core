package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.OwnedBy;


import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = AzureARMDeploymentSpec.class, name = AzureDeploymentTypes.ARM),
        @JsonSubTypes.Type(value = AzureBluePrintDeploymentSpec.class, name = AzureDeploymentTypes.BLUEPRINT)
})

public interface AzureCreateDeploymentSpec {
    String getType();
    void validateParams();
}
