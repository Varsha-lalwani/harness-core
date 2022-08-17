/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.azure.model.ARMScopeType.MANAGEMENT_GROUP;
import static io.harness.azure.model.ARMScopeType.RESOURCE_GROUP;
import static io.harness.azure.model.ARMScopeType.SUBSCRIPTION;
import static io.harness.azure.model.ARMScopeType.TENANT;
import static io.harness.cdng.provision.azure.AzureCommonHelper.AZURE_TEMPLATE_TYPE;
import static io.harness.cdng.provision.azure.AzureCommonHelper.DEFAULT_TIMEOUT;
import static io.harness.cdng.provision.azure.AzureCommonHelper.PARAMETERS_FILE_IDENTIFIER;
import static io.harness.cdng.provision.azure.AzureCommonHelper.TEMPLATE_FILE_IDENTIFIER;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.azure.arm.AzureARMTaskNGParameters.AzureARMTaskNGParametersBuilder;
import static io.harness.delegate.task.azure.arm.AzureARMTaskType.ARM_DEPLOYMENT;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.ARMScopeType;
import io.harness.beans.FeatureName;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.provision.azure.beans.AzureCreateARMResourcePassThroughData;
import io.harness.common.ParameterFieldHelper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.azure.arm.AzureARMTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureTaskNGParameters;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.git.model.FetchFilesResult;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@OwnedBy(CDP)
public class AzureCreateARMResourceStep extends TaskChainExecutableWithRollbackAndRbac {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.AZURE_CREATE_ARM_RESOURCE.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Inject private PipelineRbacHelper pipelineRbacHelper;
  @Inject private KryoSerializer kryoSerializer;

  @Inject private StepHelper stepHelper;

  @Inject private AzureCommonHelper azureCommonHelper;
  @Inject private CDStepHelper cdStepHelper;

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    if (!cdFeatureFlagHelper.isEnabled(AmbianceUtils.getAccountId(ambiance), FeatureName.AZURE_ARM_BP_NG)) {
      throw new AccessDeniedException("Azure NG is not enabled for this account. Please contact harness customer care.",
          ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
    List<EntityDetail> entityDetailList = new ArrayList<>();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);

    // Template file connector
    AzureCreateARMResourceStepConfigurationParameters spec =
        ((AzureCreateARMResourceStepParameters) stepParameters.getSpec()).getConfigurationParameters();
    AzureCreateARMResourceTemplateFile azureCreateTemplateFile = spec.getTemplateFile();

    if (ManifestStoreType.isInGitSubset(azureCreateTemplateFile.getStore().getSpec().getKind())) {
      String connectorRef =
          getParameterFieldValue(azureCreateTemplateFile.getStore().getSpec().getConnectorReference());
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
      EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
      entityDetailList.add(entityDetail);
    }

    if (spec.getParameters() != null
        && ManifestStoreType.isInGitSubset(spec.getParameters().getStore().getSpec().getKind())) {
      String connectorRef = getParameterFieldValue(spec.getParameters().getStore().getSpec().getConnectorReference());
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
      EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
      entityDetailList.add(entityDetail);
    }

    // Azure connector
    String connectorRef = spec.getConnectorRef().getValue();
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
    EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
    entityDetailList.add(entityDetail);

    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetailList, true);
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    AzureCreateARMResourceStepConfigurationParameters stepConfigurationParameters =
        ((AzureCreateARMResourceStepParameters) stepParameters.getSpec()).getConfigurationParameters();
    ConnectorInfoDTO connectorDTO =
        cdStepHelper.getConnector(stepConfigurationParameters.getConnectorRef().getValue(), ambiance);
    if (!(connectorDTO.getConnectorConfig() instanceof AzureConnectorDTO)) {
      throw new InvalidRequestException("Invalid connector selected in Azure step. Select Azure connector");
    }

