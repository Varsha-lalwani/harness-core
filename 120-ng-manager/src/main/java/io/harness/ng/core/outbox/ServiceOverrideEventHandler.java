package io.harness.ng.core.outbox;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.audit.beans.AuthenticationInfoDTO.fromSecurityPrincipal;
import static io.harness.ng.core.utils.NGYamlUtils.getYamlString;
import static io.harness.security.PrincipalContextData.PRINCIPAL_CONTEXT;

import static io.serializer.HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.ng.core.events.OutboxEventConstants;
import io.harness.ng.core.events.ServiceOverrideDeleteEvent;
import io.harness.ng.core.events.ServiceOverrideUpsertEvent;
import io.harness.ng.core.serviceoverride.beans.ServiceOverrideRequest;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.security.PrincipalContextData;
import io.harness.security.dto.Principal;
import io.harness.security.dto.ServicePrincipal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class ServiceOverrideEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final AuditClientService auditClientService;

  @Inject
  ServiceOverrideEventHandler(AuditClientService auditClientService) {
    this.auditClientService = auditClientService;
    this.objectMapper = NG_DEFAULT_OBJECT_MAPPER;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case OutboxEventConstants.SERVICE_OVERRIDE_UPSERTED:
          return handlerServiceOverrideUpserted(outboxEvent);
        case OutboxEventConstants.SERVICE_OVERRIDE_DELETED:
          return handlerServiceOverrideDeleted(outboxEvent);
        default:
          return false;
      }

    } catch (IOException ex) {
      return false;
    }
  }
  private boolean handlerServiceOverrideUpserted(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    ServiceOverrideUpsertEvent serviceOverrideUpsertEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ServiceOverrideUpsertEvent.class);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.UPSERT)
            .module(ModuleType.CORE)
            .insertId(outboxEvent.getId())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .timestamp(outboxEvent.getCreatedAt())
            .newYaml(getYamlString(ServiceOverrideRequest.builder()
                                       .serviceOverride(serviceOverrideUpsertEvent.getServiceOverride())
                                       .build()))
            .build();

    Principal principal = null;

    if (globalContext.get(PRINCIPAL_CONTEXT) == null) {
      principal = new ServicePrincipal(NG_MANAGER.getServiceId());
    } else {
      principal = ((PrincipalContextData) globalContext.get(PRINCIPAL_CONTEXT)).getPrincipal();
    }
    return auditClientService.publishAudit(auditEntry, fromSecurityPrincipal(principal), globalContext);
  }
  private boolean handlerServiceOverrideDeleted(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    ServiceOverrideDeleteEvent serviceOverrideDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), ServiceOverrideDeleteEvent.class);
    AuditEntry auditEntry =
        AuditEntry.builder()
            .action(Action.DELETE)
            .module(ModuleType.CORE)
            .insertId(outboxEvent.getId())
            .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
            .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
            .timestamp(outboxEvent.getCreatedAt())
            .oldYaml(getYamlString(ServiceOverrideRequest.builder()
                                       .serviceOverride(serviceOverrideDeleteEvent.getServiceOverride())
                                       .build()))
            .build();

    Principal principal = null;
    if (globalContext.get(PRINCIPAL_CONTEXT) == null) {
      principal = new ServicePrincipal(NG_MANAGER.getServiceId());
    } else {
      principal = ((PrincipalContextData) globalContext.get(PRINCIPAL_CONTEXT)).getPrincipal();
    }
    return auditClientService.publishAudit(auditEntry, fromSecurityPrincipal(principal), globalContext);
  }
}
