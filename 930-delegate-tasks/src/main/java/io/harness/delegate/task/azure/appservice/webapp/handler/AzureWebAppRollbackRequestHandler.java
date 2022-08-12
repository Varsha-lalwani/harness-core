/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice.webapp.handler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.azure.model.AzureConstants.AZURE_APP_SVC_ARTIFACT_DOWNLOAD_DIR_PATH;
import static io.harness.azure.model.AzureConstants.DEPLOYMENT_SLOT_PRODUCTION_NAME;
import static io.harness.azure.model.AzureConstants.DEPLOY_TO_SLOT;
import static io.harness.azure.model.AzureConstants.NO_TRAFFIC_SHIFT_REQUIRED;
import static io.harness.azure.model.AzureConstants.REPOSITORY_DIR_PATH;
import static io.harness.azure.model.AzureConstants.SLOT_TRAFFIC_PERCENTAGE;
import static io.harness.azure.model.AzureConstants.START_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.STOP_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS;
import static io.harness.azure.model.AzureConstants.UPDATE_SLOT_CONFIGURATION_SETTINGS;
import static io.harness.delegate.task.azure.appservice.webapp.AppServiceDeploymentProgress.SAVE_CONFIGURATION;
import static io.harness.delegate.task.azure.appservice.webapp.AppServiceDeploymentProgress.STOP_SLOT;
import static io.harness.delegate.task.azure.appservice.webapp.AppServiceDeploymentProgress.UPDATE_SLOT_CONFIGURATIONS_SETTINGS;
import static io.harness.delegate.task.azure.appservice.webapp.AppServiceDeploymentProgress.UPDATE_SLOT_CONTAINER_SETTINGS;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureConstants;
import io.harness.delegate.beans.azure.mapper.AzureAppServiceConfigurationDTOMapper;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
import io.harness.delegate.task.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;
import io.harness.delegate.task.azure.appservice.deployment.context.AzureAppServicePackageDeploymentContext;
import io.harness.delegate.task.azure.appservice.webapp.AppServiceDeploymentProgress;
import io.harness.delegate.task.azure.appservice.webapp.ng.exception.AzureWebAppRollbackExceptionData;
import io.harness.delegate.task.azure.appservice.webapp.ng.request.AzureWebAppRollbackRequest;
import io.harness.delegate.task.azure.appservice.webapp.ng.response.AzureWebAppNGRollbackResponse;
import io.harness.delegate.task.azure.appservice.webapp.ng.response.AzureWebAppRequestResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.delegate.task.azure.artifact.ArtifactDownloadContext;
import io.harness.delegate.task.azure.artifact.AzureArtifactDownloadResponse;
import io.harness.delegate.task.azure.artifact.AzureArtifactDownloadService;
import io.harness.delegate.task.azure.artifact.AzurePackageArtifactConfig;
import io.harness.delegate.task.azure.common.AutoCloseableWorkingDirectory;
import io.harness.delegate.task.azure.common.AzureLogCallbackProvider;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.utils.ArtifactType;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Slf4j
public class AzureWebAppRollbackRequestHandler extends AzureWebAppRequestHandler<AzureWebAppRollbackRequest> {
  @Inject private AzureArtifactDownloadService artifactDownloaderService;

  @Override
  protected AzureWebAppRequestResponse execute(AzureWebAppRollbackRequest taskRequest, AzureConfig azureConfig,
      AzureLogCallbackProvider logCallbackProvider, String taskId) {
    azureSecretHelper.decryptAzureWebAppRollbackParameters(taskRequest.getPreDeploymentData());
    switch (taskRequest.getAzureArtifactType()) {
      case CONTAINER:
        return executeContainer(taskRequest, azureConfig, logCallbackProvider, taskId);
      case PACKAGE:
        return executePackage(taskRequest, azureConfig, logCallbackProvider, taskId);
      default:
        throw new UnsupportedOperationException(
            format("Artifact type [%s] is not supported yet", taskRequest.getAzureArtifactType()));
    }
  }

  @Override
  protected Class<AzureWebAppRollbackRequest> getRequestType() {
    return AzureWebAppRollbackRequest.class;
  }

