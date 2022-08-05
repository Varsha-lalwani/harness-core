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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.provision.azure.beans.AzureCreatePassThroughData;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.task.azure.arm.AzureARMTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureARMTaskType;
import io.harness.delegate.task.azure.arm.AzureTaskNGParameters;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;

import software.wings.beans.TaskType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({StepUtils.class})
@RunWith(PowerMockRunner.class)
@OwnedBy(HarnessTeam.CDP)
public class AzureCreateStepTest extends CategoryTest {
  @Mock PipelineRbacHelper pipelineRbacHelper;
  @Mock CDStepHelper cdStepHelper;
  @Mock StepHelper stepHelper;
  @Mock CDFeatureFlagHelper cdFeatureFlagHelper;
  @Captor ArgumentCaptor<List<EntityDetail>> captor;

  @InjectMocks private AzureCreateStep azureCreateStep;
  private static final String CONNECTOR_REF = "test-connector";
  private Ambiance getAmbiance() {
    return Ambiance.newBuilder()
        .putSetupAbstractions("accountId", "test-account")
        .putSetupAbstractions("projectIdentifier", "test-project")
        .putSetupAbstractions("orgIdentifier", "test-org")
        .build();
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testValidateResourcesWithInlineTemplateStore() {
    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), any());

    AzureCreateStepParameters stepParameters = new AzureCreateStepParameters();
    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    AzureCreateParameterFile parameterFileBuilder = new AzureCreateParameterFile();
    StoreConfigWrapper fileStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(
                GithubStore.builder().connectorRef(ParameterField.createValueField("parameters-connector-ref")).build())
            .build();
    parameterFileBuilder.setStore(fileStoreConfigWrapper);
    StoreConfigWrapper inlineStoreConfigWrapper =
        StoreConfigWrapper.builder().spec(HarnessStore.builder().build()).build();
    templateFileBuilder.setStore(inlineStoreConfigWrapper);
    stepParameters.setConfiguration(AzureCreateStepConfigurationParameters.builder()
                                        .templateFile(templateFileBuilder)
                                        .parameters(parameterFileBuilder)
                                        .connectorRef(ParameterField.createValueField("connectorRef"))
                                        .build());
    StepElementParameters step = StepElementParameters.builder().spec(stepParameters).build();
    azureCreateStep.validateResources(getAmbiance(), step);
    verify(pipelineRbacHelper, times(1)).checkRuntimePermissions(eq(getAmbiance()), captor.capture(), eq(true));

