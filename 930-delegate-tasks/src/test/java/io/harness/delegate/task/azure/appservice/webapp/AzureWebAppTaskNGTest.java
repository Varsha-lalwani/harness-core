/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice.webapp;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.rule.OwnerRule.ABOSII;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.azureconnector.AzureAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureClientKeyCertDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureSecretType;
import io.harness.delegate.beans.logstreaming.CommandUnitProgress;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.azure.AzureTestUtils;
import io.harness.delegate.task.azure.appservice.settings.AppSettingsFile;
import io.harness.delegate.task.azure.appservice.settings.EncryptedAppSettingsFile;
import io.harness.delegate.task.azure.appservice.webapp.handler.AzureWebAppRequestHandler;
import io.harness.delegate.task.azure.appservice.webapp.ng.AzureWebAppInfraDelegateConfig;
import io.harness.delegate.task.azure.appservice.webapp.ng.AzureWebAppRequestType;
import io.harness.delegate.task.azure.appservice.webapp.ng.request.AbstractWebAppTaskRequest;
import io.harness.delegate.task.azure.appservice.webapp.ng.request.AzureWebAppSlotDeploymentRequest;
import io.harness.delegate.task.azure.appservice.webapp.ng.request.AzureWebAppTaskRequest;
import io.harness.delegate.task.azure.appservice.webapp.ng.response.AzureWebAppRequestResponse;
import io.harness.delegate.task.azure.appservice.webapp.ng.response.AzureWebAppTaskResponse;
import io.harness.delegate.task.azure.artifact.AzureArtifactConfig;
import io.harness.delegate.task.azure.artifact.AzureContainerArtifactConfig;
import io.harness.delegate.task.azure.common.AzureLogCallbackProvider;
import io.harness.delegate.task.azure.common.AzureLogCallbackProviderFactory;
import io.harness.encryption.SecretRefData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDP)
public class AzureWebAppTaskNGTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private AzureWebAppRequestHandler<AbstractWebAppTaskRequest> azureWebAppRequestHandler;
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;
  @Mock private Map<String, AzureWebAppRequestHandler<? extends AzureWebAppTaskRequest>> requestHandlerMap;
  @Mock private AzureLogCallbackProviderFactory logCallbackProviderFactory;
  @Mock private AzureLogCallbackProvider logCallbackProvider;
  @Mock private SecretDecryptionService decryptionService;
  @Mock private LogCallback logCallback;

  private final DelegateTaskPackage delegateTaskPackage =
      DelegateTaskPackage.builder().data(TaskData.builder().build()).build();

  @InjectMocks
  private AzureWebAppTaskNG azureWebAppTaskNG =
      new AzureWebAppTaskNG(delegateTaskPackage, logStreamingTaskClient, response -> {}, () -> true);

  @Before
  public void setup() {
    doReturn(azureWebAppRequestHandler).when(requestHandlerMap).get(AzureWebAppRequestType.SLOT_DEPLOYMENT.name());
    doReturn(logCallbackProvider)
        .when(logCallbackProviderFactory)
        .createNg(eq(logStreamingTaskClient), any(CommandUnitsProgress.class));
    doReturn(logCallback).when(logCallbackProvider).obtainLogCallback(anyString(), null);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunArrayParameters() {
    assertThatThrownBy(() -> azureWebAppTaskNG.run(new Object[] {null}))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRun() throws Exception {
    final AbstractWebAppTaskRequest abstractWebAppTaskRequest = spy(AbstractWebAppTaskRequest.class);
    final AzureWebAppRequestResponse requestResponse = mock(AzureWebAppRequestResponse.class);

    doReturn(AzureWebAppRequestType.SLOT_DEPLOYMENT).when(abstractWebAppTaskRequest).getRequestType();
    doReturn(requestResponse)
        .when(azureWebAppRequestHandler)
        .handleRequest(abstractWebAppTaskRequest, logCallbackProvider, null);

    AzureWebAppTaskResponse taskResponse = azureWebAppTaskNG.run(abstractWebAppTaskRequest);
    assertThat(taskResponse.getRequestResponse()).isSameAs(requestResponse);
    assertThat(taskResponse.getCommandUnitsProgress()).isNotNull();

    verify(azureWebAppRequestHandler).handleRequest(abstractWebAppTaskRequest, logCallbackProvider, null);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunException() {
    final AbstractWebAppTaskRequest abstractWebAppTaskRequest = spy(AbstractWebAppTaskRequest.class);
    final LinkedHashMap<String, CommandUnitProgress> commandUnitsProgressMap = new LinkedHashMap<>();

    commandUnitsProgressMap.put(
        "Fetch Files", CommandUnitProgress.builder().status(CommandExecutionStatus.SUCCESS).build());
    commandUnitsProgressMap.put(
        "Slot Setup", CommandUnitProgress.builder().status(CommandExecutionStatus.RUNNING).build());

    doReturn(CommandUnitsProgress.builder().commandUnitProgressMap(commandUnitsProgressMap).build())
        .when(abstractWebAppTaskRequest)
        .getCommandUnitsProgress();
    doReturn(AzureWebAppRequestType.SLOT_DEPLOYMENT).when(abstractWebAppTaskRequest).getRequestType();
    doThrow(new RuntimeException("Azure deployment failed"))
        .when(azureWebAppRequestHandler)
        .handleRequest(abstractWebAppTaskRequest, logCallbackProvider, null);

    assertThatThrownBy(() -> azureWebAppTaskNG.run(abstractWebAppTaskRequest))
        .isInstanceOf(TaskNGDataException.class)
        .matches(exception -> {
          TaskNGDataException ngDataException = (TaskNGDataException) exception;
          assertThat(ngDataException.getCommandUnitsProgress().getUnitProgresses()).hasSize(2);
          return true;
        });

    verify(logCallbackProvider).obtainLogCallback("Slot Setup", null);
    verify(logCallback).saveExecutionLog("Failed: [RuntimeException: Azure deployment failed].", ERROR, FAILURE);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunWithDecrypt() throws Exception {
    final AzureWebAppRequestResponse requestResponse = mock(AzureWebAppRequestResponse.class);
    final AzureWebAppInfraDelegateConfig infrastructure = AzureTestUtils.createTestWebAppInfraDelegateConfig();
    final AzureArtifactConfig artifactConfig = AzureTestUtils.createTestAzureContainerConfig();
    final List<EncryptedDataDetail> fileEncryptionDetails = emptyList();
    final AzureWebAppSlotDeploymentRequest taskRequest =
        AzureWebAppSlotDeploymentRequest.builder()
            .accountId("accountId")
            .startupCommand(AppSettingsFile.create(
                EncryptedAppSettingsFile.create(SecretRefData.builder().identifier("startup").build()),
                fileEncryptionDetails))
            .applicationSettings(AppSettingsFile.create(
                EncryptedAppSettingsFile.create(SecretRefData.builder().identifier("application").build()),
                fileEncryptionDetails))
            .connectionStrings(AppSettingsFile.create("test"))
            .artifact(artifactConfig)
            .infrastructure(infrastructure)
            .build();
    final AzureClientKeyCertDTO azureClientKeyCert =
        AzureClientKeyCertDTO.builder().clientCertRef(SecretRefData.builder().build()).build();
    final List<EncryptedDataDetail> encryptedDataDetails = emptyList();

    infrastructure.setAzureConnectorDTO(
        AzureConnectorDTO.builder()
            .credential(AzureCredentialDTO.builder()
                            .azureCredentialType(AzureCredentialType.MANUAL_CREDENTIALS)
                            .config(AzureManualDetailsDTO.builder()
                                        .authDTO(AzureAuthDTO.builder()
                                                     .azureSecretType(AzureSecretType.KEY_CERT)
                                                     .credentials(azureClientKeyCert)
                                                     .build())
                                        .build())
                            .build())
            .build());
    infrastructure.setEncryptionDataDetails(encryptedDataDetails);

    doReturn(requestResponse).when(azureWebAppRequestHandler).handleRequest(taskRequest, logCallbackProvider, null);

    azureWebAppTaskNG.run(taskRequest);

    verify(decryptionService).decrypt(azureClientKeyCert, encryptedDataDetails);
    verify(decryptionService).decrypt(taskRequest.getStartupCommand().getEncryptedFile(), fileEncryptionDetails);
    verify(decryptionService).decrypt(taskRequest.getApplicationSettings().getEncryptedFile(), fileEncryptionDetails);
    verify(decryptionService)
        .decrypt(artifactConfig.getConnectorConfig().getDecryptableEntities().get(0),
            artifactConfig.getEncryptedDataDetails());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunWithDecryptDuplicateEntries() throws Exception {
    // Values are expected to be the same, but there should be 2 different objects
    final AzureClientKeyCertDTO azureClientKeyCert1 =
        AzureClientKeyCertDTO.builder().clientCertRef(SecretRefData.builder().identifier("abc").build()).build();
    final AzureClientKeyCertDTO azureClientKeyCert2 =
        AzureClientKeyCertDTO.builder().clientCertRef(SecretRefData.builder().identifier("abc").build()).build();
    final List<EncryptedDataDetail> encryptedDataDetails = emptyList();
    final AzureConnectorDTO azureInfraConnector =
        AzureConnectorDTO.builder()
            .credential(AzureCredentialDTO.builder()
                            .azureCredentialType(AzureCredentialType.MANUAL_CREDENTIALS)
                            .config(AzureManualDetailsDTO.builder()
                                        .authDTO(AzureAuthDTO.builder()
                                                     .azureSecretType(AzureSecretType.KEY_CERT)
                                                     .credentials(azureClientKeyCert1)
                                                     .build())
                                        .build())
                            .build())
            .build();
    final AzureConnectorDTO azureArtifactConnector =
        AzureConnectorDTO.builder()
            .credential(AzureCredentialDTO.builder()
                            .azureCredentialType(AzureCredentialType.MANUAL_CREDENTIALS)
                            .config(AzureManualDetailsDTO.builder()
                                        .authDTO(AzureAuthDTO.builder()
                                                     .azureSecretType(AzureSecretType.KEY_CERT)
                                                     .credentials(azureClientKeyCert2)
                                                     .build())
                                        .build())
                            .build())
            .build();
    final AzureWebAppRequestResponse requestResponse = mock(AzureWebAppRequestResponse.class);
    final AzureWebAppInfraDelegateConfig infrastructure = AzureTestUtils.createTestWebAppInfraDelegateConfig();
    final AzureArtifactConfig artifactConfig = AzureTestUtils.createTestAzureContainerConfig();
    final AzureWebAppTaskRequest taskRequest = AzureWebAppSlotDeploymentRequest.builder()
                                                   .accountId("accountId")
                                                   .artifact(artifactConfig)
                                                   .infrastructure(infrastructure)
                                                   .build();

    infrastructure.setAzureConnectorDTO(azureInfraConnector);
    ((AzureContainerArtifactConfig) artifactConfig).setConnectorConfig(azureArtifactConnector);
    infrastructure.setEncryptionDataDetails(encryptedDataDetails);

    doReturn(requestResponse).when(azureWebAppRequestHandler).handleRequest(taskRequest, logCallbackProvider, null);

    azureWebAppTaskNG.run(taskRequest);

    verify(decryptionService).decrypt(azureClientKeyCert1, encryptedDataDetails);
    verify(decryptionService).decrypt(azureClientKeyCert2, artifactConfig.getEncryptedDataDetails());
  }
}