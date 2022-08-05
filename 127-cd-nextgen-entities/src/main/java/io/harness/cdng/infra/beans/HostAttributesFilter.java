package io.harness.cdng.infra.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.pdcconnector.HostFilterType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@JsonTypeName("HostAttributes")
@TypeAlias("HostAttributesFilter")
@RecasterAlias("io.harness.cdng.infra.beans.HostAttributesFilter")
public class HostAttributesFilter implements Filter {
  Map<String, String> value;

  @Override
  public HostFilterType getType() {
    return HostFilterType.HOST_ATTRIBUTES;
  }
}
