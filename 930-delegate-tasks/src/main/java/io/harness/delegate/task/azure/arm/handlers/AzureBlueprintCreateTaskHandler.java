/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure.arm.handlers;

import static java.lang.String.format;

import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.blueprint.assignment.Assignment;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.task.azure.arm.AzureBlueprintDeploymentService;
import io.harness.delegate.task.azure.arm.AzureBlueprintTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureBlueprintTaskNGResponse;
import io.harness.delegate.task.azure.arm.AzureTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureTaskNGResponse;
import io.harness.delegate.task.azure.arm.deployment.validator.ArtifactsJsonValidator;
import io.harness.delegate.task.azure.arm.deployment.validator.AssignmentJsonValidator;
import io.harness.delegate.task.azure.arm.deployment.validator.BlueprintJsonValidator;
import io.harness.delegate.task.azure.arm.deployment.validator.DeploymentBlueprintContextValidator;
import io.harness.delegate.task.azure.common.AzureConnectorMapper;
import io.harness.delegate.task.azure.common.AzureLogCallbackProvider;
import io.harness.delegate.task.azure.common.validator.Validators;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.TimeoutException;
import io.harness.serializer.JsonUtils;

import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentBlueprintContext;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class AzureBlueprintCreateTaskHandler extends AzureARMAbstractTaskHandler {
  @Inject private AzureBlueprintDeploymentService azureBlueprintDeploymentService;
  @Inject protected AzureConnectorMapper azureConnectorMapper;

  @Override
  public AzureTaskNGResponse executeTaskInternal(AzureTaskNGParameters taskNGParameters, String delegateId,
      String taskId, AzureLogCallbackProvider logCallback) throws IOException, TimeoutException, InterruptedException {
    AzureBlueprintTaskNGParameters azureBlueprintTaskNGParameters = (AzureBlueprintTaskNGParameters) taskNGParameters;
    AzureConfig azureConfig = azureConnectorMapper.toAzureConfig(azureBlueprintTaskNGParameters.getAzureConnectorDTO());

    azureBlueprintDeploymentService.deployBlueprintAtResourceScope(
        toDeploymentBlueprintContext(azureBlueprintTaskNGParameters, azureConfig, logCallback));
    return AzureBlueprintTaskNGResponse.builder().build();
  }

  // TODO: Candidate
  private DeploymentBlueprintContext toDeploymentBlueprintContext(
      AzureBlueprintTaskNGParameters azureBlueprintTaskNGParameters, AzureConfig azureConfig,
      AzureLogCallbackProvider logCallback) {
    String assignmentJson = azureBlueprintTaskNGParameters.getAssignmentJson();
    String blueprintJson = azureBlueprintTaskNGParameters.getBlueprintJson();
    Map<String, String> artifacts =
        AzureResourceUtility.fixArtifactNames(azureBlueprintTaskNGParameters.getArtifacts());
    String assignmentName = azureBlueprintTaskNGParameters.getAssignmentName();

    Validators.validate(assignmentJson, new AssignmentJsonValidator());
    Validators.validate(blueprintJson, new BlueprintJsonValidator());
    Validators.validate(artifacts, new ArtifactsJsonValidator());

    Assignment assignment = JsonUtils.asObject(assignmentJson, Assignment.class);
    String blueprintId = assignment.getProperties().getBlueprintId();
    String definitionResourceScope = AzureResourceUtility.getDefinitionResourceScope(blueprintId);
    String versionId = AzureResourceUtility.getVersionId(blueprintId);
    String blueprintName = AzureResourceUtility.getBlueprintName(blueprintId);
    assignment.setName(AzureResourceUtility.generateAssignmentNameIfBlank(assignmentName, blueprintName));
    String assignmentSubscriptionId = AzureResourceUtility.getAssignmentSubscriptionId(assignment);
    String assignmentResourceScope = AzureResourceUtility.getAssignmentResourceScope(assignment);

    checkBlueprintNameInBlueprintJson(blueprintJson, blueprintName);

    DeploymentBlueprintContext deploymentBlueprintContext =
        DeploymentBlueprintContext.builder()
            .azureConfig(azureConfig)
            .definitionResourceScope(definitionResourceScope)
            .versionId(versionId)
            .blueprintName(blueprintName)
            .blueprintJSON(blueprintJson)
            .artifacts(artifacts)
            .assignment(assignment)
            .assignmentSubscriptionId(assignmentSubscriptionId)
            .assignmentResourceScope(assignmentResourceScope)
            .assignmentJSON(assignmentJson)
            .roleAssignmentName(AzureResourceUtility.getRandomUUID())
            .logStreamingTaskClient(logCallback)
            .steadyStateTimeoutInMin(
                (int) azureBlueprintTaskNGParameters.getTimeoutInMs() * 1000) // TODO This may need to be in min
            .build();

    Validators.validate(deploymentBlueprintContext, new DeploymentBlueprintContextValidator());
    return deploymentBlueprintContext;
  }

  // TODO: Candidate
  private void checkBlueprintNameInBlueprintJson(String blueprintJson, String blueprintName) {
    Optional<String> blueprintNameFromBlueprintJson =
        AzureResourceUtility.getBlueprintNameFromBlueprintJson(blueprintJson);
    if (blueprintNameFromBlueprintJson.isPresent() && blueprintName.equals(blueprintNameFromBlueprintJson.get())) {
      throw new InvalidArgumentsException(format(
          "Not match blueprint name found in blueprint json file with properties.blueprintId property in assign json file. "
              + "Found name in blueprint json: %s, and properties.blueprintId: %s",
          blueprintNameFromBlueprintJson.get(), blueprintName));
    }
  }
}
