package io.harness.ng.core.serviceoverride.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDP)
@Data
@Builder
public class ServiceOverrideRequest implements YamlDTO {
  @Valid @NotNull @JsonProperty("service") private NGServiceOverridesEntity serviceOverride;
}
