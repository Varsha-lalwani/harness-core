package io.harness.ng.core.outbox;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.audit.beans.AuthenticationInfoDTO.fromSecurityPrincipal;
import static io.harness.security.PrincipalContextData.PRINCIPAL_CONTEXT;

import static io.serializer.HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;

import io.harness.ModuleType;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.ng.core.events.InfrastructureCreateEvent;
import io.harness.ng.core.events.InfrastructureDeleteEvent;
import io.harness.ng.core.events.InfrastructureUpdateEvent;
import io.harness.ng.core.events.InfrastructureUpsertEvent;
import io.harness.ng.core.events.OutboxEventConstants;
import io.harness.ng.core.infrastructure.entity.InfrastructureEntity;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.security.PrincipalContextData;
import io.harness.security.dto.Principal;
import io.harness.security.dto.ServicePrincipal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;

public class InfrastructureEventHandler implements OutboxEventHandler {
  private final ObjectMapper objectMapper;
  private final AuditClientService auditClientService;

  @Inject
  InfrastructureEventHandler(AuditClientService auditClientService) {
    this.auditClientService = auditClientService;
    this.objectMapper = NG_DEFAULT_OBJECT_MAPPER;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case OutboxEventConstants.INFRASTRUCTURE_DEF_CREATED:
          return handlerInfrastructureCreated(outboxEvent);
        case OutboxEventConstants.INFRASTRUCTURE_DEF_UPSERTED:
          return handlerInfrastructureUpserted(outboxEvent);
        case OutboxEventConstants.INFRASTRUCTURE_DEF_UPDATED:
          return handlerInfrastructureUpdated(outboxEvent);
        case OutboxEventConstants.INFRASTRUCTURE_DEF_DELETED:
          return handlerInfrastructureDeleted(outboxEvent);
        default:
          return false;
      }

    } catch (IOException ex) {
      return false;
    }
  }

  private boolean handlerInfrastructureCreated(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    InfrastructureCreateEvent infrastructureCreateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), InfrastructureCreateEvent.class);
    final InfrastructureEntity infrastructure = infrastructureCreateEvent.getInfrastructureEntity();
    final AuditEntry auditEntry = AuditEntry.builder()
                                      .action(Action.CREATE)
                                      .module(ModuleType.CD)
                                      .insertId(outboxEvent.getId())
                                      .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                      .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                      .timestamp(outboxEvent.getCreatedAt())
                                      .newYaml(infrastructure.getYaml())
                                      .build();

    Principal principal = null;
    if (globalContext.get(PRINCIPAL_CONTEXT) == null) {
      principal = new ServicePrincipal(NG_MANAGER.getServiceId());
    } else {
      principal = ((PrincipalContextData) globalContext.get(PRINCIPAL_CONTEXT)).getPrincipal();
    }
    return auditClientService.publishAudit(auditEntry, fromSecurityPrincipal(principal), globalContext);
  }

  private boolean handlerInfrastructureUpserted(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    InfrastructureUpsertEvent infrastructureUpsertEvent =
        objectMapper.readValue(outboxEvent.getEventData(), InfrastructureUpsertEvent.class);
    final InfrastructureEntity infrastructure = infrastructureUpsertEvent.getInfrastructureEntity();
    final AuditEntry auditEntry = AuditEntry.builder()
                                      .action(Action.UPSERT)
                                      .module(ModuleType.CD)
                                      .insertId(outboxEvent.getId())
                                      .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                      .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                      .timestamp(outboxEvent.getCreatedAt())
                                      .newYaml(infrastructure.getYaml())
                                      .build();

    Principal principal = null;
    if (globalContext.get(PRINCIPAL_CONTEXT) == null) {
      principal = new ServicePrincipal(NG_MANAGER.getServiceId());
    } else {
      principal = ((PrincipalContextData) globalContext.get(PRINCIPAL_CONTEXT)).getPrincipal();
    }
    return auditClientService.publishAudit(auditEntry, fromSecurityPrincipal(principal), globalContext);
  }
  private boolean handlerInfrastructureUpdated(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    InfrastructureUpdateEvent infrastructureUpdateEvent =
        objectMapper.readValue(outboxEvent.getEventData(), InfrastructureUpdateEvent.class);
    final InfrastructureEntity newInfrastructure = infrastructureUpdateEvent.getNewInfrastructureEntity();
    final InfrastructureEntity oldInfrastructure = infrastructureUpdateEvent.getOldInfrastructureEntity();
    final AuditEntry auditEntry = AuditEntry.builder()
                                      .action(Action.UPDATE)
                                      .module(ModuleType.CD)
                                      .insertId(outboxEvent.getId())
                                      .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                      .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                      .timestamp(outboxEvent.getCreatedAt())
                                      .newYaml(newInfrastructure.getYaml())
                                      .oldYaml(oldInfrastructure.getYaml())
                                      .build();

    Principal principal = null;
    if (globalContext.get(PRINCIPAL_CONTEXT) == null) {
      principal = new ServicePrincipal(NG_MANAGER.getServiceId());
    } else {
      principal = ((PrincipalContextData) globalContext.get(PRINCIPAL_CONTEXT)).getPrincipal();
    }
    return auditClientService.publishAudit(auditEntry, fromSecurityPrincipal(principal), globalContext);
  }
  private boolean handlerInfrastructureDeleted(OutboxEvent outboxEvent) throws IOException {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    InfrastructureDeleteEvent infrastructureDeleteEvent =
        objectMapper.readValue(outboxEvent.getEventData(), InfrastructureDeleteEvent.class);
    final InfrastructureEntity infrastructure = infrastructureDeleteEvent.getInfrastructureEntity();
    final AuditEntry auditEntry = AuditEntry.builder()
                                      .action(Action.DELETE)
                                      .module(ModuleType.CD)
                                      .insertId(outboxEvent.getId())
                                      .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                      .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                      .timestamp(outboxEvent.getCreatedAt())
                                      .oldYaml(infrastructure.getYaml())
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