    List<EntityDetail> entityDetails = captor.getValue();
    assertThat(entityDetails.size()).isEqualTo(2);
    assertThat(entityDetails.get(1).getEntityRef().getIdentifier()).isEqualTo("connectorRef");
    assertThat(entityDetails.get(1).getEntityRef().getAccountIdentifier()).isEqualTo("test-account");
    assertThat(entityDetails.get(0).getEntityRef().getIdentifier()).isEqualTo("parameters-connector-ref");
    assertThat(entityDetails.get(0).getEntityRef().getAccountIdentifier()).isEqualTo("test-account");
  }

  //  @Test
  //  @Owner(developers = NGONZALEZ)
  //  @Category(UnitTests.class)
  //  public void testValidateResourcesWithInlineParameters() {
  //    AzureCreateStepParameters stepParameters = new AzureCreateStepParameters();
  //    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
  //    AzureCreateParameterFile parameterFileBuilder = new AzureCreateParameterFile();
  //
  //    parametersFileSpec.setParameterBody(ParameterField.createValueField("filebody"));
  //    inlineTemplateFileSpec.setTemplateBody(ParameterField.createValueField("body"));
  //
  //    stepParameters.setConfiguration(AzureCreateStepConfigurationParameters.builder()
  //                                        .templateFile(AzureCreateTemplateFile.builder()
  //                                                          .spec(inlineTemplateFileSpec)
  //                                                          .type(AzureCreateTemplateFileTypes.Inline)
  //                                                          .build())
  //                                        .parameters(AzureCreateParameterFile.builder()
  //                                                        .type(AzureARMParametersFileTypes.Inline)
  //                                                        .spec(parametersFileSpec)
  //                                                        .build())
  //                                        .connectorRef(ParameterField.createValueField("connectorRef"))
  //                                        .build());
  //    StepElementParameters step = StepElementParameters.builder().spec(stepParameters).build();
  //    azureCreateStep.validateResources(getAmbiance(), step);
  //    verify(pipelineRbacHelper, times(1)).checkRuntimePermissions(eq(getAmbiance()), captor.capture(), eq(true));
  //
  //    List<EntityDetail> entityDetails = captor.getValue();
  //    assertThat(entityDetails.size()).isEqualTo(1);
  //    assertThat(entityDetails.get(0).getEntityRef().getIdentifier()).isEqualTo("connectorRef");
  //    assertThat(entityDetails.get(0).getEntityRef().getAccountIdentifier()).isEqualTo("test-account");
  //  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testValidateResourcesWithAllRemote() {
    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), any());

    AzureCreateStepParameters stepParameters = new AzureCreateStepParameters();
    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    AzureCreateParameterFile parameterFileBuilder = new AzureCreateParameterFile();
    StoreConfigWrapper fileStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(
                GithubStore.builder().connectorRef(ParameterField.createValueField("parameters-connector-ref")).build())
            .build();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder().connectorRef(ParameterField.createValueField("template-connector-ref")).build())
            .build();
    parameterFileBuilder.setStore(fileStoreConfigWrapper);
    templateFileBuilder.setStore(templateStore);

    stepParameters.setConfiguration(AzureCreateStepConfigurationParameters.builder()
                                        .templateFile(templateFileBuilder)
                                        .parameters(parameterFileBuilder)
                                        .connectorRef(ParameterField.createValueField("connectorRef"))
                                        .build());
    StepElementParameters step = StepElementParameters.builder().spec(stepParameters).build();
    azureCreateStep.validateResources(getAmbiance(), step);
    verify(pipelineRbacHelper, times(1)).checkRuntimePermissions(eq(getAmbiance()), captor.capture(), eq(true));

    List<EntityDetail> entityDetails = captor.getValue();
    assertThat(entityDetails.size()).isEqualTo(3);
    assertThat(entityDetails.get(2).getEntityRef().getIdentifier()).isEqualTo("connectorRef");
    assertThat(entityDetails.get(2).getEntityRef().getAccountIdentifier()).isEqualTo("test-account");
    assertThat(entityDetails.get(1).getEntityRef().getIdentifier()).isEqualTo("parameters-connector-ref");
    assertThat(entityDetails.get(1).getEntityRef().getAccountIdentifier()).isEqualTo("test-account");
    assertThat(entityDetails.get(0).getEntityRef().getIdentifier()).isEqualTo("template-connector-ref");
    assertThat(entityDetails.get(0).getEntityRef().getAccountIdentifier()).isEqualTo("test-account");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testExecuteAzureCreateStepTask() {
    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), any());

    AzureCreateStepParameters stepParameters = new AzureCreateStepParameters();
    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    AzureCreateParameterFile parameterFileBuilder = new AzureCreateParameterFile();

    StoreConfigWrapper fileStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(
                GithubStore.builder().connectorRef(ParameterField.createValueField("parameters-connector-ref")).build())
            .build();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder().connectorRef(ParameterField.createValueField("template-connector-ref")).build())
            .build();
    parameterFileBuilder.setStore(fileStoreConfigWrapper);
    templateFileBuilder.setStore(templateStore);

    stepParameters.setConfiguration(AzureCreateStepConfigurationParameters.builder()
                                        .templateFile(templateFileBuilder)
                                        .parameters(parameterFileBuilder)
                                        .connectorRef(ParameterField.createValueField("connectorRef"))
                                        .build());
    TaskSelectorYaml taskSelectorYaml = new TaskSelectorYaml("create-d-selector-1");
    stepParameters.setDelegateSelectors(ParameterField.createValueField(Arrays.asList(taskSelectorYaml)));
    StepElementParameters step = StepElementParameters.builder().spec(stepParameters).build();
    Mockito.mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);

    Class<ArrayList<TaskSelector>> delegateSelectors = (Class<ArrayList<TaskSelector>>) (Class) ArrayList.class;
    ArgumentCaptor<ArrayList<TaskSelector>> taskSelectorsArgumentCaptor = ArgumentCaptor.forClass(delegateSelectors);

    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

    AzureARMTaskNGParameters azureTaskNGParameters = AzureARMTaskNGParameters.builder()
                                                         .encryptedDataDetails(encryptedDataDetails)
                                                         .parametersBody("parameters-body")
                                                         .taskType(AzureARMTaskType.ARM_DEPLOYMENT)
                                                         .templateBody("template-body")
                                                         .accountId("test-account")
                                                         .connectorDTO(AzureConnectorDTO.builder().build())
                                                         .deploymentMode(AzureDeploymentMode.INCREMENTAL)
                                                         .subscriptionId("subscription-id")
                                                         .build();
    TaskChainResponse taskChainResponse = azureCreateStep.executeCreateTask(
        getAmbiance(), step, azureTaskNGParameters, AzureCreatePassThroughData.builder().build());
    assertThat(taskChainResponse).isNotNull();
    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(
        any(), taskDataArgumentCaptor.capture(), any(), any(), any(), taskSelectorsArgumentCaptor.capture(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getParameters()).isNotNull();
    AzureTaskNGParameters taskNGParameters =
        (AzureTaskNGParameters) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(taskNGParameters).isNotNull();
    assertThat(taskNGParameters.getAzureARMTaskType()).isEqualTo(AzureARMTaskType.ARM_DEPLOYMENT);
    assertThat(taskDataArgumentCaptor.getValue().getTaskType()).isEqualTo(TaskType.AZURE_NG_ARM_BLUEPRINT.name());
    assertThat(taskSelectorsArgumentCaptor.getValue().get(0).getSelector()).isEqualTo("create-d-selector-1");
  }
  // TODO: Missing test for the finalizeExecutionWithSecurityContext. Do this once the DelegateTask has been
  // implemented.
}
