package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.audit.ResourceTypeConstants.INFRASTRUCTURE_DEF;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.infrastructure.entity.InfrastructureEntity;

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
public class InfrastructureUpdateEvent implements Event {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String envIdentifier;
  private InfrastructureEntity newInfrastructureEntity;
  private InfrastructureEntity oldInfrastructureEntity;

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    return new ProjectScope(
        accountIdentifier, newInfrastructureEntity.getOrgIdentifier(), newInfrastructureEntity.getProjectIdentifier());
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newInfrastructureEntity.getName());
    labels.put(ResourceConstants.LABEL_KEY_ENV_IDENTIFIER, envIdentifier);
    return Resource.builder()
        .identifier(newInfrastructureEntity.getIdentifier())
        .labels(labels)
        .type(INFRASTRUCTURE_DEF)
        .build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return OutboxEventConstants.INFRASTRUCTURE_DEF_UPDATED;
  }
}
