package io.harness.cdng.provision.azure;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotation.RecasterAlias;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.pms.yaml.YamlNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@RecasterAlias("io.harness.cdng.provision.azure.RemoteTemplateFileSpec")
public class AzureRemoteTemplateFileSpec implements AzureCreateTemplateFileSpec {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    String uuid;
    @NotNull StoreConfigWrapper store;

    @Override
    public String getType() {
        return AzureCreateTemplateFileTypes.Remote;
    }
}
