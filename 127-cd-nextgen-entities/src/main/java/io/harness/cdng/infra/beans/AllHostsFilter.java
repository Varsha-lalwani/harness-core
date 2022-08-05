package io.harness.cdng.infra.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.pdcconnector.HostFilterType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@JsonTypeName("All")
@TypeAlias("AllHostsFilter")
@RecasterAlias("io.harness.cdng.infra.beans.AllHostsFilter")
public class AllHostsFilter implements Filter {
  @Override
  public HostFilterType getType() {
    return HostFilterType.ALL;
  }
}
