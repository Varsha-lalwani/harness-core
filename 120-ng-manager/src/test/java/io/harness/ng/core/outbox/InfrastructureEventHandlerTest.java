package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.events.OutboxEventConstants.INFRASTRUCTURE_DEF_CREATED;
import static io.harness.ng.core.events.OutboxEventConstants.INFRASTRUCTURE_DEF_DELETED;
import static io.harness.ng.core.events.OutboxEventConstants.INFRASTRUCTURE_DEF_UPDATED;
import static io.harness.ng.core.events.OutboxEventConstants.INFRASTRUCTURE_DEF_UPSERTED;
import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;

import static io.serializer.HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.client.api.AuditClientService;
import io.harness.category.element.UnitTests;
import io.harness.context.GlobalContext;
import io.harness.ng.core.events.InfrastructureCreateEvent;
import io.harness.ng.core.events.InfrastructureDeleteEvent;
import io.harness.ng.core.events.InfrastructureUpdateEvent;
import io.harness.ng.core.events.InfrastructureUpsertEvent;
import io.harness.ng.core.events.ServiceOutboxEvents;
import io.harness.ng.core.infrastructure.entity.InfrastructureEntity;
import io.harness.outbox.OutboxEvent;
import io.harness.rule.Owner;
import io.harness.security.SourcePrincipalContextData;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

@OwnedBy(PL)
public class InfrastructureEventHandlerTest extends CategoryTest {
  private ObjectMapper objectMapper;
  private AuditClientService auditClientService;
  private InfrastructureEventHandler infrastructureEventHandler;

