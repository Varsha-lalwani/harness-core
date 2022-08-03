/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.manifest.yaml.harness.HarnessStoreConstants.HARNESS_STORE_TYPE;
import static io.harness.cdng.provision.azure.AzureCommonHelper.DEFAULT_TIMEOUT;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.azure.arm.AzureARMTaskNGParameters.AzureARMTaskNGParametersBuilder;
import static io.harness.delegate.task.azure.arm.AzureARMTaskType.ARM_DEPLOYMENT;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.provision.azure.beans.AzureCreatePassThroughData;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.azure.arm.AzureARMTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureTaskNGParameters;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.FetchFilesResult;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
@Singleton
public class AzureCreateStepHelper {
  @Inject private CDStepHelper cdStepHelper;
  public static final String TEMPLATE_FILE_IDENTIFIER = "templateFile";
  public static final String PARAMETERS_FILE_IDENTIFIER = "parameterFile";
  @Inject private AzureCommonHelper azureCommonHelper;

  public TaskChainResponse startChainLink(
      AzureCreateStepExecutor azureCreateStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters) {
    AzureCreateStepConfigurationParameters stepConfigurationParameters =
        ((AzureCreateStepParameters) stepElementParameters.getSpec()).getConfiguration();
    ConnectorInfoDTO connectorDTO =
        cdStepHelper.getConnector(stepConfigurationParameters.getConnectorRef().getValue(), ambiance);
    if (!(connectorDTO.getConnectorConfig() instanceof AzureConnectorDTO)) {
      throw new InvalidRequestException("Invalid connector selected in Azure step. Select Azure connector");
    }

    List<GitFetchFilesConfig> gitFetchFilesConfigs =
        getParametersGitFetchFileConfigs(ambiance, stepConfigurationParameters);
    AzureCreateTemplateFile azureCreateTemplateFile = stepConfigurationParameters.getTemplateFile();
    if (isTemplateStoredOnGit(azureCreateTemplateFile)) {
      gitFetchFilesConfigs.add(azureCommonHelper.getTemplateGitFetchFileConfig(
          ambiance, stepConfigurationParameters.getTemplateFile(), AzureDeploymentTypes.ARM));
    }

    AzureCreatePassThroughData passThroughData = getAzureCreatePassThroughData(stepConfigurationParameters);
    if (isNotEmpty(gitFetchFilesConfigs)) {
      return azureCommonHelper.getGitFetchFileTaskChainResponse(
          ambiance, gitFetchFilesConfigs, stepElementParameters, passThroughData);
    }
    String templateBody = null;
    String parametersBody = null;

    if (Objects.equals(azureCreateTemplateFile.getStore().getSpec().getKind(), HARNESS_STORE_TYPE)) {
      // TODO: Add logic for harness store type
    }

    if (Objects.equals(
            stepConfigurationParameters.getParameters().getStore().getSpec().getKind(), HARNESS_STORE_TYPE)) {
      // TODO: Add logic for harness store type
    }
    populatePassThroughData(passThroughData, templateBody, parametersBody);
    AzureTaskNGParameters azureARMTaskNGParameters = getAzureTaskNGParams(
        ambiance, stepElementParameters, (AzureConnectorDTO) connectorDTO.getConnectorConfig(), passThroughData);

    return azureCreateStepExecutor.executeCreateTask(
        ambiance, stepElementParameters, azureARMTaskNGParameters, passThroughData);
  }

  private boolean isTemplateStoredOnGit(AzureCreateTemplateFile azureCreateTemplateFileSpec) {
    return ManifestStoreType.isInGitSubset((azureCreateTemplateFileSpec).getStore().getSpec().getKind());
  }

  private AzureCreatePassThroughData getAzureCreatePassThroughData(
      AzureCreateStepConfigurationParameters stepConfiguration) {
    boolean hasGitFiles = azureCommonHelper.hasGitStoredParameters(stepConfiguration)
        || isTemplateStoredOnGit(stepConfiguration.getTemplateFile());

    return AzureCreatePassThroughData.builder().hasGitFiles(hasGitFiles).build();
  }