  private AzureWebAppRequestResponse executeContainer(AzureWebAppRollbackRequest taskRequest, AzureConfig azureConfig,
      AzureLogCallbackProvider logCallbackProvider, String taskId) {
    log.info("Rollback using container artifact");
    azureSecretHelper.decryptAzureWebAppRollbackParameters(taskRequest.getPreDeploymentData());
    AzureWebClientContext azureWebClientContext =
        buildAzureWebClientContext(taskRequest.getInfrastructure(), azureConfig);
    AzureAppServiceDockerDeploymentContext dockerDeploymentContext =
        toAzureAppServiceDockerDeploymentContext(taskRequest, azureWebClientContext, logCallbackProvider);

    try {
      performRollback(
          logCallbackProvider, taskRequest, azureWebClientContext, dockerDeploymentContext, azureConfig, taskId);
      List<AzureAppDeploymentData> azureAppDeploymentData =
          getAppServiceDeploymentData(taskRequest, azureWebClientContext);

      markDeploymentStatusAsSuccess(taskRequest, logCallbackProvider, taskId);

      return AzureWebAppNGRollbackResponse.builder()
          .azureAppDeploymentData(azureAppDeploymentData)
          .preDeploymentData(taskRequest.getPreDeploymentData())
          .deploymentProgressMarker(taskRequest.getPreDeploymentData().getDeploymentProgressMarker())
          .build();
    } catch (Exception e) {
      throw new AzureWebAppRollbackExceptionData(taskRequest.getPreDeploymentData().getDeploymentProgressMarker(), e);
    }
  }

  private AzureWebAppNGRollbackResponse executePackage(AzureWebAppRollbackRequest taskRequest, AzureConfig azureConfig,
      AzureLogCallbackProvider logCallbackProvider, String taskId) {
    log.info("Rollback using package artifact");
    AzureWebClientContext azureWebClientContext =
        buildAzureWebClientContext(taskRequest.getInfrastructure(), azureConfig);
    AzureAppServicePackageDeploymentContext deploymentContext =
        toAzureAppServicePackageDeploymentContext(taskRequest, azureWebClientContext, logCallbackProvider, taskId);

    performRollback(logCallbackProvider, taskRequest, azureWebClientContext, deploymentContext, azureConfig, taskId);

    List<AzureAppDeploymentData> azureAppDeploymentData =
        getAppServiceDeploymentData(taskRequest, azureWebClientContext);

    markDeploymentStatusAsSuccess(taskRequest, logCallbackProvider, taskId);
    return AzureWebAppNGRollbackResponse.builder()
        .azureAppDeploymentData(azureAppDeploymentData)
        .preDeploymentData(taskRequest.getPreDeploymentData())
        .build();
  }

  private AzureAppServicePackageDeploymentContext toAzureAppServicePackageDeploymentContext(
      AzureWebAppRollbackRequest taskRequest, AzureWebClientContext azureWebClientContext,
      AzureLogCallbackProvider logCallbackProvider, String taskId) {
    AutoCloseableWorkingDirectory autoCloseableWorkingDirectory =
        new AutoCloseableWorkingDirectory(REPOSITORY_DIR_PATH, AZURE_APP_SVC_ARTIFACT_DOWNLOAD_DIR_PATH);
    AzurePackageArtifactConfig artifactConfig = (AzurePackageArtifactConfig) taskRequest.getArtifact();
    AzureArtifactDownloadResponse artifactResponse = null;
    if (artifactConfig != null) {
      ArtifactDownloadContext downloadContext = azureAppServiceResourceUtilities.toArtifactNgDownloadContext(
          artifactConfig, autoCloseableWorkingDirectory, logCallbackProvider);
      artifactResponse = artifactDownloaderService.download(downloadContext, taskId);
    }

    AzureAppServicePreDeploymentData preDeploymentData = taskRequest.getPreDeploymentData();
    return AzureAppServicePackageDeploymentContext.builder()
        .logCallbackProvider(logCallbackProvider)
        .appSettingsToAdd(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettings(
            preDeploymentData.getAppSettingsToAdd()))
        .appSettingsToRemove(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettings(
            preDeploymentData.getAppSettingsToRemove()))
        .connSettingsToAdd(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStrings(
            preDeploymentData.getConnStringsToAdd()))
        .connSettingsToRemove(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStrings(
            preDeploymentData.getConnStringsToRemove()))
        .slotName(preDeploymentData.getSlotName())
        .artifactFile(artifactResponse != null ? artifactResponse.getArtifactFile() : null)
        .artifactType(artifactResponse != null ? artifactResponse.getArtifactType() : ArtifactType.ZIP)
        .azureWebClientContext(azureWebClientContext)
        .startupCommand(preDeploymentData.getStartupCommand())
        .steadyStateTimeoutInMin(
            azureAppServiceResourceUtilities.getTimeoutIntervalInMin(taskRequest.getTimeoutIntervalInMin()))
        .isBasicDeployment(DEPLOYMENT_SLOT_PRODUCTION_NAME.equalsIgnoreCase(preDeploymentData.getSlotName()))
        .build();
  }