  @Before
  public void setup() {
    objectMapper = NG_DEFAULT_OBJECT_MAPPER;
    auditClientService = mock(AuditClientService.class);
    infrastructureEventHandler = spy(new InfrastructureEventHandler(auditClientService));
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testCreate() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String envIdentifier = randomAlphabetic(10);
    InfrastructureEntity infrastructure = InfrastructureEntity.builder()
                                              .identifier(identifier)
                                              .orgIdentifier(orgIdentifier)
                                              .projectIdentifier(projectIdentifier)
                                              .envIdentifier(envIdentifier)
                                              .build();
    InfrastructureCreateEvent infrastructureCreateEvent = InfrastructureCreateEvent.builder()
                                                              .infrastructureEntity(infrastructure)
                                                              .accountIdentifier(accountIdentifier)
                                                              .orgIdentifier(orgIdentifier)
                                                              .projectIdentifier(projectIdentifier)
                                                              .envIdentifier(envIdentifier)
                                                              .build();

    GlobalContext globalContext = new GlobalContext();
    Principal principal =
        new UserPrincipal(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
    SourcePrincipalContextData sourcePrincipalContextData =
        SourcePrincipalContextData.builder().principal(principal).build();
    globalContext.upsertGlobalContextRecord(sourcePrincipalContextData);
    String eventData = objectMapper.writeValueAsString(infrastructureCreateEvent);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .eventType(INFRASTRUCTURE_DEF_CREATED)
                                  .resourceScope(infrastructureCreateEvent.getResourceScope())
                                  .resource(infrastructureCreateEvent.getResource())
                                  .globalContext(globalContext)
                                  .eventData(eventData)
                                  .createdAt(Long.valueOf(randomNumeric(6)))
                                  .id(randomAlphabetic(10))
                                  .blocked(false)
                                  .build();
    String newYaml = infrastructure.getYaml();
    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any(), any())).thenReturn(true);
    infrastructureEventHandler.handle(outboxEvent);
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any(), any());
    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertAuditEntry(accountIdentifier, orgIdentifier, projectIdentifier, identifier, auditEntry, outboxEvent);
    assertEquals(Action.CREATE, auditEntry.getAction());
    assertNull(auditEntry.getOldYaml());
    assertEquals(newYaml, auditEntry.getNewYaml());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testDelete() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String envIdentifier = randomAlphabetic(10);

    InfrastructureEntity infrastructure = InfrastructureEntity.builder()
                                              .identifier(identifier)
                                              .orgIdentifier(orgIdentifier)
                                              .projectIdentifier(projectIdentifier)
                                              .envIdentifier(envIdentifier)
                                              .build();
    InfrastructureDeleteEvent infrastructureDeleteEvent = InfrastructureDeleteEvent.builder()
                                                              .infrastructureEntity(infrastructure)
                                                              .accountIdentifier(accountIdentifier)
                                                              .orgIdentifier(orgIdentifier)
                                                              .projectIdentifier(projectIdentifier)
                                                              .envIdentifier(envIdentifier)
                                                              .build();

    GlobalContext globalContext = new GlobalContext();
    Principal principal =
        new UserPrincipal(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
    SourcePrincipalContextData sourcePrincipalContextData =
        SourcePrincipalContextData.builder().principal(principal).build();
    globalContext.upsertGlobalContextRecord(sourcePrincipalContextData);
    String eventData = objectMapper.writeValueAsString(infrastructureDeleteEvent);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .eventType(INFRASTRUCTURE_DEF_DELETED)
                                  .resourceScope(infrastructureDeleteEvent.getResourceScope())
                                  .resource(infrastructureDeleteEvent.getResource())
                                  .globalContext(globalContext)
                                  .eventData(eventData)
                                  .createdAt(Long.valueOf(randomNumeric(6)))
                                  .id(randomAlphabetic(10))
                                  .blocked(false)
                                  .build();
    String oldYaml = infrastructure.getYaml();
    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any(), any())).thenReturn(true);
    infrastructureEventHandler.handle(outboxEvent);
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any(), any());
    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertAuditEntry(accountIdentifier, orgIdentifier, projectIdentifier, identifier, auditEntry, outboxEvent);
    assertEquals(Action.DELETE, auditEntry.getAction());
    assertNull(auditEntry.getNewYaml());
    assertEquals(oldYaml, auditEntry.getOldYaml());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testUpdate() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String envIdentifier = randomAlphabetic(10);
    InfrastructureEntity newInfrastructure = InfrastructureEntity.builder()
                                                 .identifier(identifier)
                                                 .orgIdentifier(orgIdentifier)
                                                 .projectIdentifier(projectIdentifier)
                                                 .envIdentifier(envIdentifier)
                                                 .build();
    InfrastructureEntity oldInfrastructure = InfrastructureEntity.builder()
                                                 .identifier(identifier)
                                                 .orgIdentifier(orgIdentifier)
                                                 .projectIdentifier(projectIdentifier)
                                                 .envIdentifier(envIdentifier)
                                                 .build();
    InfrastructureUpdateEvent infrastructureUpdateEvent = InfrastructureUpdateEvent.builder()
                                                              .newInfrastructureEntity(newInfrastructure)
                                                              .oldInfrastructureEntity(oldInfrastructure)
                                                              .accountIdentifier(accountIdentifier)
                                                              .orgIdentifier(orgIdentifier)
                                                              .projectIdentifier(projectIdentifier)
                                                              .build();

    GlobalContext globalContext = new GlobalContext();
    Principal principal =
        new UserPrincipal(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
    SourcePrincipalContextData sourcePrincipalContextData =
        SourcePrincipalContextData.builder().principal(principal).build();
    globalContext.upsertGlobalContextRecord(sourcePrincipalContextData);
    String eventData = objectMapper.writeValueAsString(infrastructureUpdateEvent);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .eventType(INFRASTRUCTURE_DEF_UPDATED)
                                  .resourceScope(infrastructureUpdateEvent.getResourceScope())
                                  .resource(infrastructureUpdateEvent.getResource())
                                  .globalContext(globalContext)
                                  .eventData(eventData)
                                  .createdAt(Long.valueOf(randomNumeric(6)))
                                  .id(randomAlphabetic(10))
                                  .blocked(false)
                                  .build();
    String newYaml = newInfrastructure.getYaml();
    String oldYaml = oldInfrastructure.getYaml();
    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any(), any())).thenReturn(true);
    infrastructureEventHandler.handle(outboxEvent);
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any(), any());
    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertAuditEntry(accountIdentifier, orgIdentifier, projectIdentifier, identifier, auditEntry, outboxEvent);
    assertEquals(Action.UPDATE, auditEntry.getAction());
    assertEquals(oldYaml, auditEntry.getOldYaml());
    assertEquals(newYaml, auditEntry.getNewYaml());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testUpsert() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String envIdentifier = randomAlphabetic(10);
    InfrastructureEntity infrastructure = InfrastructureEntity.builder()
                                              .identifier(identifier)
                                              .orgIdentifier(orgIdentifier)
                                              .projectIdentifier(projectIdentifier)
                                              .envIdentifier(envIdentifier)
                                              .build();
    InfrastructureUpsertEvent infrastructureUpsertEvent = InfrastructureUpsertEvent.builder()
                                                              .infrastructureEntity(infrastructure)
                                                              .accountIdentifier(accountIdentifier)
                                                              .orgIdentifier(orgIdentifier)
                                                              .projectIdentifier(projectIdentifier)
                                                              .envIdentifier(envIdentifier)
                                                              .build();

    GlobalContext globalContext = new GlobalContext();
    Principal principal =
        new UserPrincipal(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
    SourcePrincipalContextData sourcePrincipalContextData =
        SourcePrincipalContextData.builder().principal(principal).build();
    globalContext.upsertGlobalContextRecord(sourcePrincipalContextData);
    String eventData = objectMapper.writeValueAsString(infrastructureUpsertEvent);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .eventType(INFRASTRUCTURE_DEF_UPSERTED)
                                  .resourceScope(infrastructureUpsertEvent.getResourceScope())
                                  .resource(infrastructureUpsertEvent.getResource())
                                  .globalContext(globalContext)
                                  .eventData(eventData)
                                  .createdAt(Long.valueOf(randomNumeric(6)))
                                  .id(randomAlphabetic(10))
                                  .blocked(false)
                                  .build();
    String newYaml = infrastructure.getYaml();
    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    when(auditClientService.publishAudit(any(), any(), any())).thenReturn(true);
    infrastructureEventHandler.handle(outboxEvent);
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any(), any());
    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertAuditEntry(accountIdentifier, orgIdentifier, projectIdentifier, identifier, auditEntry, outboxEvent);
    assertEquals(Action.UPSERT, auditEntry.getAction());
    assertNull(auditEntry.getOldYaml());
    assertEquals(newYaml, auditEntry.getNewYaml());
  }

  private void assertAuditEntry(String accountId, String orgIdentifier, String projectIdentifier, String identifier,
      AuditEntry auditEntry, OutboxEvent outboxEvent) {
    assertNotNull(auditEntry);
    assertEquals(accountId, auditEntry.getResourceScope().getAccountIdentifier());
    assertEquals(orgIdentifier, auditEntry.getResourceScope().getOrgIdentifier());
    assertEquals(projectIdentifier, auditEntry.getResourceScope().getProjectIdentifier());
    assertEquals(auditEntry.getInsertId(), outboxEvent.getId());
    assertEquals(identifier, auditEntry.getResource().getIdentifier());
    assertEquals(ModuleType.CD, auditEntry.getModule());
    assertEquals(outboxEvent.getCreatedAt().longValue(), auditEntry.getTimestamp());
  }
}
