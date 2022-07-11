/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.rule.OwnerRule.NGONZALEZ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.provision.azure.beans.AzureCreatePassThroughData;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsS3BucketResponse;
import io.harness.delegate.beans.connector.azureconnector.AzureAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureClientSecretKeyDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialSpecDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureSecretType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.task.azure.arm.AzureARMTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureBlueprintTaskNGParameters;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;

import software.wings.beans.TaskType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({StepUtils.class})
@RunWith(PowerMockRunner.class)
@OwnedBy(HarnessTeam.CDP)
public class AzureCreateStepHelperTest extends CategoryTest {
  @Mock private CDStepHelper cdStepHelper;
  @Mock private SecretManagerClientService secretManagerClientService;

  @Mock private K8sStepHelper k8sStepHelper;

  @Mock private StepHelper stepHelper;
  @Mock private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;

  @Mock private AzureCreateStepExecutor azureCreateStepExecutor;
  @InjectMocks private final AzureCreateStepHelper helper = new AzureCreateStepHelper();

  @Before
  public void setUpMocks() {
    MockitoAnnotations.initMocks(this);
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(
                AzureConnectorDTO.builder()
                    .azureEnvironmentType(AzureEnvironmentType.AZURE_US_GOVERNMENT)
                    .credential(
                        AzureCredentialDTO.builder()
                            .azureCredentialType(AzureCredentialType.MANUAL_CREDENTIALS)
                            .config(AzureManualDetailsDTO.builder()
                                        .clientId("client-id")
                                        .tenantId("tenant-id")
                                        .authDTO(AzureAuthDTO.builder()
                                                     .azureSecretType(AzureSecretType.SECRET_KEY)
                                                     .credentials(
                                                         AzureClientSecretKeyDTO.builder()
                                                             .secretKey(SecretRefData.builder()
                                                                            .decryptedValue("secret-key".toCharArray())
                                                                            .build())
                                                             .build())
                                                     .build())
                                        .build())
                            .build())
                    .build())
            .build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());
    ConnectorInfoDTO gitConnectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(GitConfigDTO.builder().gitConnectionType(GitConnectionType.REPO).build())
            .build();
    doReturn(gitConnectorInfoDTO).when(k8sStepHelper).getConnector(any(), any());
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();

