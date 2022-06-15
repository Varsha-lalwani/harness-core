/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.provision.azure.AzureCommonHelper.BLUEPRINT_IDENTIFIER;
import static io.harness.cdng.provision.azure.AzureCommonHelper.DEFAULT_TIMEOUT;
import static io.harness.delegate.task.azure.arm.AzureARMTaskType.BLUEPRINT_DEPLOYMENT;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.provision.azure.beans.AzureCreateBPPassThroughData;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.azure.arm.AzureBlueprintTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureTaskNGParameters;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
@Singleton
public class AzureCreateBPStepHelper {
  @Inject private CDStepHelper cdStepHelper;
  @Inject private AzureCommonHelper azureCommonHelper;

  private static final String BLUEPRINT_JSON = "blueprint.json";
  private static final String ASSIGN_JSON = "assign.json";
  private static final String ARTIFACTS = "artifacts/";

  public TaskChainResponse startChainLink(AzureCreateBPStepExecutor azureCreateStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters) {
    AzureCreateBPStepConfigurationParameters azureCreateBPStepConfigurationParameters =
        ((AzureCreateBPStepParameters) stepElementParameters.getSpec()).getConfiguration();
    ConnectorInfoDTO connectorDTO =
        cdStepHelper.getConnector(azureCreateBPStepConfigurationParameters.getConnectorRef().getValue(), ambiance);
    if (!(connectorDTO.getConnectorConfig() instanceof AzureConnectorDTO)) {
      throw new InvalidRequestException("Invalid connector selected in Azure step. Select Azure connector");
    }
    List<GitFetchFilesConfig> gitFetchFilesConfigs =
        Collections.singletonList(azureCommonHelper.getTemplateGitFetchFileConfig(
            ambiance, azureCreateBPStepConfigurationParameters.getTemplateFile(), AzureDeploymentTypes.BLUEPRINT));

    AzureCreateBPPassThroughData passThroughData = AzureCreateBPPassThroughData.builder().build();
    return azureCommonHelper.getGitFetchFileTaskChainResponse(
        ambiance, gitFetchFilesConfigs, stepElementParameters, passThroughData);
  }

  private AzureTaskNGParameters getAzureTaskNGParams(Ambiance ambiance, StepElementParameters stepElementParameters,
      AzureConnectorDTO connectorConfig, PassThroughData passThroughData) {
    AzureCreateBPStepParameters azureCreateStepParameters =
        (AzureCreateBPStepParameters) stepElementParameters.getSpec();
    AzureCreateBPPassThroughData azureCreateBPPassThroughData = (AzureCreateBPPassThroughData) passThroughData;

    return AzureBlueprintTaskNGParameters.builder()
        .accountId(AmbianceUtils.getAccountId(ambiance))
        .taskType(BLUEPRINT_DEPLOYMENT)
        .connectorDTO(connectorConfig)
        .timeoutInMs(StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), DEFAULT_TIMEOUT))
        .blueprintJson(azureCreateBPPassThroughData.getBlueprintBody())
        .assignmentJson(azureCreateBPPassThroughData.getAssignBody())
        .artifacts(azureCreateBPPassThroughData.getArtifacts())
        .assignmentName(azureCreateStepParameters.getConfiguration().getAssignmentName().getValue())
        .encryptedDataDetailList(azureCommonHelper.getAzureEncryptionDetails(ambiance, connectorConfig))
        .build();
  }
  private void populatePassThroughData(AzureCreateBPPassThroughData passThroughData, String blueprintBody,
      String assignmentBody, Map<String, String> artifacts) {
    passThroughData.setArtifacts(artifacts);
    passThroughData.setAssignBody(assignmentBody);
    passThroughData.setBlueprintBody(blueprintBody);
  }

  TaskChainResponse handleGitFetchResponse(AzureCreateBPStepExecutor azureCreateStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, PassThroughData passThroughData, GitFetchResponse responseData) {
    Map<String, FetchFilesResult> filesFromMultipleRepo = responseData.getFilesFromMultipleRepo();

    String assignBody = null;
    String blueprintBody = null;
    Map<String, String> artifacts = new HashMap<>();
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
    AzureCreateBPStepParameters spec = (AzureCreateBPStepParameters) stepElementParameters.getSpec();

    populatePassThroughData((AzureCreateBPPassThroughData) passThroughData, blueprintBody, assignBody, artifacts);
    AzureConnectorDTO connectorDTO = azureCommonHelper.getAzureConnectorConfig(
        ambiance, ParameterField.createValueField(spec.getConfiguration().getConnectorRef().getValue()));

    AzureTaskNGParameters azureTaskNGParameters =
        getAzureTaskNGParams(ambiance, stepElementParameters, connectorDTO, passThroughData);
    return azureCreateStepExecutor.executeCreateTask(
        ambiance, stepElementParameters, azureTaskNGParameters, passThroughData);
  }

  public TaskChainResponse executeNextLink(AzureCreateBPStepExecutor azureCreateStepExecutor, Ambiance ambiance,
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
