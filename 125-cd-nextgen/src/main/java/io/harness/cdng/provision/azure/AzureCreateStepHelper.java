/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.azure.arm.AzureARMTaskType.ARM_DEPLOYMENT;
import static io.harness.delegate.task.azure.arm.AzureARMTaskType.BLUEPRINT_DEPLOYMENT;
import static io.harness.steps.StepUtils.prepareCDTaskRequest;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.provision.azure.beans.AzureCreatePassThroughData;
import io.harness.common.ParameterFieldHelper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.azure.arm.AzureARMTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureBlueprintTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureTaskNGParameters;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.yaml.ParameterField;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
  @Inject private K8sStepHelper k8sStepHelper;

  @Inject private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StepHelper stepHelper;

  @Inject private EngineExpressionService engineExpressionService;

  @Named("PRIVILEGED") @Inject private SecretManagerClientService secretManagerClientService;

  public static final String DEFAULT_TIMEOUT = "10m";

  private static final String TEMPLATE_FILE_IDENTIFIER = "templateFile";
  private static final String PARAMETERS_FILE_IDENTIFIER = "parameterFile";

  private static final String BLUEPRINT_IDENTIFIER = "bluePrint";

  private static final String BLUEPRINT_JSON = "blueprint.json";
  private static final String ASSIGN_JSON = "assign.json";
  private static final String ARTIFACTS = "artifacts/";

  public TaskChainResponse startChainLink(
      AzureCreateStepExecutor azureCreateStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters) {
    // Retrieve AZ connector
    AzureCreateStepParameters azureCreateStepParameters = (AzureCreateStepParameters) stepElementParameters.getSpec();
    AzureCreateStepConfigurationParameters stepConfigurationParameters = azureCreateStepParameters.getConfiguration();
    ConnectorInfoDTO connectorDTO =
        cdStepHelper.getConnector(stepConfigurationParameters.getAzureDeploymentType().getConnectorRef(), ambiance);
    if (!(connectorDTO.getConnectorConfig() instanceof AzureConnectorDTO)) {
      throw new InvalidRequestException("Invalid connector selected in Azure step. Select Azure connector");
    }

    List<GitFetchFilesConfig> gitFetchFilesConfigs =
        getParametersGitFetchFileConfigs(ambiance, stepConfigurationParameters);
    AzureCreateTemplateFileSpec azureCreateTemplateFileSpec =
        stepConfigurationParameters.getAzureDeploymentType().getTemplateSpecs().getSpec();
    if (isTemplateStoredOnGit(azureCreateTemplateFileSpec)) {
      gitFetchFilesConfigs.add(getTemplateGitFetchFileConfig(ambiance, stepConfigurationParameters));
    }

    AzureCreatePassThroughData passThroughData = getAzureCreatePassThroughData(stepConfigurationParameters);
    if (isNotEmpty(gitFetchFilesConfigs)) {
      return getGitFetchFileTaskChainResponse(ambiance, gitFetchFilesConfigs, stepElementParameters, passThroughData);
    }
    String templateBody = null;
    String parametersBody = null;
    // Inline is only valid for ARM
    if (Objects.equals(azureCreateStepParameters.getConfiguration().getAzureDeploymentType().getType(),
            AzureDeploymentTypes.ARM)) {
      AzureARMDeploymentSpec azureARMDeploymentSpec =
          (AzureARMDeploymentSpec) azureCreateStepParameters.getConfiguration().getAzureDeploymentType();
      if (Objects.equals(azureARMDeploymentSpec.getTemplateSpecs().getType(), AzureCreateTemplateFileTypes.Inline)) {
        templateBody = ((AzureInlineTemplateFileSpec) azureARMDeploymentSpec.getTemplateFile().getSpec())
                           .getTemplateBody()
                           .getValue();
      }

      if (Objects.equals(azureARMDeploymentSpec.getParameters().getType(), AzureARMParametersFileTypes.Inline)) {
        parametersBody = (((AzureInlineParametersFileSpec) azureARMDeploymentSpec.getParameters().getSpec())
                              .getParameterBody()
                              .getValue());
      }
    }
    populatePassThroughData(passThroughData, templateBody, parametersBody, null, null, null);
    AzureTaskNGParameters azureARMTaskNGParameters = getAzureTaskNGParams(
        ambiance, stepElementParameters, (AzureConnectorDTO) connectorDTO.getConnectorConfig(), passThroughData);

    return azureCreateStepExecutor.executeCreateTask(
        ambiance, stepElementParameters, azureARMTaskNGParameters, passThroughData);
  }

  private AzureTaskNGParameters getAzureTaskNGParams(Ambiance ambiance, StepElementParameters stepElementParameters,
      AzureConnectorDTO connectorConfig, AzureCreatePassThroughData passThroughData) {
    AzureCreateStepParameters azureCreateStepParameters = (AzureCreateStepParameters) stepElementParameters.getSpec();
    AzureCreateStepConfigurationParameters stepConfigurationParameters = azureCreateStepParameters.getConfiguration();
    if (Objects.equals(stepConfigurationParameters.getAzureDeploymentType().getType(), AzureDeploymentTypes.ARM)) {
      AzureARMDeploymentSpec specs = (AzureARMDeploymentSpec) stepConfigurationParameters.getAzureDeploymentType();
      AzureARMTaskNGParameters.AzureARMTaskNGParametersBuilder builder = AzureARMTaskNGParameters.builder();
      builder.accountId(AmbianceUtils.getAccountId(ambiance))
          .taskType(ARM_DEPLOYMENT)
          .templateBody(passThroughData.getTemplateBody())
          .connectorDTO(connectorConfig)
          .parametersBody(passThroughData.getParametersBody())
          .timeoutInMs(StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), DEFAULT_TIMEOUT));
      if (specs.getScope().getSpec() instanceof AzureResourceGroupSpec) {
        AzureResourceGroupSpec resourceGroupSpec = (AzureResourceGroupSpec) specs.getScope().getSpec();
        builder.scopeType(ARMScopeType.fromString(specs.getScope().getType()))
            .deploymentMode(retrieveDeploymentMode(
                ARMScopeType.fromString(specs.getScope().getType()), resourceGroupSpec.getMode().getValue()))
            .subscriptionId(resourceGroupSpec.getSubscription().getValue())
            .resourceGroupName(resourceGroupSpec.getResourceGroup().getValue());
      } else if (specs.getScope().getSpec() instanceof AzureSubscritionSpec) {
        AzureSubscritionSpec subscriptionSpec = (AzureSubscritionSpec) specs.getScope().getSpec();
        builder.scopeType(ARMScopeType.fromString(specs.getScope().getType()))
            .deploymentDataLocation(subscriptionSpec.getDeploymentDataLocation().getValue())
            .deploymentMode(retrieveDeploymentMode(
                ARMScopeType.fromString(specs.getScope().getType()), subscriptionSpec.getMode().getValue()))
            .subscriptionId(subscriptionSpec.getSubscription().getValue());
      } else if (specs.getScope().getSpec() instanceof AzureManagementSpec) {
        AzureManagementSpec managementSpec = (AzureManagementSpec) specs.getScope().getSpec();
        builder.scopeType(ARMScopeType.fromString(specs.getScope().getType()))
            .deploymentMode(retrieveDeploymentMode(ARMScopeType.fromString(specs.getScope().getType()), null))
            .deploymentDataLocation(managementSpec.getDeploymentDataLocation().getValue())
            .managementGroupId(managementSpec.getManagementGroupId().getValue());
      } else if (specs.getScope().getSpec() instanceof AzureTenantSpec) {
        AzureTenantSpec tenantSpec = (AzureTenantSpec) specs.getScope().getSpec();
        builder.scopeType(ARMScopeType.fromString(specs.getScope().getType()))
            .deploymentMode(retrieveDeploymentMode(ARMScopeType.fromString(specs.getScope().getType()), null))
            .deploymentDataLocation(tenantSpec.getDeploymentDataLocation().getValue());
      } else {
        throw new InvalidRequestException(
            "Invalid scope type in Azure step. Select one of the following: ResourceGroup, Subscription, Management, Tenant");
      }
      return builder.encryptedDataDetails(getAzureEncryptionDetails(ambiance, connectorConfig)).build();
    } else {
      AzureBluePrintDeploymentSpec specs =
          (AzureBluePrintDeploymentSpec) stepConfigurationParameters.getAzureDeploymentType();
      return AzureBlueprintTaskNGParameters.builder()
          .accountId(AmbianceUtils.getAccountId(ambiance))
          .taskType(BLUEPRINT_DEPLOYMENT)
          .connectorDTO(connectorConfig)
          .timeoutInMs(StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), DEFAULT_TIMEOUT))
          .blueprintJson(passThroughData.getBlueprintBody())
          .assignmentJson(passThroughData.getAssignBody())
          .artifacts(passThroughData.getArtifacts())
          .assignmentName(specs.getAssignmentName().getValue())
          .encryptedDataDetailList(getAzureEncryptionDetails(ambiance, connectorConfig))
          .build();
    }
  }

  private AzureDeploymentMode retrieveDeploymentMode(ARMScopeType scopeType, String mode) {
    if (ARMScopeType.RESOURCE_GROUP == scopeType) {
      return mode != null ? AzureDeploymentMode.valueOf(mode.toUpperCase()) : AzureDeploymentMode.INCREMENTAL;
    }
    return AzureDeploymentMode.INCREMENTAL;
  }

  private void populatePassThroughData(AzureCreatePassThroughData passThroughData, String templateBody,
      String parametersBody, String blueprintBody, String assignmentBody, Map<String, String> artifacts) {
    passThroughData.setTemplateBody(templateBody);
    passThroughData.setParametersBody(parametersBody);
    passThroughData.setArtifacts(artifacts);
    passThroughData.setAssignBody(assignmentBody);
    passThroughData.setBlueprintBody(blueprintBody);
  }

  private TaskChainResponse getGitFetchFileTaskChainResponse(Ambiance ambiance,
      List<GitFetchFilesConfig> gitFetchFilesConfigs, StepElementParameters stepElementParameters,
      AzureCreatePassThroughData passThroughData) {
    GitFetchRequest gitFetchRequest = GitFetchRequest.builder()
                                          .gitFetchFilesConfigs(gitFetchFilesConfigs)
                                          .accountId(AmbianceUtils.getAccountId(ambiance))
                                          .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.GIT_FETCH_NEXT_GEN_TASK.name())
                                  .parameters(new Object[] {gitFetchRequest})
                                  .build();

    final TaskRequest taskRequest = prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        Arrays.asList(K8sCommandUnitConstants.FetchFiles, AzureCommandUnit.Create.name()),
        TaskType.GIT_FETCH_NEXT_GEN_TASK.getDisplayName(),
        StepUtils.getTaskSelectors(stepElementParameters.getDelegateSelectors()),
        stepHelper.getEnvironmentType(ambiance));

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(passThroughData)
        .build();
  }

  // This method retrieves only the parameter file for ARM and if the parameter type is Remote
  @NotNull
  private List<GitFetchFilesConfig> getParametersGitFetchFileConfigs(
      Ambiance ambiance, AzureCreateStepConfigurationParameters stepConfiguration) {
    if (Objects.equals(stepConfiguration.getAzureDeploymentType().getType(), AzureDeploymentTypes.ARM)) {
      AzureARMDeploymentSpec specs = (AzureARMDeploymentSpec) stepConfiguration.getAzureDeploymentType();
      if (specs.getParameters() != null
          && Objects.equals(specs.getParameters().getType(), AzureARMParametersFileTypes.Remote)) {
        AzureRemoteParametersFileSpec fileSpec = (AzureRemoteParametersFileSpec) specs.getParameters().getSpec();
        return new ArrayList<>(
            Collections.singletonList(GitFetchFilesConfig.builder()
                                          .manifestType("Azure Parameters")
                                          .identifier(PARAMETERS_FILE_IDENTIFIER)
                                          .gitStoreDelegateConfig(getGitStoreDelegateConfig(
                                              fileSpec.getStore().getSpec(), ambiance, AzureDeploymentTypes.ARM))
                                          .build()));
      }
    }
    return new ArrayList<>();
  }

  private GitFetchFilesConfig getTemplateGitFetchFileConfig(
      Ambiance ambiance, AzureCreateStepConfigurationParameters stepConfiguration) {
    AzureRemoteTemplateFileSpec azureCreateTemplateFileSpec =
        (AzureRemoteTemplateFileSpec) stepConfiguration.getAzureDeploymentType().getTemplateSpecs().getSpec();
    if (Objects.equals(stepConfiguration.getAzureDeploymentType().getType(), AzureDeploymentTypes.ARM)) {
      return GitFetchFilesConfig.builder()
          .manifestType("Azure Template")
          .identifier(TEMPLATE_FILE_IDENTIFIER)
          .gitStoreDelegateConfig(getGitStoreDelegateConfig(
              azureCreateTemplateFileSpec.getStore().getSpec(), ambiance, AzureDeploymentTypes.ARM))
          .build();
    } else {
      return GitFetchFilesConfig.builder()
          .manifestType("Azure BluePrint Folder")
          .identifier(BLUEPRINT_IDENTIFIER)
          .gitStoreDelegateConfig(getGitStoreDelegateConfig(
              azureCreateTemplateFileSpec.getStore().getSpec(), ambiance, AzureDeploymentTypes.BLUEPRINT))
          .build();
    }
  }

  private GitStoreDelegateConfig getGitStoreDelegateConfig(StoreConfig store, Ambiance ambiance, String type) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) store;
    cdStepHelper.validateGitStoreConfig(gitStoreConfig);
    String connectorId = getParameterFieldValue(gitStoreConfig.getConnectorRef());
    ConnectorInfoDTO connectorDTO = k8sStepHelper.getConnector(connectorId, ambiance);

    GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO((ScmConnector) connectorDTO.getConnectorConfig());
    NGAccess basicNGAccessObject = AmbianceUtils.getNgAccess(ambiance);
    SSHKeySpecDTO sshKeySpecDTO =
        gitConfigAuthenticationInfoHelper.getSSHKey(gitConfigDTO, AmbianceUtils.getAccountId(ambiance),
            AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));
    List<EncryptedDataDetail> encryptedDataDetails =
        gitConfigAuthenticationInfoHelper.getEncryptedDataDetails(gitConfigDTO, sshKeySpecDTO, basicNGAccessObject);
    String repoName = gitStoreConfig.getRepoName() != null ? gitStoreConfig.getRepoName().getValue() : null;
    if (gitConfigDTO.getGitConnectionType() == GitConnectionType.ACCOUNT) {
      String repoUrl = cdStepHelper.getGitRepoUrl(gitConfigDTO, repoName);
      gitConfigDTO.setUrl(repoUrl);
      gitConfigDTO.setGitConnectionType(GitConnectionType.REPO);
    }
    GitStoreDelegateConfig.GitStoreDelegateConfigBuilder builder = GitStoreDelegateConfig.builder();
    builder.gitConfigDTO(gitConfigDTO)
        .sshKeySpecDTO(sshKeySpecDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .fetchType(gitStoreConfig.getGitFetchType())
        .branch(getParameterFieldValue(gitStoreConfig.getBranch()))
        .commitId(getParameterFieldValue(gitStoreConfig.getCommitId()))
        .connectorName(connectorDTO.getName())
        .build();
    List<String> paths = new ArrayList<>();

    if (Objects.equals(type, AzureDeploymentTypes.BLUEPRINT)) {
      paths.add(ParameterFieldHelper.getParameterFieldValue(gitStoreConfig.getFolderPath()));
    } else {
      paths.addAll(ParameterFieldHelper.getParameterFieldValue(gitStoreConfig.getPaths()));
    }
    builder.paths(paths);

    return builder.build();
  }
  private boolean isTemplateStoredOnGit(AzureCreateTemplateFileSpec azureCreateTemplateFileSpec) {
    return azureCreateTemplateFileSpec.getType().equals(AzureCreateTemplateFileTypes.Remote)
        && ManifestStoreType.isInGitSubset(
            ((AzureRemoteTemplateFileSpec) azureCreateTemplateFileSpec).getStore().getSpec().getKind());
  }

  private AzureCreatePassThroughData getAzureCreatePassThroughData(
      AzureCreateStepConfigurationParameters stepConfiguration) {
    boolean hasGitFiles = hasGitStoredParameters(stepConfiguration)
        || isTemplateStoredOnGit(stepConfiguration.getAzureDeploymentType().getTemplateSpecs().getSpec());

    return AzureCreatePassThroughData.builder().hasGitFiles(hasGitFiles).build();
  }

  private boolean hasGitStoredParameters(AzureCreateStepConfigurationParameters stepConfigurationParameters) {
    if (Objects.equals(stepConfigurationParameters.getAzureDeploymentType().getType(), AzureDeploymentTypes.ARM)) {
      AzureARMDeploymentSpec spec = (AzureARMDeploymentSpec) stepConfigurationParameters.getAzureDeploymentType();
      return spec.getParameters() != null
          && Objects.equals(spec.getParameters().getType(), AzureARMParametersFileTypes.Remote)
          && ManifestStoreType.isInGitSubset(
              ((AzureRemoteParametersFileSpec) spec.getParameters().getSpec()).getStore().getSpec().getKind());
    }
    return false;
  }

  TaskChainResponse handleGitFetchResponse(AzureCreateStepExecutor azureCreateStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, AzureCreateStepConfigurationParameters stepConfigurationParameters,
      AzureCreatePassThroughData passThroughData, GitFetchResponse responseData) {
    Map<String, FetchFilesResult> filesFromMultipleRepo = responseData.getFilesFromMultipleRepo();

    String templateBody = null;
    String parametersBody = null;
    String assignBody = null;
    String blueprintBody = null;
    Map<String, String> artifacts = new HashMap<>();
    // If the step is ARM, retrieve the templateBody and parametersBody from git or the inline fields
    if (Objects.equals(stepConfigurationParameters.getAzureDeploymentType().getType(), AzureDeploymentTypes.ARM)) {
      if (filesFromMultipleRepo.get(PARAMETERS_FILE_IDENTIFIER) != null) {
        parametersBody = filesFromMultipleRepo.get(PARAMETERS_FILE_IDENTIFIER).getFiles().get(0).getFileContent();
      } else {
        AzureARMDeploymentSpec spec = (AzureARMDeploymentSpec) stepConfigurationParameters.getAzureDeploymentType();
        if (spec.getParameters() != null
            && Objects.equals(spec.getParameters().getType(), AzureARMParametersFileTypes.Inline)) {
          AzureInlineParametersFileSpec fileSpec = (AzureInlineParametersFileSpec) spec.getParameters().getSpec();
          parametersBody = fileSpec.getParameterBody().getValue();
        }
      }
      if (filesFromMultipleRepo.get(TEMPLATE_FILE_IDENTIFIER) != null) {
        templateBody = filesFromMultipleRepo.get(TEMPLATE_FILE_IDENTIFIER).getFiles().get(0).getFileContent();
      } else {
        AzureARMDeploymentSpec spec = (AzureARMDeploymentSpec) stepConfigurationParameters.getAzureDeploymentType();
        if (Objects.equals(spec.getTemplateFile().getType(), AzureCreateTemplateFileTypes.Inline)) {
          AzureInlineTemplateFileSpec fileSpec = (AzureInlineTemplateFileSpec) spec.getTemplateFile().getSpec();
          parametersBody = fileSpec.getTemplateBody().getValue();
        }
      }
    } else {
      if (filesFromMultipleRepo.get(BLUEPRINT_IDENTIFIER) != null) {
        List<GitFile> gitFiles = filesFromMultipleRepo.get(BLUEPRINT_IDENTIFIER).getFiles();
        for (GitFile gitFile : gitFiles) {
          if (gitFile.getFilePath().contains(BLUEPRINT_JSON)) {
            blueprintBody = gitFile.getFileContent();
          } else if (gitFile.getFilePath().contains(ASSIGN_JSON)) {
            assignBody = gitFile.getFileContent();
          } else if (gitFile.getFilePath().contains(ARTIFACTS)) {
            artifacts.put(
                gitFile.getFilePath().substring(gitFile.getFilePath().lastIndexOf(ARTIFACTS) + ARTIFACTS.length()),
                gitFile.getFileContent());
          }
        }
      }
    }
    populatePassThroughData(passThroughData, templateBody, parametersBody, blueprintBody, assignBody, artifacts);
    AzureConnectorDTO connectorDTO = getAzureConnectorConfig(ambiance,
        ParameterField.createValueField(stepConfigurationParameters.getAzureDeploymentType().getConnectorRef()));
    AzureTaskNGParameters azureTaskNGParameters =
        getAzureTaskNGParams(ambiance, stepElementParameters, connectorDTO, passThroughData);
    return azureCreateStepExecutor.executeCreateTask(
        ambiance, stepElementParameters, azureTaskNGParameters, passThroughData);
  }
  private AzureConnectorDTO getAzureConnectorConfig(Ambiance ambiance, ParameterField<String> connectorRef) {
    return (AzureConnectorDTO) cdStepHelper.getConnector(getParameterFieldValue(connectorRef), ambiance)
        .getConnectorConfig();
  }

  public TaskChainResponse executeNextLink(AzureCreateStepExecutor azureCreateStepExecutor, Ambiance ambiance,
      StepElementParameters stepParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseSupplier) throws Exception {
    UnitProgressData unitProgressData = null;
    try {
      ResponseData responseData = responseSupplier.get();
      if (responseData instanceof GitFetchResponse) {
        unitProgressData = ((GitFetchResponse) responseData).getUnitProgressData();
        AzureCreateStepParameters azureCreateStepParameters = (AzureCreateStepParameters) stepParameters.getSpec();
        AzureCreateStepConfigurationParameters stepConfigurationParameters =
            azureCreateStepParameters.getConfiguration();
        return handleGitFetchResponse(azureCreateStepExecutor, ambiance, stepParameters, stepConfigurationParameters,
            (AzureCreatePassThroughData) passThroughData, (GitFetchResponse) responseData);
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
      return getExceptionTaskChainResponse(ambiance, e.getCommandUnitsProgress(), e);
    } catch (Exception e) {
      log.error(format("Exception in create stack step: %s", e.getMessage()));
      return getExceptionTaskChainResponse(ambiance, unitProgressData, e);
    }
  }
  private TaskChainResponse getExceptionTaskChainResponse(
      Ambiance ambiance, UnitProgressData unitProgressData, Exception e) {
    return TaskChainResponse.builder()
        .chainEnd(true)
        .passThroughData(
            StepExceptionPassThroughData.builder()
                .unitProgressData(
                    cdStepHelper.completeUnitProgressData(unitProgressData, ambiance, ExceptionUtils.getMessage(e)))
                .errorMessage(e.getCause() != null ? String.format("%s: %s", e.getMessage(), e.getCause().getMessage())
                                                   : e.getMessage())
                .build())
        .build();
  }

  public List<EncryptedDataDetail> getAzureEncryptionDetails(Ambiance ambiance, AzureConnectorDTO azureConnectorDTO) {
    if (isNotEmpty(azureConnectorDTO.getDecryptableEntities())) {
      return secretManagerClientService.getEncryptionDetails(
          AmbianceUtils.getNgAccess(ambiance), azureConnectorDTO.getDecryptableEntities().get(0));
    } else {
      return emptyList();
    }
  }
}
