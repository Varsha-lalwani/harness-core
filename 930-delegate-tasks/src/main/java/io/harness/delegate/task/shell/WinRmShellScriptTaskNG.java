/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.shell.winrm.WinRmCommandConstants.SESSION_TIMEOUT;

import static software.wings.common.Constants.WINDOWS_HOME_DIR;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.shell.winrm.WinRmConfigAuthEnhancer;
import io.harness.delegate.task.winrm.WinRmExecutorFactoryNG;
import io.harness.delegate.task.winrm.WinRmSessionConfig;
import io.harness.delegate.task.winrm.WinRmSessionConfig.WinRmSessionConfigBuilder;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.ExecuteCommandResponse;

import software.wings.core.winrm.executors.WinRmExecutor;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class WinRmShellScriptTaskNG extends AbstractDelegateRunnableTask {
  public static final String COMMAND_UNIT = "Execute";
  public static final String INIT_UNIT = "Initialize";

  @Inject private WinRmExecutorFactoryNG winRmExecutorFactoryNG;
  @Inject private WinRmConfigAuthEnhancer winRmConfigAuthEnhancer;

  public WinRmShellScriptTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    WinRmShellScriptTaskParametersNG shellScriptTaskParameters = (WinRmShellScriptTaskParametersNG) parameters;
    CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

    try {
      ShellScriptTaskResponseNG initStatus =
          executeInit(shellScriptTaskParameters, commandUnitsProgress, this.getLogStreamingTaskClient());

      if (CommandExecutionStatus.FAILURE.equals(initStatus.getStatus())) {
        return initStatus;
      }

      return executeCommand(shellScriptTaskParameters, commandUnitsProgress, this.getLogStreamingTaskClient());
    } catch (Exception e) {
      log.error("PowerShell Script Failed to execute.", e);
      return ShellScriptTaskResponseNG.builder()
          .status(CommandExecutionStatus.FAILURE)
          .errorMessage("Power Shell Script Failed to execute. Reason: " + e.getMessage())
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();
    }
  }

  private ShellScriptTaskResponseNG executeInit(WinRmShellScriptTaskParametersNG taskParameters,
      CommandUnitsProgress commandUnitsProgress, ILogStreamingTaskClient logStreamingTaskClient) {
    WinRmSessionConfigBuilder configBuilder = WinRmSessionConfig.builder()
                                                  .accountId(taskParameters.getAccountId())
                                                  .executionId(taskParameters.getExecutionId())
                                                  .workingDirectory(WINDOWS_HOME_DIR)
                                                  .commandUnitName(INIT_UNIT)
                                                  .environment(taskParameters.getEnvironmentVariables())
                                                  .hostname(taskParameters.getHost())
                                                  .timeout(SESSION_TIMEOUT);

    WinRmSessionConfig config = null;
    //    WinRmSessionConfig config =
    //        winRmConfigAuthEnhancer.configureAuthentication((WinRmCredentialsSpecDTO)
    //        taskParameters.getSshKeySpecDTO(),
    //            taskParameters.getEncryptionDetails(), configBuilder,
    //            taskParameters.isUseWinRMKerberosUniqueCacheFile());

    WinRmExecutor executor = winRmExecutorFactoryNG.getExecutor(
        config, taskParameters.isDisableCommandEncoding(), logStreamingTaskClient, commandUnitsProgress);

    CommandExecutionStatus commandExecutionStatus =
        executor.executeCommandString(getInitCommand(taskParameters.getWorkingDirectory()), true);

    return ShellScriptTaskResponseNG.builder()
        .status(commandExecutionStatus)
        .errorMessage(getErrorMessage(commandExecutionStatus))
        .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
        .build();
  }

  private ShellScriptTaskResponseNG executeCommand(WinRmShellScriptTaskParametersNG taskParameters,
      CommandUnitsProgress commandUnitsProgress, ILogStreamingTaskClient logStreamingTaskClient) {
    WinRmSessionConfigBuilder configBuilder = WinRmSessionConfig.builder()
                                                  .accountId(taskParameters.getAccountId())
                                                  .executionId(taskParameters.getExecutionId())
                                                  .workingDirectory(taskParameters.getWorkingDirectory())
                                                  .commandUnitName(COMMAND_UNIT)
                                                  .environment(taskParameters.getEnvironmentVariables())
                                                  .hostname(taskParameters.getHost())
                                                  .timeout(SESSION_TIMEOUT);

    WinRmSessionConfig config = null;
    //        winRmConfigAuthEnhancer.configureAuthentication((WinRmCredentialsSpecDTO)
    //        taskParameters.getSshKeySpecDTO(),
    //            taskParameters.getEncryptionDetails(), configBuilder,
    //            taskParameters.isUseWinRMKerberosUniqueCacheFile());

    WinRmExecutor executor = winRmExecutorFactoryNG.getExecutor(
        config, taskParameters.isDisableCommandEncoding(), logStreamingTaskClient, commandUnitsProgress);

    ExecuteCommandResponse executeCommandResponse =
        executor.executeCommandString(taskParameters.getScript(), taskParameters.getOutputVars());

    return ShellScriptTaskResponseNG.builder()
        .executeCommandResponse(executeCommandResponse)
        .status(executeCommandResponse.getStatus())
        .errorMessage(getErrorMessage(executeCommandResponse.getStatus()))
        .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
        .build();
  }

  private String getErrorMessage(CommandExecutionStatus status) {
    switch (status) {
      case QUEUED:
        return "Shell Script execution queued.";
      case FAILURE:
        return "Shell Script execution failed. Please check execution logs.";
      case RUNNING:
        return "Shell Script execution running.";
      case SKIPPED:
        return "Shell Script execution skipped.";
      case SUCCESS:
      default:
        return "";
    }
  }

  private String getInitCommand(String workingDirectory) {
    String script = "$RUNTIME_PATH=[System.Environment]::ExpandEnvironmentVariables(\"%s\")%n"
        + "if(!(Test-Path \"$RUNTIME_PATH\"))%n"
        + "{%n"
        + "    New-Item -ItemType Directory -Path \"$RUNTIME_PATH\"%n"
        + "    Write-Host \"$RUNTIME_PATH Folder Created Successfully.\"%n"
        + "}%n"
        + "else%n"
        + "{%n"
        + "    Write-Host \"${RUNTIME_PATH} Folder already exists.\"%n"
        + "}";

    return String.format(script, isNotEmpty(workingDirectory) ? workingDirectory : WINDOWS_HOME_DIR);
  }
}
