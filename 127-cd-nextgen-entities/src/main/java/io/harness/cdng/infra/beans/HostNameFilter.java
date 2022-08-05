package io.harness.cdng.infra.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.pdcconnector.HostFilterType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@JsonTypeName("HostNames")
@TypeAlias("HostNameFilter")
@RecasterAlias("io.harness.cdng.infra.beans.HostNameFilter")
public class HostNameFilter implements Filter {
  List<String> value;

  @Override
  public HostFilterType getType() {
    return HostFilterType.HOST_NAMES;
  }
}