  private void performRollback(AzureLogCallbackProvider logCallbackProvider, AzureWebAppRollbackRequest taskRequest,
      AzureWebClientContext azureWebClientContext, AzureAppServiceDeploymentContext deploymentContext,
      AzureConfig azureConfig, String taskId) {
    AppServiceDeploymentProgress progressMarker = getProgressMarker(taskRequest);
    log.info(String.format("Starting rollback from previous marker - [%s]", progressMarker.getStepName()));

    switch (progressMarker) {
      case SAVE_CONFIGURATION:
        rollbackFromSaveConfigurationState(logCallbackProvider, taskId);
        break;
      case STOP_SLOT:
        rollbackFromStopSlotState(logCallbackProvider, taskRequest, deploymentContext, taskId);
        break;
      case UPDATE_SLOT_CONFIGURATIONS_SETTINGS:
        rollbackFromUpdateConfigurationState(logCallbackProvider, taskRequest, deploymentContext, taskId);
        break;
      case UPDATE_SLOT_CONTAINER_SETTINGS:
      case DEPLOY_TO_SLOT:
        rollbackSetupSlot(taskRequest, deploymentContext, taskId);
        rollbackTrafficShift(logCallbackProvider, taskRequest, azureWebClientContext, deploymentContext, taskId);
        break;
      case SWAP_SLOT:
        swapSlots(azureConfig, logCallbackProvider, taskRequest, taskId);
        rollbackSetupSlot(taskRequest, deploymentContext, taskId);
        rollbackTrafficShift(logCallbackProvider, taskRequest, azureWebClientContext, deploymentContext, taskId);
        break;
      case DEPLOYMENT_COMPLETE:
        noRollback(logCallbackProvider, taskId);
        break;

      default:
        break;
    }
  }

  private void swapSlots(AzureConfig azureConfig, AzureLogCallbackProvider logCallbackProvider,
      AzureWebAppRollbackRequest taskRequest, String taskId) {
    AzureWebClientContext webClientContext = buildAzureWebClientContext(taskRequest.getInfrastructure(), azureConfig);
    azureAppServiceResourceUtilities.swapSlots(webClientContext, logCallbackProvider,
        taskRequest.getInfrastructure().getDeploymentSlot(), taskRequest.getTargetSlot(),
        taskRequest.getTimeoutIntervalInMin(), taskId);
  }

  private AzureAppServiceDockerDeploymentContext toAzureAppServiceDockerDeploymentContext(
      AzureWebAppRollbackRequest taskRequest, AzureWebClientContext azureWebClientContext,
      AzureLogCallbackProvider logCallbackProvider) {
    AzureAppServicePreDeploymentData preDeploymentData = taskRequest.getPreDeploymentData();

    Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettings(preDeploymentData.getAppSettingsToAdd());
    Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettings(preDeploymentData.getAppSettingsToRemove());
    Map<String, AzureAppServiceConnectionString> connSettingsToAdd =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStrings(preDeploymentData.getConnStringsToAdd());
    Map<String, AzureAppServiceConnectionString> connSettingsToRemove =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStrings(preDeploymentData.getConnStringsToRemove());
    Map<String, AzureAppServiceApplicationSetting> dockerSettings =
        AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettings(preDeploymentData.getDockerSettingsToAdd());

    return AzureAppServiceDockerDeploymentContext.builder()
        .logCallbackProvider(logCallbackProvider)
        .appSettingsToAdd(appSettingsToAdd)
        .appSettingsToRemove(appSettingsToRemove)
        .connSettingsToAdd(connSettingsToAdd)
        .connSettingsToRemove(connSettingsToRemove)
        .dockerSettings(dockerSettings)
        .imagePathAndTag(preDeploymentData.getImageNameAndTag())
        .slotName(preDeploymentData.getSlotName())
        .azureWebClientContext(azureWebClientContext)
        .startupCommand(preDeploymentData.getStartupCommand())
        .steadyStateTimeoutInMin(
            azureAppServiceResourceUtilities.getTimeoutIntervalInMin(taskRequest.getTimeoutIntervalInMin()))
        .isBasicDeployment(DEPLOYMENT_SLOT_PRODUCTION_NAME.equalsIgnoreCase(preDeploymentData.getSlotName()))
        .build();
  }

