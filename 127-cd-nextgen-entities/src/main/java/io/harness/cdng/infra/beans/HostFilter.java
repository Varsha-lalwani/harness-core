package io.harness.cdng.infra.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.delegate.beans.connector.pdcconnector.HostFilterType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@TypeAlias("hostFilter")
@RecasterAlias("io.harness.cdng.infra.beans.HostFilter")
public class HostFilter {
  @NotNull @JsonProperty("type") HostFilterType type;

  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  @NotNull
  Filter spec;

  @Builder
  public HostFilter(HostFilterType type, Filter spec) {
    this.type = type;
    this.spec = spec;
  }
}
