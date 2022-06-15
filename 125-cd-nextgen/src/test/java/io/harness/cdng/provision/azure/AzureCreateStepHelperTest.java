/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.delegate.task.git.GitFetchResponse.GitFetchResponseBuilder;
import static io.harness.rule.OwnerRule.NGONZALEZ;

import static java.util.Collections.emptyList;
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
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.provision.azure.beans.AzureCreatePassThroughData;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsS3BucketResponse;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.task.azure.arm.AzureARMTaskNGParameters;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;

import java.util.ArrayList;
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

  @Mock private AzureCommonHelper azureCommonHelper;

  AzureHelperTest azureHelperTest = new AzureHelperTest();

  @InjectMocks private final AzureCreateStepHelper helper = new AzureCreateStepHelper();

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

    doReturn(azureHelperTest.getARMTemplate())
        .when(azureCommonHelper)
        .getTemplateGitFetchFileConfig(any(), any(), any());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithoutAzureConnector() {
    StepElementParameters stepsParameters = createStepForARM(true, true, "RESOURCE_GROUP");
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorConfig(AppDynamicsConnectorDTO.builder().build()).build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());
    helper.startChainLink(azureCreateStepExecutor, getAmbiance(), stepsParameters);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithRemoteTemplateFiles() {
    StepElementParameters stepsParameters = createStepForARM(true, true, "RESOURCE_GROUP");
    ArgumentCaptor<ArrayList<GitFetchFilesConfig>> taskDataArgumentCaptor = ArgumentCaptor.forClass(ArrayList.class);
    doReturn(true).when(azureCommonHelper).hasGitStoredParameters(any());
    helper.startChainLink(azureCreateStepExecutor, getAmbiance(), stepsParameters);
    verify(azureCommonHelper, times(1))
        .getGitFetchFileTaskChainResponse(any(), taskDataArgumentCaptor.capture(), any(), any());
    ArrayList<GitFetchFilesConfig> gitFetchRequest = taskDataArgumentCaptor.getValue();
    assertThat(gitFetchRequest.get(0).getIdentifier()).isEqualTo("parameterFile");
    assertThat(gitFetchRequest.get(1).getIdentifier()).isEqualTo("templateFile");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithInlineTemplateFiles() {
    StepElementParameters stepsParameters = createStepForARM(false, true, "RESOURCE_GROUP");
    ArgumentCaptor<ArrayList<GitFetchFilesConfig>> taskDataArgumentCaptor = ArgumentCaptor.forClass(ArrayList.class);
    doReturn(true).when(azureCommonHelper).hasGitStoredParameters(any());
    helper.startChainLink(azureCreateStepExecutor, getAmbiance(), stepsParameters);
    verify(azureCommonHelper, times(1))
        .getGitFetchFileTaskChainResponse(any(), taskDataArgumentCaptor.capture(), any(), any());
    ArrayList<GitFetchFilesConfig> gitFetchRequest = taskDataArgumentCaptor.getValue();
    assertThat(gitFetchRequest.get(0).getIdentifier()).isEqualTo("parameterFile");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithInlineFilesAndResourceGroup() {
    StepElementParameters stepsParameters = createStepForARM(false, false, "RESOURCE_GROUP");
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
    StepElementParameters stepsParameters = createStepForARM(false, false, "SUBSCRIPTION");
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
    StepElementParameters stepsParameters = createStepForARM(false, false, "MANAGEMENT_GROUP");
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
  public void testHandleGitFetchResponseForARM() {
    StepElementParameters stepsParameters = createStepForARM(true, true, "RESOURCE_GROUP");
    GitFetchResponseBuilder gitFetchResponseBuilder = GitFetchResponse.builder();
    gitFetchResponseBuilder.filesFromMultipleRepo(azureHelperTest.createFetchFilesResultMap(false, true, true));
    doReturn(azureHelperTest.createAzureConnectorDTO().getConnectorConfig())
        .when(azureCommonHelper)
        .getAzureConnectorConfig(any(), any());

    ArgumentCaptor<AzureARMTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(AzureARMTaskNGParameters.class);
    helper.handleGitFetchResponse(azureCreateStepExecutor, getAmbiance(), stepsParameters,
        AzureCreatePassThroughData.builder().build(), gitFetchResponseBuilder.build());
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
    StepElementParameters stepsParameters = createStepForARM(true, true, "RESOURCE_GROUP");
    GitFetchResponseBuilder gitFetchResponseBuilder = GitFetchResponse.builder();
    doReturn(azureHelperTest.createAzureConnectorDTO().getConnectorConfig())
        .when(azureCommonHelper)
        .getAzureConnectorConfig(any(), any());
    gitFetchResponseBuilder.filesFromMultipleRepo(azureHelperTest.createFetchFilesResultMap(false, true, true));
    doReturn(emptyList()).when(azureCommonHelper).getAzureEncryptionDetails(any(), any());
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
  public void testExecuteNextLinkForInvalidResponse() throws Exception {
    StepElementParameters stepsParameters = createStepForARM(true, true, "RESOURCE_GROUP");
    AwsS3BucketResponse awsS3BucketResponse = AwsS3BucketResponse.builder().build();
    TaskChainResponse response = helper.executeNextLink(azureCreateStepExecutor, getAmbiance(), stepsParameters,
        AzureCreatePassThroughData.builder().build(), () -> awsS3BucketResponse);
    assertThat(response.isChainEnd());
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

  private StepElementParameters createStepForARM(Boolean remoteTemplate, Boolean remoteParameters, String typeOfScope) {
    AzureCreateStepParameters stepParameters = new AzureCreateStepParameters();
    AzureCreateStepConfigurationParameters.AzureCreateStepConfigurationParametersBuilder configurationParameters =
        AzureCreateStepConfigurationParameters.builder();
    configurationParameters.connectorRef(ParameterField.createValueField("connectorRef"));
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

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    AzureCreateParameterFile parameterFileBuilder = new AzureCreateParameterFile();

    if (remoteParameters) {
      parameterFileBuilder.setStore(fileStoreConfigWrapper);
    } else {
//      parameterFileBuilder.setStore();

    }

    if (remoteTemplate) {
      templateFileBuilder.setStore(templateStore);
    } else {
      //      templateFileBuilder.setStore();
    }
    AzureCreateStepScope.AzureCreateStepScopeBuilder scopeBuilder = AzureCreateStepScope.builder();
    switch (typeOfScope) {
      case "RESOURCE_GROUP":
        AzureResourceGroupSpec resourceGroupSpec = AzureResourceGroupSpec.builder()
                                                       .resourceGroup(ParameterField.createValueField("resource_group"))
                                                       .mode(ParameterField.createValueField("INCREMENTAL"))
                                                       .subscription(ParameterField.createValueField("subscription"))
                                                       .build();

        configurationParameters.scope(
            scopeBuilder.type(AzureScopeTypesNames.RESOURCE_GROUP).spec(resourceGroupSpec).build());
        break;
      case "SUBSCRIPTION":
        AzureSubscriptionSpec subscriptionSpec =
            AzureSubscriptionSpec.builder()
                .subscription(ParameterField.createValueField("subscription"))
                .location(ParameterField.createValueField("deployment_data_location"))
                .build();
        configurationParameters.scope(
            scopeBuilder.type(AzureScopeTypesNames.SUBSCRIPTION).spec(subscriptionSpec).build());
        break;
      case "MANAGEMENT_GROUP":
        AzureManagementSpec managementGroupSpec =
            AzureManagementSpec.builder()
                .managementGroupId(ParameterField.createValueField("management_group"))
                .location(ParameterField.createValueField("deployment_data_location"))
                .build();
        configurationParameters.scope(
            scopeBuilder.type(AzureScopeTypesNames.MANAGEMENT_GROUP).spec(managementGroupSpec).build());
        break;
      case "TENANT":
        AzureTenantSpec tenantSpec =
            AzureTenantSpec.builder()
                .location(ParameterField.createValueField("deployment_data_location"))
                .build();
        configurationParameters.scope(scopeBuilder.type(AzureScopeTypesNames.TENANT).spec(tenantSpec).build());
        break;
      default:
        throw new IllegalArgumentException("Invalid type of scope");
    }
    configurationParameters.templateFile(templateFileBuilder);
    configurationParameters.parameters(parameterFileBuilder);

    stepParameters.setConfiguration(configurationParameters.build());

    return StepElementParameters.builder().spec(stepParameters).build();
  }
}
