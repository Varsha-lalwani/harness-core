package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.audit.ResourceTypeConstants.SERVICE_OVERRIDE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.serviceoverride.beans.NGServiceOverridesEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(CDP)
@Getter
@Builder
@AllArgsConstructor
public class ServiceOverrideDeleteEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private NGServiceOverridesEntity serviceOverride;

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new ProjectScope(
        accountIdentifier, serviceOverride.getOrgIdentifier(), serviceOverride.getProjectIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    return Resource.builder().identifier(serviceOverride.getId()).type(SERVICE_OVERRIDE).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return OutboxEventConstants.SERVICE_OVERRIDE_DELETED;
  }
}