  private void noRollback(AzureLogCallbackProvider logCallbackProvider, String taskId) {
    String message = "The previous deployment was complete. Hence nothing to revert during rollback";
    markCommandUnitAsDone(logCallbackProvider, STOP_DEPLOYMENT_SLOT, message, taskId);
    markCommandUnitAsDone(logCallbackProvider, UPDATE_SLOT_CONFIGURATION_SETTINGS, message, taskId);
    markCommandUnitAsDone(logCallbackProvider, UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS, message, taskId);
    markCommandUnitAsDone(logCallbackProvider, START_DEPLOYMENT_SLOT, message, taskId);
    markCommandUnitAsDone(logCallbackProvider, SLOT_TRAFFIC_PERCENTAGE, message, taskId);
  }

  private void rollbackTrafficShift(AzureLogCallbackProvider logCallbackProvider,
      AzureWebAppRollbackRequest taskRequest, AzureWebClientContext azureWebClientContext,
      AzureAppServiceDeploymentContext deploymentContext, String taskId) {
    AzureAppServicePreDeploymentData preDeploymentData = taskRequest.getPreDeploymentData();
    if (!deploymentContext.isBasicDeployment()
        && isTrafficWeightDifferent(azureWebClientContext, deploymentContext, preDeploymentData)) {
      rollbackUpdateSlotTrafficWeight(preDeploymentData, azureWebClientContext, logCallbackProvider, taskId);
    }
    LogCallback rerouteTrafficLogCallback = logCallbackProvider.obtainLogCallback(SLOT_TRAFFIC_PERCENTAGE, taskId);
    rerouteTrafficLogCallback.saveExecutionLog(NO_TRAFFIC_SHIFT_REQUIRED, INFO, SUCCESS);
  }

  private boolean isTrafficWeightDifferent(AzureWebClientContext azureWebClientContext,
      AzureAppServiceDeploymentContext deploymentContext, AzureAppServicePreDeploymentData preDeploymentData) {
    double slotTrafficWeight =
        azureAppServiceService.getSlotTrafficWeight(azureWebClientContext, deploymentContext.getSlotName());
    return slotTrafficWeight != preDeploymentData.getTrafficWeight();
  }

  private void rollbackUpdateSlotTrafficWeight(AzureAppServicePreDeploymentData preDeploymentData,
      AzureWebClientContext azureWebClientContext, AzureLogCallbackProvider logCallbackProvider, String taskId) {
    double trafficWeight = preDeploymentData.getTrafficWeight();
    String slotName = preDeploymentData.getSlotName();
    azureAppServiceDeploymentService.rerouteProductionSlotTraffic(
        azureWebClientContext, slotName, trafficWeight, logCallbackProvider, taskId);
  }

  private void rollbackSetupSlot(
      AzureWebAppRollbackRequest taskRequest, AzureAppServiceDeploymentContext deploymentContext, String taskId) {
    if ((deploymentContext instanceof AzureAppServicePackageDeploymentContext)
        && ((AzureAppServicePackageDeploymentContext) deploymentContext).getArtifactFile() == null) {
      LogCallback updateSlotLogCallback =
          deploymentContext.getLogCallbackProvider().obtainLogCallback(UPDATE_SLOT_CONFIGURATION_SETTINGS, taskId);
      updateSlotLogCallback.saveExecutionLog(
          "Skip Update Slot Configuration Settings as no previous successful deployment found", INFO, SUCCESS);
      LogCallback deploySlotLogCallback =
          deploymentContext.getLogCallbackProvider().obtainLogCallback(DEPLOY_TO_SLOT, taskId);
      deploySlotLogCallback.saveExecutionLog(
          "Skip Deploying to Slot as no previous successful deployment found", INFO, SUCCESS);
      return;
    }
    deploymentContext.deploy(azureAppServiceDeploymentService, taskRequest.getPreDeploymentData(), taskId);
  }

