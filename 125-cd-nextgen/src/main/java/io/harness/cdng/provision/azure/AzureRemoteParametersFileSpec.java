package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.data.validator.EntityIdentifier;
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
public class AzureRemoteParametersFileSpec implements AzureARMParametersFileSpec {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    String uuid;
    @NotNull StoreConfigWrapper store;
    @NotNull
    @EntityIdentifier
    String identifier;

    @Override
    public String getType() {
        return AzureCreateTemplateFileTypes.Remote;
    }
}