    List<GitFetchFilesConfig> gitFetchFilesConfigs =
        azureCommonHelper.getParametersGitFetchFileConfigs(ambiance, stepConfigurationParameters);
    AzureCreateARMResourceTemplateFile azureCreateTemplateFile = stepConfigurationParameters.getTemplateFile();
    if (azureCommonHelper.isTemplateStoredOnGit(azureCreateTemplateFile)) {
      gitFetchFilesConfigs.add(getTemplateGitFetchFileConfig(ambiance, stepConfigurationParameters.getTemplateFile()));
    }

    AzureCreateARMResourcePassThroughData passThroughData =
        azureCommonHelper.getAzureCreatePassThroughData(stepConfigurationParameters);
    if (isNotEmpty(gitFetchFilesConfigs)) {
      return azureCommonHelper.getGitFetchFileTaskChainResponse(
          ambiance, gitFetchFilesConfigs, stepParameters, passThroughData);
    }
    String templateBody = null;
    String parametersBody = null;

    //    if (Objects.equals(azureCreateTemplateFile.getStore().getSpec().getKind(), HARNESS_STORE_TYPE)) {
    //      // TODO: Add logic for harness store type
    //    }
    //
    //    if (Objects.equals(
    //            stepConfigurationParameters.getParameters().getStore().getSpec().getKind(), HARNESS_STORE_TYPE)) {
    //      // TODO: Add logic for harness store type
    //    }
    populatePassThroughData(passThroughData, templateBody, parametersBody);
    AzureTaskNGParameters azureARMTaskNGParameters = getAzureTaskNGParams(
        ambiance, stepParameters, (AzureConnectorDTO) connectorDTO.getConnectorConfig(), passThroughData);
    return executeCreateTask(ambiance, stepParameters, azureARMTaskNGParameters, passThroughData);
  }
  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    ResponseData responseData = responseSupplier.get();
    if (responseData instanceof GitFetchResponse) {
      return handleGitFetchResponse(ambiance, stepParameters, passThroughData, (GitFetchResponse) responseData);
    } else {
      String errorMessage = "Unknown Error";
      return TaskChainResponse.builder()
          .chainEnd(true)
          .passThroughData(StepExceptionPassThroughData.builder()
                               .unitProgressData(UnitProgressData.builder().unitProgresses(new ArrayList<>()).build())
                               .errorMessage(errorMessage)
                               .build())
          .build();
    }
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    if (passThroughData instanceof StepExceptionPassThroughData) {
      StepExceptionPassThroughData stepExceptionPassThroughData = (StepExceptionPassThroughData) passThroughData;
      return cdStepHelper.handleStepExceptionFailure(stepExceptionPassThroughData);
    }
    // TODO: To implement after the DelegateTask is implemented.
    return null;
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  private TaskChainResponse executeCreateTask(Ambiance ambiance, StepElementParameters stepParameters,
      AzureTaskNGParameters parameters, PassThroughData passThroughData) {
    TaskData taskData = TaskData.builder()
                            .async(true)
                            .taskType(TaskType.AZURE_NG_ARM.name())
                            .timeout(StepUtils.getTimeoutMillis(stepParameters.getTimeout(), DEFAULT_TIMEOUT))
                            .parameters(new Object[] {parameters})
                            .build();
    final TaskRequest taskRequest = StepUtils.prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        Collections.singletonList(AzureCommandUnit.Create.name()), TaskType.AZURE_NG_ARM.getDisplayName(),
        TaskSelectorYaml.toTaskSelector(
            ((AzureCreateARMResourceStepParameters) stepParameters.getSpec()).getDelegateSelectors()),
        stepHelper.getEnvironmentType(ambiance));

    return TaskChainResponse.builder().taskRequest(taskRequest).passThroughData(passThroughData).chainEnd(true).build();
  }

  private void populatePassThroughData(
      AzureCreateARMResourcePassThroughData passThroughData, String templateBody, String parametersBody) {
    passThroughData.setTemplateBody(templateBody);
    passThroughData.setParametersBody(parametersBody);
  }

  private AzureTaskNGParameters getAzureTaskNGParams(Ambiance ambiance, StepElementParameters stepElementParameters,
      AzureConnectorDTO connectorConfig, PassThroughData passThroughData) {
    AzureCreateARMResourceStepParameters azureCreateStepParameters =
        (AzureCreateARMResourceStepParameters) stepElementParameters.getSpec();
    AzureCreateARMResourcePassThroughData azureCreatePassThroughData =
        (AzureCreateARMResourcePassThroughData) passThroughData;
    AzureCreateARMResourceStepConfigurationParameters stepConfigurationParameters =
        azureCreateStepParameters.getConfigurationParameters();
    AzureARMTaskNGParametersBuilder builder = AzureARMTaskNGParameters.builder();
    builder.accountId(AmbianceUtils.getAccountId(ambiance))
        .taskType(ARM_DEPLOYMENT)
        .templateBody(azureCreatePassThroughData.getTemplateBody())
        .connectorDTO(connectorConfig)
        .parametersBody(azureCreatePassThroughData.getParametersBody())
        .timeoutInMs(StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), DEFAULT_TIMEOUT));

    setScopeTypeValues(builder, stepConfigurationParameters);
    return builder.encryptedDataDetails(azureCommonHelper.getAzureEncryptionDetails(ambiance, connectorConfig)).build();
  }

  private AzureARMTaskNGParametersBuilder setScopeTypeValues(AzureARMTaskNGParametersBuilder builder,
      AzureCreateARMResourceStepConfigurationParameters stepConfigurationParameters) {
    String scopeType = stepConfigurationParameters.getScope().getType();

    switch (scopeType) {
      case AzureScopeTypesNames.ResourceGroup:
        AzureResourceGroupSpec resourceGroupSpec =
            (AzureResourceGroupSpec) stepConfigurationParameters.getScope().getSpec();
        builder.scopeType(fromYamlScopeToInternalScope(stepConfigurationParameters.getScope().getType()))
            .deploymentMode(azureCommonHelper.retrieveDeploymentMode(
                fromYamlScopeToInternalScope(stepConfigurationParameters.getScope().getType()),
                resourceGroupSpec.getMode().toString()))
            .subscriptionId(resourceGroupSpec.getSubscription().getValue())
            .resourceGroupName(resourceGroupSpec.getResourceGroup().getValue());
        break;
      case AzureScopeTypesNames.Subscription:
        AzureSubscriptionSpec subscriptionSpec =
            (AzureSubscriptionSpec) stepConfigurationParameters.getScope().getSpec();
        builder.scopeType(fromYamlScopeToInternalScope(stepConfigurationParameters.getScope().getType()))
            .deploymentDataLocation(subscriptionSpec.getLocation().getValue())
            .deploymentMode(azureCommonHelper.retrieveDeploymentMode(
                fromYamlScopeToInternalScope(stepConfigurationParameters.getScope().getType()), null))
            .subscriptionId(subscriptionSpec.getSubscription().getValue());
        break;
      case AzureScopeTypesNames.ManagementGroup:
        AzureManagementSpec managementSpec = (AzureManagementSpec) stepConfigurationParameters.getScope().getSpec();
        builder.scopeType(fromYamlScopeToInternalScope(stepConfigurationParameters.getScope().getType()))
            .deploymentMode(azureCommonHelper.retrieveDeploymentMode(
                fromYamlScopeToInternalScope(stepConfigurationParameters.getScope().getType()), null))
            .deploymentDataLocation(managementSpec.getLocation().getValue())
            .managementGroupId(managementSpec.getManagementGroupId().getValue());
        break;

      case AzureScopeTypesNames.Tenant:
        AzureTenantSpec tenantSpec = (AzureTenantSpec) stepConfigurationParameters.getScope().getSpec();
        builder.scopeType(fromYamlScopeToInternalScope(stepConfigurationParameters.getScope().getType()))
            .deploymentMode(azureCommonHelper.retrieveDeploymentMode(
                fromYamlScopeToInternalScope(stepConfigurationParameters.getScope().getType()), null))
            .deploymentDataLocation(tenantSpec.getLocation().getValue());
        break;
      default:
        throw new InvalidRequestException(
            "Invalid scope type in Azure step. Select one of the following: ResourceGroup, Subscription, Management, Tenant");
    }
    return builder;
  }

  TaskChainResponse handleGitFetchResponse(Ambiance ambiance, StepElementParameters stepElementParameters,
      PassThroughData passThroughData, GitFetchResponse responseData) {
    Map<String, FetchFilesResult> filesFromMultipleRepo = responseData.getFilesFromMultipleRepo();
    AzureCreateARMResourceStepParameters spec = (AzureCreateARMResourceStepParameters) stepElementParameters.getSpec();
    String templateBody = null;
    String parametersBody = null;
    // If the step is ARM, retrieve the templateBody and parametersBody from git or the inline fields
    if (filesFromMultipleRepo.get(PARAMETERS_FILE_IDENTIFIER) != null) {
      parametersBody = filesFromMultipleRepo.get(PARAMETERS_FILE_IDENTIFIER).getFiles().get(0).getFileContent();
    }
    //    } else {
    ////      if (spec.getConfiguration().getParameters() != null
    ////          && Objects.equals(
    ////              spec.getConfiguration().getParameters().getStore().getSpec().getKind(), HARNESS_STORE_TYPE)) {
    ////        // TODO: Add harness store logic
    ////      }
    //    }
    if (filesFromMultipleRepo.get(TEMPLATE_FILE_IDENTIFIER) != null) {
      templateBody = filesFromMultipleRepo.get(TEMPLATE_FILE_IDENTIFIER).getFiles().get(0).getFileContent();
      //    } else {
      //      if (Objects.equals(
      //              spec.getConfiguration().getTemplateFile().getStore().getSpec().getKind(), HARNESS_STORE_TYPE)) {
      //        // TODO: Add harness store logic
      //      }
    }
    populatePassThroughData((AzureCreateARMResourcePassThroughData) passThroughData, templateBody, parametersBody);
    AzureConnectorDTO connectorDTO = azureCommonHelper.getAzureConnectorConfig(
        ambiance, ParameterField.createValueField(spec.getConfigurationParameters().getConnectorRef().getValue()));

    AzureTaskNGParameters azureTaskNGParameters =
        getAzureTaskNGParams(ambiance, stepElementParameters, connectorDTO, passThroughData);
    return executeCreateTask(ambiance, stepElementParameters, azureTaskNGParameters, passThroughData);
  }
  private static ARMScopeType fromYamlScopeToInternalScope(final String value) {
    switch (value) {
      case AzureScopeTypesNames.ResourceGroup:
        return RESOURCE_GROUP;
      case AzureScopeTypesNames.Subscription:
        return SUBSCRIPTION;
      case AzureScopeTypesNames.ManagementGroup:
        return MANAGEMENT_GROUP;
      case AzureScopeTypesNames.Tenant:
        return TENANT;
      default:
        return null;
    }
  }

  private GitFetchFilesConfig getTemplateGitFetchFileConfig(
      Ambiance ambiance, AzureCreateARMResourceTemplateFile azureCreateTemplateFile) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) azureCreateTemplateFile.getStore().getSpec();
    List<String> paths = new ArrayList<>(ParameterFieldHelper.getParameterFieldValue(gitStoreConfig.getPaths()));
    return GitFetchFilesConfig.builder()
        .manifestType(AZURE_TEMPLATE_TYPE)
        .identifier(TEMPLATE_FILE_IDENTIFIER)
        .gitStoreDelegateConfig(
            azureCommonHelper.getGitStoreDelegateConfig(azureCreateTemplateFile.getStore().getSpec(), ambiance, paths))
        .build();
  }
}