  private void rollbackFromUpdateConfigurationState(AzureLogCallbackProvider logCallbackProvider,
      AzureWebAppRollbackRequest taskRequest, AzureAppServiceDeploymentContext deploymentContext, String taskId) {
    LogCallback logCallback = logCallbackProvider.obtainLogCallback(UPDATE_SLOT_CONFIGURATION_SETTINGS, taskId);
    azureAppServiceDeploymentService.updateDeploymentSlotConfigurationSettings(
        deploymentContext, taskRequest.getPreDeploymentData(), logCallback);

    markCommandUnitAsDone(
        logCallbackProvider, UPDATE_SLOT_CONFIGURATION_SETTINGS, "Reverted the slot configuration settings", taskId);

    String message = "No artifact/image was deployed during deployment. Hence skipping this step";
    markCommandUnitAsDone(logCallbackProvider, DEPLOY_TO_SLOT, message, taskId);

    message = "Slot traffic was not changed. Hence skipping this step";
    markCommandUnitAsDone(logCallbackProvider, SLOT_TRAFFIC_PERCENTAGE, message, taskId);
  }

  private void rollbackFromStopSlotState(AzureLogCallbackProvider logCallbackProvider,
      AzureWebAppRollbackRequest taskRequest, AzureAppServiceDeploymentContext deploymentContext, String taskId) {
    String message = "Slot configuration was not changed. Hence skipping this step";
    markCommandUnitAsDone(logCallbackProvider, UPDATE_SLOT_CONFIGURATION_SETTINGS, message, taskId);

    LogCallback deployLogCallback = logCallbackProvider.obtainLogCallback(DEPLOY_TO_SLOT, taskId);
    azureAppServiceDeploymentService.startSlotAsyncWithSteadyCheck(
        deploymentContext, taskRequest.getPreDeploymentData(), deployLogCallback);
    markCommandUnitAsDone(logCallbackProvider, DEPLOY_TO_SLOT, "Rollback completed", taskId);
    message = "Slot traffic was not changed. Hence skipping this step";
    markCommandUnitAsDone(logCallbackProvider, SLOT_TRAFFIC_PERCENTAGE, message, taskId);
  }

  protected void markDeploymentStatusAsSuccess(
      AzureWebAppRollbackRequest taskRequest, AzureLogCallbackProvider logCallbackProvider, String taskId) {
    LogCallback logCallback = logCallbackProvider.obtainLogCallback(AzureConstants.DEPLOYMENT_STATUS, taskId);
    logCallback.saveExecutionLog(
        String.format("The following task - [%s] completed successfully", taskRequest.getRequestType().name()),
        LogLevel.INFO, CommandExecutionStatus.SUCCESS);
  }

  private List<AzureAppDeploymentData> getAppServiceDeploymentData(
      AzureWebAppRollbackRequest taskRequest, AzureWebClientContext azureWebClientContext) {
    if (slotDeploymentDidNotHappen(taskRequest)) {
      return Collections.emptyList();
    }
    return azureAppServiceService.fetchDeploymentData(
        azureWebClientContext, taskRequest.getPreDeploymentData().getSlotName());
  }

  private boolean slotDeploymentDidNotHappen(AzureWebAppRollbackRequest taskRequest) {
    AppServiceDeploymentProgress progressMarker = getProgressMarker(taskRequest);
    return (progressMarker == SAVE_CONFIGURATION) || (progressMarker == STOP_SLOT)
        || (progressMarker == UPDATE_SLOT_CONFIGURATIONS_SETTINGS)
        || (progressMarker == UPDATE_SLOT_CONTAINER_SETTINGS);
  }

  private AppServiceDeploymentProgress getProgressMarker(AzureWebAppRollbackRequest taskRequest) {
    AzureAppServicePreDeploymentData preDeploymentData = taskRequest.getPreDeploymentData();
    String deploymentProgressMarker = preDeploymentData.getDeploymentProgressMarker();
    return AppServiceDeploymentProgress.valueOf(deploymentProgressMarker);
  }

  private void rollbackFromSaveConfigurationState(AzureLogCallbackProvider logCallbackProvider, String taskId) {
    String message = "The previous deployment did not start. Hence nothing to revert during rollback";
    markCommandUnitAsDone(logCallbackProvider, UPDATE_SLOT_CONFIGURATION_SETTINGS, message, taskId);
    markCommandUnitAsDone(logCallbackProvider, DEPLOY_TO_SLOT, message, taskId);
    markCommandUnitAsDone(logCallbackProvider, SLOT_TRAFFIC_PERCENTAGE, message, taskId);
  }

  private void markCommandUnitAsDone(
      AzureLogCallbackProvider logCallbackProvider, String commandUnit, String message, String taskId) {
    LogCallback logCallback = logCallbackProvider.obtainLogCallback(commandUnit, taskId);
    logCallback.saveExecutionLog(
        String.format("Message - [%s]", message), LogLevel.INFO, CommandExecutionStatus.SUCCESS);
  }
}
