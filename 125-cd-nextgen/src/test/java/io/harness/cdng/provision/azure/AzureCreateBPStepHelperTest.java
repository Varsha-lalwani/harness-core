/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.cdng.provision.azure.AzureCreateBPStepConfigurationParameters.AzureCreateBPStepConfigurationParametersBuilder;
import static io.harness.delegate.task.git.GitFetchResponse.GitFetchResponseBuilder;
import static io.harness.rule.OwnerRule.NGONZALEZ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.provision.azure.beans.AzureCreateBPPassThroughData;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.task.azure.arm.AzureBlueprintTaskNGParameters;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({StepUtils.class})
@RunWith(PowerMockRunner.class)
@OwnedBy(HarnessTeam.CDP)
public class AzureCreateBPStepHelperTest extends CategoryTest {
  @Mock private CDStepHelper cdStepHelper;
  @Mock private SecretManagerClientService secretManagerClientService;

  @Mock private K8sStepHelper k8sStepHelper;

  @Mock private StepHelper stepHelper;
  @Mock private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;

  @Mock private AzureCreateBPStepExecutor azureCreateStepExecutor;

  @Mock private AzureCommonHelper azureCommonHelper;

  AzureHelperTest azureHelperTest = new AzureHelperTest();
  @InjectMocks private final AzureCreateBPStepHelper helper = new AzureCreateBPStepHelper();

  @Before
  public void setUpMocks() {
    MockitoAnnotations.initMocks(this);
    ConnectorInfoDTO connectorInfoDTO = azureHelperTest.createAzureConnectorDTO();

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());
    ConnectorInfoDTO gitConnectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(GitConfigDTO.builder().gitConnectionType(GitConnectionType.REPO).build())
            .build();
    doReturn(gitConnectorInfoDTO).when(cdStepHelper).getConnector(any(), any());
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();

    doReturn(sshKeySpecDTO).when(gitConfigAuthenticationInfoHelper).getSSHKey(any(), any(), any(), any());
    List<EncryptedDataDetail> apiEncryptedDataDetails = new ArrayList<>();
    doReturn(apiEncryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(), any());
    doReturn(azureHelperTest.getBPTemplate())
        .when(azureCommonHelper)
        .getTemplateGitFetchFileConfig(any(), any(), any());
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithBlueprint() {
    StepElementParameters stepsParameters = createStepForBP();

    ArgumentCaptor<List<GitFetchFilesConfig>> taskDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    helper.startChainLink(azureCreateStepExecutor, azureHelperTest.getAmbiance(), stepsParameters);
    verify(azureCommonHelper, times(1))
        .getGitFetchFileTaskChainResponse(any(), taskDataArgumentCaptor.capture(), any(), any());
    List<GitFetchFilesConfig> gitFetchRequest = taskDataArgumentCaptor.getValue();
    assertThat(gitFetchRequest.get(0).getIdentifier()).isEqualTo("bluePrint");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testHandleGitFetchResponseForBP() {
    StepElementParameters stepsParameters = createStepForBP();
    GitFetchResponseBuilder gitFetchResponseBuilder = GitFetchResponse.builder();
    gitFetchResponseBuilder.filesFromMultipleRepo(azureHelperTest.createFetchFilesResultMap(true, false, false));
    doReturn(azureHelperTest.createAzureConnectorDTO().getConnectorConfig())
        .when(azureCommonHelper)
        .getAzureConnectorConfig(any(), any());

    ArgumentCaptor<AzureBlueprintTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(AzureBlueprintTaskNGParameters.class);
    helper.handleGitFetchResponse(azureCreateStepExecutor, azureHelperTest.getAmbiance(), stepsParameters,
        AzureCreateBPPassThroughData.builder().build(), gitFetchResponseBuilder.build());
    verify(azureCreateStepExecutor, times(1)).executeCreateTask(any(), any(), taskDataArgumentCaptor.capture(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    AzureBlueprintTaskNGParameters azureTaskNGParameters = taskDataArgumentCaptor.getValue();
    assertThat(azureTaskNGParameters).isExactlyInstanceOf(AzureBlueprintTaskNGParameters.class);
    assertThat(azureTaskNGParameters.getBlueprintJson()).isEqualTo("bluePrint");
    assertThat(azureTaskNGParameters.getAssignmentJson()).isEqualTo("AssignContent");
    assertThat(azureTaskNGParameters.getArtifacts().get("superfile")).isEqualTo("ArtifactsContent");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testExecuteNextLinkForBP() throws Exception {
    StepElementParameters stepsParameters = createStepForBP();
    GitFetchResponseBuilder gitFetchResponseBuilder = GitFetchResponse.builder();
    doReturn(azureHelperTest.createAzureConnectorDTO().getConnectorConfig())
        .when(azureCommonHelper)
        .getAzureConnectorConfig(any(), any());
    gitFetchResponseBuilder.filesFromMultipleRepo(azureHelperTest.createFetchFilesResultMap(true, false, false));
    helper.executeNextLink(azureCreateStepExecutor, azureHelperTest.getAmbiance(), stepsParameters,
        AzureCreateBPPassThroughData.builder().build(), gitFetchResponseBuilder::build);
    ArgumentCaptor<AzureBlueprintTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(AzureBlueprintTaskNGParameters.class);
    verify(azureCreateStepExecutor, times(1)).executeCreateTask(any(), any(), taskDataArgumentCaptor.capture(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    AzureBlueprintTaskNGParameters azureTaskNGParameters = taskDataArgumentCaptor.getValue();
    assertThat(azureTaskNGParameters).isExactlyInstanceOf(AzureBlueprintTaskNGParameters.class);
    assertThat(azureTaskNGParameters.getBlueprintJson()).isEqualTo("bluePrint");
    assertThat(azureTaskNGParameters.getAssignmentJson()).isEqualTo("AssignContent");
    assertThat(azureTaskNGParameters.getArtifacts().get("superfile")).isEqualTo("ArtifactsContent");
  }

  private StepElementParameters createStepForBP() {
    AzureCreateBPStepParameters stepParameters = new AzureCreateBPStepParameters();
    AzureCreateBPStepConfigurationParametersBuilder configurationParameters =
        AzureCreateBPStepConfigurationParameters.builder();
    configurationParameters.connectorRef(ParameterField.createValueField("connectorRef"));

    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFile = new AzureCreateTemplateFile();
    templateFile.setStore(templateStore);

    configurationParameters.scope(ParameterField.createValueField("scope"));
    configurationParameters.assignmentName(ParameterField.createValueField("assignment_name"));
    configurationParameters.templateFile(templateFile);

    stepParameters.setConfiguration(configurationParameters.build());

    return StepElementParameters.builder().spec(stepParameters).build();
  }
}