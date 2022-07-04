/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure.arm.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.AzureConstants;
import io.harness.delegate.task.azure.arm.AzureARMBaseHelper;
import io.harness.delegate.task.azure.arm.AzureTaskNGParameters;
import io.harness.delegate.task.azure.arm.AzureTaskNGResponse;
import io.harness.delegate.task.azure.common.AzureLogCallbackProvider;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.TimeoutException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import com.google.inject.Inject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public abstract class AzureARMAbstractTaskHandler {
  @Inject AzureARMBaseHelper azureARMBaseHelper;

  public abstract AzureTaskNGResponse executeTaskInternal(AzureTaskNGParameters taskNGParameters, String delegateId,
      String taskId, AzureLogCallbackProvider logCallback) throws IOException, TimeoutException, InterruptedException;

  public AzureTaskNGResponse executeTask(AzureTaskNGParameters azureTaskNGParameters, String delegateId, String taskId,
      AzureLogCallbackProvider logCallback) throws Exception {
    AzureTaskNGResponse response = executeTaskInternal(azureTaskNGParameters, delegateId, taskId, logCallback);
    if (SUCCESS.equals(response.getCommandExecutionStatus())) {
      logCallback.obtainLogCallback("Create").saveExecutionLog("Execution finished successfully.", LogLevel.INFO);
    } else {
      logCallback.obtainLogCallback("Create").saveExecutionLog("Execution has been failed.", LogLevel.ERROR);
    }
    return response;
  }

  protected void printDefaultFailureMsgForARMDeploymentUnits(
      Exception ex, AzureLogCallbackProvider logStreamingTaskClient, final String runningCommandUnit) {
    if ((ex instanceof InvalidRequestException) || isBlank(runningCommandUnit)) {
      return;
    }

    if (AzureConstants.EXECUTE_ARM_DEPLOYMENT.equals(runningCommandUnit)) {
      printErrorMsg(logStreamingTaskClient, runningCommandUnit, format("%nError while executing ARM deployment"));
    }

    if (AzureConstants.ARM_DEPLOYMENT_STEADY_STATE.equals(runningCommandUnit)) {
      printErrorMsg(logStreamingTaskClient, runningCommandUnit, format("%nError during ARM deployment steady check"));
    }

    if (AzureConstants.ARM_DEPLOYMENT_OUTPUTS.equals(runningCommandUnit)) {
      printErrorMsg(logStreamingTaskClient, runningCommandUnit, format("%nError while getting ARM deployment outputs"));
    }
  }
  protected void printErrorMsg(
      AzureLogCallbackProvider logStreamingTaskClient, final String runningCommandUnit, final String errorMsg) {
    if (isBlank(runningCommandUnit)) {
      return;
    }
    LogCallback logCallback = logStreamingTaskClient.obtainLogCallback(runningCommandUnit);
    logCallback.saveExecutionLog(errorMsg, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
  }
}
