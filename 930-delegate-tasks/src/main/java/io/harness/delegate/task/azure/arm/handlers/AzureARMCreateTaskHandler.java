/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure.arm.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.task.azure.arm.AzureARMDeploymentService;
import io.harness.delegate.task.azure.arm.AzureARMPreDeploymentData;
import io.harness.delegate.task.azure.arm.AzureARMTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureARMTaskNGResponse;
import io.harness.delegate.task.azure.arm.AzureTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureTaskNGResponse;
import io.harness.delegate.task.azure.arm.deployment.context.DeploymentManagementGroupContext;
import io.harness.delegate.task.azure.arm.deployment.context.DeploymentResourceGroupContext;
import io.harness.delegate.task.azure.arm.deployment.context.DeploymentSubscriptionContext;
import io.harness.delegate.task.azure.arm.deployment.context.DeploymentTenantContext;
import io.harness.delegate.task.azure.common.AzureConnectorMapper;
import io.harness.delegate.task.azure.common.AzureLogCallbackProvider;
import io.harness.exception.TimeoutException;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@OwnedBy(CDP)

public class AzureARMCreateTaskHandler extends AzureARMAbstractTaskHandler {
  @Inject protected AzureConnectorMapper azureConnectorMapper;
  @Inject private AzureARMDeploymentService azureARMDeploymentService;

  @Override
  public AzureTaskNGResponse executeTaskInternal(AzureTaskNGParameters taskNGParameters, String delegateId,
      String taskId, AzureLogCallbackProvider logCallback) throws IOException, TimeoutException, InterruptedException {
    AzureARMTaskNGParameters azureARMTaskNGParameters = (AzureARMTaskNGParameters) taskNGParameters;
    ARMScopeType deploymentScope = azureARMTaskNGParameters.getDeploymentScope();
    AzureConfig azureConfig = azureConnectorMapper.toAzureConfig(azureARMTaskNGParameters.getAzureConnectorDTO());
    switch (deploymentScope) {
      case RESOURCE_GROUP:
        return deployAtResourceGroupScope(azureConfig, logCallback, azureARMTaskNGParameters);
      case SUBSCRIPTION:
        return deployAtSubscriptionScope(azureConfig, logCallback, azureARMTaskNGParameters);
      case MANAGEMENT_GROUP:
        return deployAtManagementGroupScope(azureConfig, logCallback, azureARMTaskNGParameters);
      case TENANT:
        return deployAtTenantScope(azureConfig, logCallback, azureARMTaskNGParameters);
      default:
        throw new IllegalArgumentException(format("Invalid Azure ARM deployment scope: [%s]", deploymentScope));
    }
  }

  private AzureTaskNGResponse deployAtResourceGroupScope(AzureConfig azureConfig, AzureLogCallbackProvider logCallback,
      AzureARMTaskNGParameters azureARMTaskNGParameters) {
    AzureARMPreDeploymentData.AzureARMPreDeploymentDataBuilder preDeploymentData =
        AzureARMPreDeploymentData.builder()
            .resourceGroup(azureARMTaskNGParameters.getResourceGroupName())
            .subscriptionId(azureARMTaskNGParameters.getSubscriptionId());

    DeploymentResourceGroupContext context =
        azureARMBaseHelper.toDeploymentResourceGroupContext(azureARMTaskNGParameters, azureConfig, logCallback);
    try {
      if (!azureARMTaskNGParameters.isRollback()) {
        azureARMDeploymentService.validateTemplate(context);
        String existingResourceGroupTemplate = azureARMDeploymentService.exportExistingResourceGroupTemplate(context);
        preDeploymentData.resourceGroupTemplateJson(existingResourceGroupTemplate);
      }
      String outPuts = azureARMDeploymentService.deployAtResourceGroupScope(context);
      return AzureARMTaskNGResponse.builder()
          .outputs(outPuts)
          .azureARMPreDeploymentData(preDeploymentData.build())
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .build();
    } catch (Exception ex) {
      printDefaultFailureMsgForARMDeploymentUnits(
          ex, context.getLogStreamingTaskClient(), context.getRunningCommandUnit());
      throw ex;
    }
  }

  private AzureTaskNGResponse deployAtSubscriptionScope(AzureConfig azureConfig, AzureLogCallbackProvider logCallback,
      AzureARMTaskNGParameters azureARMTaskNGParameters) {
    DeploymentSubscriptionContext context =
        azureARMBaseHelper.toDeploymentSubscriptionContext(azureARMTaskNGParameters, azureConfig, logCallback);
    try {
      String outputs = azureARMDeploymentService.deployAtSubscriptionScope(context);
      return azureARMBaseHelper.populateDeploymentResponse(outputs);
    } catch (Exception ex) {
      printDefaultFailureMsgForARMDeploymentUnits(
          ex, context.getLogStreamingTaskClient(), context.getRunningCommandUnit());
      throw ex;
    }
  }

  private AzureTaskNGResponse deployAtManagementGroupScope(AzureConfig azureConfig,
      AzureLogCallbackProvider logCallback, AzureARMTaskNGParameters azureARMTaskNGParameters) {
    DeploymentManagementGroupContext context =
        azureARMBaseHelper.toDeploymentManagementGroupContext(azureARMTaskNGParameters, azureConfig, logCallback);
    try {
      String outputs = azureARMDeploymentService.deployAtManagementGroupScope(context);
      return azureARMBaseHelper.populateDeploymentResponse(outputs);
    } catch (Exception ex) {
      printDefaultFailureMsgForARMDeploymentUnits(
          ex, context.getLogStreamingTaskClient(), context.getRunningCommandUnit());
      throw ex;
    }
  }

  private AzureTaskNGResponse deployAtTenantScope(AzureConfig azureConfig, AzureLogCallbackProvider logCallback,
      AzureARMTaskNGParameters azureARMTaskNGParameters) {
    DeploymentTenantContext context =
        azureARMBaseHelper.toDeploymentTenantContext(azureARMTaskNGParameters, azureConfig, logCallback);
    try {
      String outputs = azureARMDeploymentService.deployAtTenantScope(context);
      return azureARMBaseHelper.populateDeploymentResponse(outputs);
    } catch (Exception ex) {
      printDefaultFailureMsgForARMDeploymentUnits(
          ex, context.getLogStreamingTaskClient(), context.getRunningCommandUnit());
      throw ex;
    }
  }
}