    doReturn(sshKeySpecDTO).when(gitConfigAuthenticationInfoHelper).getSSHKey(any(), any(), any(), any());
    List<EncryptedDataDetail> apiEncryptedDataDetails = new ArrayList<>();
    doReturn(apiEncryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(), any());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithoutAzureConnector() {
    StepElementParameters stepsParameters = createStepForARM(true, true, false, "RESOURCE_GROUP");
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorConfig(AppDynamicsConnectorDTO.builder().build()).build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());
    helper.startChainLink(azureCreateStepExecutor, getAmbiance(), stepsParameters);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithRemoteTemplateFiles() {
    StepElementParameters stepsParameters = createStepForARM(true, true, true, "RESOURCE_GROUP");
    mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);
    TaskChainResponse response = helper.startChainLink(azureCreateStepExecutor, getAmbiance(), stepsParameters);
    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptor.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    GitFetchRequest gitFetchRequest = (GitFetchRequest) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(gitFetchRequest).isExactlyInstanceOf(GitFetchRequest.class);
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().size()).isEqualTo(2);
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().get(0).getIdentifier()).isEqualTo("parameterFile");
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().get(1).getIdentifier()).isEqualTo("templateFile");
    assertThat(taskDataArgumentCaptor.getValue().getTaskType()).isEqualTo(TaskType.GIT_FETCH_NEXT_GEN_TASK.name());
    assertThat(response.getTaskRequest()).isNotNull();
    assertThat(response.getPassThroughData()).isNotNull();
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithInlineTemplateFiles() {
    StepElementParameters stepsParameters = createStepForARM(false, true, true, "RESOURCE_GROUP");
    mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);
    TaskChainResponse response = helper.startChainLink(azureCreateStepExecutor, getAmbiance(), stepsParameters);
    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptor.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    GitFetchRequest gitFetchRequest = (GitFetchRequest) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(gitFetchRequest).isExactlyInstanceOf(GitFetchRequest.class);
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().size()).isEqualTo(1);
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().get(0).getIdentifier()).isEqualTo("parameterFile");
    assertThat(taskDataArgumentCaptor.getValue().getTaskType()).isEqualTo(TaskType.GIT_FETCH_NEXT_GEN_TASK.name());
    assertThat(response.getTaskRequest()).isNotNull();
    assertThat(response.getPassThroughData()).isNotNull();
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithInlineFilesAndResourceGroup() {
    StepElementParameters stepsParameters = createStepForARM(false, false, true, "RESOURCE_GROUP");
    ArgumentCaptor<AzureARMTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(AzureARMTaskNGParameters.class);
    helper.startChainLink(azureCreateStepExecutor, getAmbiance(), stepsParameters);
    verify(azureCreateStepExecutor, times(1)).executeCreateTask(any(), any(), taskDataArgumentCaptor.capture(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    AzureARMTaskNGParameters azureTaskNGParameters = taskDataArgumentCaptor.getValue();
    assertThat(azureTaskNGParameters).isExactlyInstanceOf(AzureARMTaskNGParameters.class);
    assertThat(azureTaskNGParameters.getParametersBody()).isEqualTo("parameters");
    assertThat(azureTaskNGParameters.getTemplateBody()).isEqualTo("template");
    assertThat(azureTaskNGParameters.getResourceGroupName()).isEqualTo("resource_group");
    assertThat(azureTaskNGParameters.getSubscriptionId()).isEqualTo("subscription");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithInlineFilesAndSubscription() {
    StepElementParameters stepsParameters = createStepForARM(false, false, true, "SUBSCRIPTION");
    ArgumentCaptor<AzureARMTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(AzureARMTaskNGParameters.class);
    helper.startChainLink(azureCreateStepExecutor, getAmbiance(), stepsParameters);
    verify(azureCreateStepExecutor, times(1)).executeCreateTask(any(), any(), taskDataArgumentCaptor.capture(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    AzureARMTaskNGParameters azureTaskNGParameters = taskDataArgumentCaptor.getValue();
    assertThat(azureTaskNGParameters).isExactlyInstanceOf(AzureARMTaskNGParameters.class);
    assertThat(azureTaskNGParameters.getParametersBody()).isEqualTo("parameters");
    assertThat(azureTaskNGParameters.getTemplateBody()).isEqualTo("template");
    assertThat(azureTaskNGParameters.getDeploymentDataLocation()).isEqualTo("deployment_data_location");
    assertThat(azureTaskNGParameters.getSubscriptionId()).isEqualTo("subscription");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithInlineFilesManagementGroup() {
    StepElementParameters stepsParameters = createStepForARM(false, false, true, "MANAGEMENT_GROUP");
    ArgumentCaptor<AzureARMTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(AzureARMTaskNGParameters.class);
    helper.startChainLink(azureCreateStepExecutor, getAmbiance(), stepsParameters);
    verify(azureCreateStepExecutor, times(1)).executeCreateTask(any(), any(), taskDataArgumentCaptor.capture(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    AzureARMTaskNGParameters azureTaskNGParameters = taskDataArgumentCaptor.getValue();
    assertThat(azureTaskNGParameters).isExactlyInstanceOf(AzureARMTaskNGParameters.class);
    assertThat(azureTaskNGParameters.getParametersBody()).isEqualTo("parameters");
    assertThat(azureTaskNGParameters.getTemplateBody()).isEqualTo("template");
    assertThat(azureTaskNGParameters.getDeploymentDataLocation()).isEqualTo("deployment_data_location");
    assertThat(azureTaskNGParameters.getManagementGroupId()).isEqualTo("management_group");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithBlueprint() {
    StepElementParameters stepsParameters = createStepForBP();
    mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);
    TaskChainResponse response = helper.startChainLink(azureCreateStepExecutor, getAmbiance(), stepsParameters);
    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptor.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    GitFetchRequest gitFetchRequest = (GitFetchRequest) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(gitFetchRequest).isExactlyInstanceOf(GitFetchRequest.class);
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().size()).isEqualTo(1);
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().get(0).getIdentifier()).isEqualTo("bluePrint");
    assertThat(taskDataArgumentCaptor.getValue().getTaskType()).isEqualTo(TaskType.GIT_FETCH_NEXT_GEN_TASK.name());
    assertThat(response.getTaskRequest()).isNotNull();
    assertThat(response.getPassThroughData()).isNotNull();
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testHandleGitFetchResponseForBP() {
    StepElementParameters stepsParameters = createStepForBP();
    GitFetchResponse.GitFetchResponseBuilder gitFetchResponseBuilder = GitFetchResponse.builder();
    gitFetchResponseBuilder.filesFromMultipleRepo(createFetchFilesResultMap(true, false, false));
    AzureCreateStepConfigurationParameters
        .AzureCreateStepConfigurationParametersBuilder azureCreateStepConfigurationParametersBuilder =
        AzureCreateStepConfigurationParameters.builder();
    azureCreateStepConfigurationParametersBuilder.azureDeploymentType(AzureBluePrintDeploymentSpec.builder().build());

    ArgumentCaptor<AzureBlueprintTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(AzureBlueprintTaskNGParameters.class);
    helper.handleGitFetchResponse(azureCreateStepExecutor, getAmbiance(), stepsParameters,
        azureCreateStepConfigurationParametersBuilder.build(), AzureCreatePassThroughData.builder().build(),
        gitFetchResponseBuilder.build());
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
  public void testHandleGitFetchResponseForARM() {
    StepElementParameters stepsParameters = createStepForARM(true, true, true, "RESOURCE_GROUP");
    GitFetchResponse.GitFetchResponseBuilder gitFetchResponseBuilder = GitFetchResponse.builder();
    gitFetchResponseBuilder.filesFromMultipleRepo(createFetchFilesResultMap(false, true, true));
    AzureCreateStepConfigurationParameters
        .AzureCreateStepConfigurationParametersBuilder azureCreateStepConfigurationParametersBuilder =
        AzureCreateStepConfigurationParameters.builder();
    azureCreateStepConfigurationParametersBuilder.azureDeploymentType(AzureARMDeploymentSpec.builder().build());

    ArgumentCaptor<AzureARMTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(AzureARMTaskNGParameters.class);
    helper.handleGitFetchResponse(azureCreateStepExecutor, getAmbiance(), stepsParameters,
        azureCreateStepConfigurationParametersBuilder.build(), AzureCreatePassThroughData.builder().build(),
        gitFetchResponseBuilder.build());
    verify(azureCreateStepExecutor, times(1)).executeCreateTask(any(), any(), taskDataArgumentCaptor.capture(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    AzureARMTaskNGParameters azureTaskNGParameters = taskDataArgumentCaptor.getValue();
    assertThat(azureTaskNGParameters).isExactlyInstanceOf(AzureARMTaskNGParameters.class);
    assertThat(azureTaskNGParameters.getTemplateBody()).isEqualTo("templateFile");
    assertThat(azureTaskNGParameters.getParametersBody()).isEqualTo("parameterFile");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testExecuteNextLinkForARM() throws Exception {
    StepElementParameters stepsParameters = createStepForARM(true, true, true, "RESOURCE_GROUP");
    GitFetchResponse.GitFetchResponseBuilder gitFetchResponseBuilder = GitFetchResponse.builder();
    gitFetchResponseBuilder.filesFromMultipleRepo(createFetchFilesResultMap(false, true, true));
    helper.executeNextLink(azureCreateStepExecutor, getAmbiance(), stepsParameters,
        AzureCreatePassThroughData.builder().build(), gitFetchResponseBuilder::build);
    ArgumentCaptor<AzureARMTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(AzureARMTaskNGParameters.class);
    verify(azureCreateStepExecutor).executeCreateTask(any(), any(), taskDataArgumentCaptor.capture(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    AzureARMTaskNGParameters azureTaskNGParameters = taskDataArgumentCaptor.getValue();
    assertThat(azureTaskNGParameters).isExactlyInstanceOf(AzureARMTaskNGParameters.class);
    assertThat(azureTaskNGParameters.getTemplateBody()).isEqualTo("templateFile");
    assertThat(azureTaskNGParameters.getParametersBody()).isEqualTo("parameterFile");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testExecuteNextLinkForBP() throws Exception {
    StepElementParameters stepsParameters = createStepForBP();
    GitFetchResponse.GitFetchResponseBuilder gitFetchResponseBuilder = GitFetchResponse.builder();
    gitFetchResponseBuilder.filesFromMultipleRepo(createFetchFilesResultMap(true, false, false));
    helper.executeNextLink(azureCreateStepExecutor, getAmbiance(), stepsParameters,
        AzureCreatePassThroughData.builder().build(), gitFetchResponseBuilder::build);
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

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testExecuteNextLinkForInvalidResponse() throws Exception {
    StepElementParameters stepsParameters = createStepForARM(true, true, true, "RESOURCE_GROUP");
    AwsS3BucketResponse awsS3BucketResponse = AwsS3BucketResponse.builder().build();
    TaskChainResponse response = helper.executeNextLink(azureCreateStepExecutor, getAmbiance(), stepsParameters,
        AzureCreatePassThroughData.builder().build(), () -> awsS3BucketResponse);
    assertThat(response.isChainEnd());
  }

  private Map<String, FetchFilesResult> createFetchFilesResultMap(
      boolean withBP, boolean withParameter, boolean withTemplate) {
    Map<String, FetchFilesResult> fetchFilesResultMap = new HashMap<>();
    FetchFilesResult.FetchFilesResultBuilder fetchFilesResult = FetchFilesResult.builder();
    if (withBP) {
      fetchFilesResultMap.put("bluePrint",
          fetchFilesResult
              .files(new ArrayList<>(
                  Arrays.asList(GitFile.builder().fileContent("bluePrint").filePath("blueprint.json").build(),
                      GitFile.builder().fileContent("AssignContent").filePath("assign.json").build(),
                      GitFile.builder().filePath("artifacts/superfile").fileContent("ArtifactsContent").build())))
              .build());
    }
    if (withParameter) {
      fetchFilesResultMap.put("templateFile",
          fetchFilesResult
              .files(new ArrayList<>(
                  Arrays.asList(GitFile.builder().fileContent("templateFile").filePath("templateFile.json").build())))
              .build());
    }
    if (withTemplate) {
      fetchFilesResultMap.put("parameterFile",
          fetchFilesResult
              .files(new ArrayList<>(
                  Arrays.asList(GitFile.builder().fileContent("parameterFile").filePath("parameterFile.json").build())))
              .build());
    }
    return fetchFilesResultMap;
  }

  private Ambiance getAmbiance() {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "account");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "org");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "project");

    return Ambiance.newBuilder()
        .putAllSetupAbstractions(setupAbstractions)
        .setStageExecutionId("stageExecutionId")
        .build();
  }

  private StepElementParameters createStepForBP() {
    AzureCreateStepParameters stepParameters = new AzureCreateStepParameters();
    AzureCreateStepConfigurationParameters.AzureCreateStepConfigurationParametersBuilder configurationParameters =
        AzureCreateStepConfigurationParameters.builder();
    AzureBluePrintDeploymentSpec.AzureBluePrintDeploymentSpecBuilder azureBluePrintDeploymentSpecBuilder =
        AzureBluePrintDeploymentSpec.builder();
    azureBluePrintDeploymentSpecBuilder.connectorRef("azureConnector");

    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile.AzureCreateTemplateFileBuilder templateFileBuilder = AzureCreateTemplateFile.builder();
    templateFileBuilder.type(AzureCreateTemplateFileTypes.Remote);
    AzureRemoteTemplateFileSpec templateFileSpec = new AzureRemoteTemplateFileSpec();
    templateFileSpec.setStore(templateStore);
    templateFileBuilder.spec(templateFileSpec);
    azureBluePrintDeploymentSpecBuilder.scope(ParameterField.createValueField("scope"));
    azureBluePrintDeploymentSpecBuilder.assignmentName(ParameterField.createValueField("assignment_name"));
    azureBluePrintDeploymentSpecBuilder.templateFile(templateFileBuilder.build());
    configurationParameters.azureDeploymentType(azureBluePrintDeploymentSpecBuilder.build());

    stepParameters.setConfiguration(configurationParameters.build());

    return StepElementParameters.builder().spec(stepParameters).build();
  }
  private StepElementParameters createStepForARM(
      Boolean remoteTemplate, Boolean remoteParameters, Boolean withAzureConnector, String typeOfScope) {
    AzureCreateStepParameters stepParameters = new AzureCreateStepParameters();
    AzureCreateStepConfigurationParameters.AzureCreateStepConfigurationParametersBuilder configurationParameters =
        AzureCreateStepConfigurationParameters.builder();
    AzureARMDeploymentSpec.AzureARMDeploymentSpecBuilder armDeploymentSpec = AzureARMDeploymentSpec.builder();
    armDeploymentSpec.connectorRef(withAzureConnector ? "azureConnector" : null);

    StoreConfigWrapper fileStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("parameters-connector-ref"))
                      .build())
            .build();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile.AzureCreateTemplateFileBuilder templateFileBuilder = AzureCreateTemplateFile.builder();
    AzureCreateParameterFile.AzureCreateParameterFileBuilder parameterFileBuilder = AzureCreateParameterFile.builder();

    if (remoteParameters) {
      parameterFileBuilder.type(AzureARMParametersFileTypes.Remote);
      AzureRemoteParametersFileSpec parametersFileSpec = new AzureRemoteParametersFileSpec();
      parametersFileSpec.setStore(fileStoreConfigWrapper);
      parametersFileSpec.setIdentifier("parameters-identifier");
      parameterFileBuilder.spec(parametersFileSpec);
    } else {
      parameterFileBuilder.type(AzureARMParametersFileTypes.Inline);
      AzureInlineParametersFileSpec parametersFileSpec = new AzureInlineParametersFileSpec();
      parametersFileSpec.setParameterBody(ParameterField.createValueField("parameters"));
      parameterFileBuilder.spec(parametersFileSpec);
    }

    if (remoteTemplate) {
      templateFileBuilder.type(AzureCreateTemplateFileTypes.Remote);
      AzureRemoteTemplateFileSpec templateFileSpec = new AzureRemoteTemplateFileSpec();
      templateFileSpec.setStore(templateStore);
      templateFileBuilder.spec(templateFileSpec);
    } else {
      templateFileBuilder.type(AzureCreateTemplateFileTypes.Inline);
      AzureInlineTemplateFileSpec templateFileSpec = new AzureInlineTemplateFileSpec();
      templateFileSpec.setTemplateBody(ParameterField.createValueField("template"));
      templateFileBuilder.spec(templateFileSpec);
    }
    AzureCreateStepScope.AzureCreateStepScopeBuilder scopeBuilder = AzureCreateStepScope.builder();
    switch (typeOfScope) {
      case "RESOURCE_GROUP":
        AzureResourceGroupSpec resourceGroupSpec = AzureResourceGroupSpec.builder()
                                                       .resourceGroup(ParameterField.createValueField("resource_group"))
                                                       .mode(ParameterField.createValueField("INCREMENTAL"))
                                                       .subscription(ParameterField.createValueField("subscription"))
                                                       .build();

        armDeploymentSpec.scope(scopeBuilder.type(AzureScopeTypesNames.RESOURCE_GROUP).spec(resourceGroupSpec).build());
        break;
      case "SUBSCRIPTION":
        AzureSubscritionSpec subscriptionSpec =
            AzureSubscritionSpec.builder()
                .subscription(ParameterField.createValueField("subscription"))
                .deploymentDataLocation(ParameterField.createValueField("deployment_data_location"))
                .mode(ParameterField.createValueField("INCREMENTAL"))
                .build();
        armDeploymentSpec.scope(scopeBuilder.type(AzureScopeTypesNames.SUBSCRIPTION).spec(subscriptionSpec).build());
        break;
      case "MANAGEMENT_GROUP":
        AzureManagementSpec managementGroupSpec =
            AzureManagementSpec.builder()
                .managementGroupId(ParameterField.createValueField("management_group"))
                .deploymentDataLocation(ParameterField.createValueField("deployment_data_location"))
                .mode(ParameterField.createValueField("INCREMENTAL"))
                .build();
        armDeploymentSpec.scope(
            scopeBuilder.type(AzureScopeTypesNames.MANAGEMENT_GROUP).spec(managementGroupSpec).build());
        break;
      case "TENANT":
        AzureTenantSpec tenantSpec =
            AzureTenantSpec.builder()
                .deploymentDataLocation(ParameterField.createValueField("deployment_data_location"))
                .mode(ParameterField.createValueField("INCREMENTAL"))
                .build();
        armDeploymentSpec.scope(scopeBuilder.type(AzureScopeTypesNames.TENANT).spec(tenantSpec).build());
        break;
      default:
        throw new IllegalArgumentException("Invalid type of scope");
    }
    armDeploymentSpec.templateFile(templateFileBuilder.build());
    armDeploymentSpec.parameters(parameterFileBuilder.build());
    configurationParameters.azureDeploymentType(armDeploymentSpec.build());

    stepParameters.setConfiguration(configurationParameters.build());

    return StepElementParameters.builder().spec(stepParameters).build();
  }
}
