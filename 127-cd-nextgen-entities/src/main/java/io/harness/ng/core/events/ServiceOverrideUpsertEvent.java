package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.audit.ResourceTypeConstants.SERVICE_OVERRIDE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.serviceoverride.beans.NGServiceOverridesEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(CDP)
@Getter
@Builder
@AllArgsConstructor
public class ServiceOverrideUpsertEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String environmentRef;
  private String serviceRef;
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
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, environmentRef + " " + serviceRef);
    labels.put(ResourceConstants.LABEL_KEY_ENV_IDENTIFIER, environmentRef);
    labels.put(ResourceConstants.LABEL_KEY_SERVICE_IDENTIFIER, serviceRef);
    return Resource.builder()
        .identifier(environmentRef + "-" + serviceRef)
        .labels(labels)
        .type(SERVICE_OVERRIDE)
        .build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return OutboxEventConstants.SERVICE_OVERRIDE_UPSERTED;
  }
}
