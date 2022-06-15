package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.pms.yaml.YamlNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class AzureCreateTemplateFile {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    String uuid;
    @NotNull
    StoreConfigWrapper store;
}