  private AzureTaskNGParameters getAzureTaskNGParams(Ambiance ambiance, StepElementParameters stepElementParameters,
      AzureConnectorDTO connectorConfig, PassThroughData passThroughData) {
    AzureCreateStepParameters azureCreateStepParameters = (AzureCreateStepParameters) stepElementParameters.getSpec();
    AzureCreatePassThroughData azureCreatePassThroughData = (AzureCreatePassThroughData) passThroughData;
    AzureCreateStepConfigurationParameters stepConfigurationParameters = azureCreateStepParameters.getConfiguration();
    AzureARMTaskNGParametersBuilder builder = AzureARMTaskNGParameters.builder();
    builder.accountId(AmbianceUtils.getAccountId(ambiance))
        .taskType(ARM_DEPLOYMENT)
        .templateBody(azureCreatePassThroughData.getTemplateBody())
        .connectorDTO(connectorConfig)
        .parametersBody(azureCreatePassThroughData.getParametersBody())
        .timeoutInMs(StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), DEFAULT_TIMEOUT));
    if (stepConfigurationParameters.getScope().getSpec() instanceof AzureResourceGroupSpec) {
      AzureResourceGroupSpec resourceGroupSpec =
          (AzureResourceGroupSpec) stepConfigurationParameters.getScope().getSpec();
      builder.scopeType(ARMScopeType.fromString(stepConfigurationParameters.getScope().getType()))
          .deploymentMode(
              retrieveDeploymentMode(ARMScopeType.fromString(stepConfigurationParameters.getScope().getType()),
                  resourceGroupSpec.getMode().getValue()))
          .subscriptionId(resourceGroupSpec.getSubscription().getValue())
          .resourceGroupName(resourceGroupSpec.getResourceGroup().getValue());
    } else if (stepConfigurationParameters.getScope().getSpec() instanceof AzureSubscriptionSpec) {
      AzureSubscriptionSpec subscriptionSpec = (AzureSubscriptionSpec) stepConfigurationParameters.getScope().getSpec();
      builder.scopeType(ARMScopeType.fromString(stepConfigurationParameters.getScope().getType()))
          .deploymentDataLocation(subscriptionSpec.getLocation().getValue())
          .deploymentMode(
              retrieveDeploymentMode(ARMScopeType.fromString(stepConfigurationParameters.getScope().getType()), null))
          .subscriptionId(subscriptionSpec.getSubscription().getValue());
    } else if (stepConfigurationParameters.getScope().getSpec() instanceof AzureManagementSpec) {
      AzureManagementSpec managementSpec = (AzureManagementSpec) stepConfigurationParameters.getScope().getSpec();
      builder.scopeType(ARMScopeType.fromString(stepConfigurationParameters.getScope().getType()))
          .deploymentMode(
              retrieveDeploymentMode(ARMScopeType.fromString(stepConfigurationParameters.getScope().getType()), null))
          .deploymentDataLocation(managementSpec.getLocation().getValue())
          .managementGroupId(managementSpec.getManagementGroupId().getValue());
    } else if (stepConfigurationParameters.getScope().getSpec() instanceof AzureTenantSpec) {
      AzureTenantSpec tenantSpec = (AzureTenantSpec) stepConfigurationParameters.getScope().getSpec();
      builder.scopeType(ARMScopeType.fromString(stepConfigurationParameters.getScope().getType()))
          .deploymentMode(
              retrieveDeploymentMode(ARMScopeType.fromString(stepConfigurationParameters.getScope().getType()), null))
          .deploymentDataLocation(tenantSpec.getLocation().getValue());
    } else {
      throw new InvalidRequestException(
          "Invalid scope type in Azure step. Select one of the following: ResourceGroup, Subscription, Management, Tenant");
    }
    return builder.encryptedDataDetails(azureCommonHelper.getAzureEncryptionDetails(ambiance, connectorConfig)).build();
  }

  private AzureDeploymentMode retrieveDeploymentMode(ARMScopeType scopeType, String mode) {
    if (ARMScopeType.RESOURCE_GROUP == scopeType) {
      return mode != null ? AzureDeploymentMode.valueOf(mode.toUpperCase()) : AzureDeploymentMode.INCREMENTAL;
    }
    return AzureDeploymentMode.INCREMENTAL;
  }

  private void populatePassThroughData(
      AzureCreatePassThroughData passThroughData, String templateBody, String parametersBody) {
    passThroughData.setTemplateBody(templateBody);
    passThroughData.setParametersBody(parametersBody);
  }

  // This method retrieves only the parameter file for ARM and if the parameter type is Remote
  @NotNull
  private List<GitFetchFilesConfig> getParametersGitFetchFileConfigs(
      Ambiance ambiance, AzureCreateStepConfigurationParameters stepConfiguration) {
    if (stepConfiguration.getParameters() != null
        && ManifestStoreType.isInGitSubset(stepConfiguration.getParameters().getStore().getSpec().getKind())) {
      return new ArrayList<>(Collections.singletonList(
          GitFetchFilesConfig.builder()
              .manifestType("Azure Parameters")
              .identifier(PARAMETERS_FILE_IDENTIFIER)
              .gitStoreDelegateConfig(azureCommonHelper.getGitStoreDelegateConfig(
                  stepConfiguration.getParameters().getStore().getSpec(), ambiance, AzureDeploymentTypes.ARM))
              .build()));
    }

    return new ArrayList<>();
  }

  TaskChainResponse handleGitFetchResponse(AzureCreateStepExecutor azureCreateStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, PassThroughData passThroughData, GitFetchResponse responseData) {
    Map<String, FetchFilesResult> filesFromMultipleRepo = responseData.getFilesFromMultipleRepo();
    AzureCreateStepParameters spec = (AzureCreateStepParameters) stepElementParameters.getSpec();
    String templateBody = null;
    String parametersBody = null;
    // If the step is ARM, retrieve the templateBody and parametersBody from git or the inline fields
    if (filesFromMultipleRepo.get(PARAMETERS_FILE_IDENTIFIER) != null) {
      parametersBody = filesFromMultipleRepo.get(PARAMETERS_FILE_IDENTIFIER).getFiles().get(0).getFileContent();
    } else {
      if (spec.getConfiguration().getParameters() != null
          && Objects.equals(
              spec.getConfiguration().getParameters().getStore().getSpec().getKind(), HARNESS_STORE_TYPE)) {
        // TODO: Add harness store logic
      }
    }
    if (filesFromMultipleRepo.get(TEMPLATE_FILE_IDENTIFIER) != null) {
      templateBody = filesFromMultipleRepo.get(TEMPLATE_FILE_IDENTIFIER).getFiles().get(0).getFileContent();
    } else {
      if (Objects.equals(
              spec.getConfiguration().getTemplateFile().getStore().getSpec().getKind(), HARNESS_STORE_TYPE)) {
        // TODO: Add harness store logic
      }
    }
    populatePassThroughData((AzureCreatePassThroughData) passThroughData, templateBody, parametersBody);
    AzureConnectorDTO connectorDTO = azureCommonHelper.getAzureConnectorConfig(
        ambiance, ParameterField.createValueField(spec.getConfiguration().getConnectorRef().getValue()));

    AzureTaskNGParameters azureTaskNGParameters =
        getAzureTaskNGParams(ambiance, stepElementParameters, connectorDTO, passThroughData);
    return azureCreateStepExecutor.executeCreateTask(
        ambiance, stepElementParameters, azureTaskNGParameters, passThroughData);
  }

  public TaskChainResponse executeNextLink(AzureCreateStepExecutor azureCreateStepExecutor, Ambiance ambiance,
      StepElementParameters stepParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseSupplier) throws Exception {
    UnitProgressData unitProgressData = null;
    try {
      ResponseData responseData = responseSupplier.get();
      if (responseData instanceof GitFetchResponse) {
        unitProgressData = ((GitFetchResponse) responseData).getUnitProgressData();
        return handleGitFetchResponse(
            azureCreateStepExecutor, ambiance, stepParameters, passThroughData, (GitFetchResponse) responseData);
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
    } catch (TaskNGDataException e) {
      log.error(format("Exception in create stack step: %s", e.getMessage()));
      return azureCommonHelper.getExceptionTaskChainResponse(ambiance, e.getCommandUnitsProgress(), e);
    } catch (Exception e) {
      log.error(format("Exception in create stack step: %s", e.getMessage()));
      return azureCommonHelper.getExceptionTaskChainResponse(ambiance, unitProgressData, e);
    }
  }
}
