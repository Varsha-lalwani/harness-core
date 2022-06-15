package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.YamlNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.provision.azure.AzureCreateStepScope")
public class AzureCreateStepScope {
    @JsonProperty(YamlNode.UUID_FIELD_NAME)
    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
    @ApiModelProperty(hidden = true)
    String uuid;

    @NotNull String type;

    @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
    @NotNull AzureScopeType spec;
}
