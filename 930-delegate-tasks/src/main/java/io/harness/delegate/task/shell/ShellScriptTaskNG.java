/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import static io.harness.delegate.task.shell.winrm.WinRmCommandConstants.SESSION_TIMEOUT;

import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.shell.winrm.WinRmConfigAuthEnhancer;
import io.harness.delegate.task.winrm.WinRmExecutorFactoryNG;
import io.harness.delegate.task.winrm.WinRmSessionConfig;
import io.harness.delegate.task.winrm.WinRmSessionConfig.WinRmSessionConfigBuilder;
import io.harness.k8s.K8sConstants;
import io.harness.k8s.KubernetesContainerService;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.WinRmCredentialsSpecDTO;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.ExecuteCommandResponse;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ScriptSshExecutor;
import io.harness.shell.ShellExecutorConfig;
import io.harness.shell.SshSessionConfig;
import io.harness.shell.SshSessionManager;

import software.wings.core.winrm.executors.WinRmExecutor;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class ShellScriptTaskNG extends AbstractDelegateRunnableTask {
  public static final String COMMAND_UNIT = "Execute";

  @Inject private ShellExecutorFactoryNG shellExecutorFactory;
  @Inject private SshExecutorFactoryNG sshExecutorFactoryNG;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private SshSessionConfigMapper sshSessionConfigMapper;
  @Inject private WinRmExecutorFactoryNG winRmExecutorFactoryNG;
  @Inject private WinRmConfigAuthEnhancer winRmConfigAuthEnhancer;

  public ShellScriptTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ShellScriptTaskParametersNG shellScriptTaskParameters = (ShellScriptTaskParametersNG) parameters;

    switch (shellScriptTaskParameters.getScriptType()) {
      case BASH:
        return runBashScript(shellScriptTaskParameters);
      case POWERSHELL:
        return runPowerShellScript(shellScriptTaskParameters);
      default:
        throw new IllegalArgumentException(
            String.format("Invalid script type provided %s", shellScriptTaskParameters.getScriptType()));
    }
  }

  private ShellScriptTaskResponseNG runBashScript(ShellScriptTaskParametersNG taskParameters) {
    CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

    if (taskParameters.isExecuteOnDelegate()) {
      ShellExecutorConfig shellExecutorConfig = getShellExecutorConfig(taskParameters);
      ScriptProcessExecutor executor =
          shellExecutorFactory.getExecutor(shellExecutorConfig, this.getLogStreamingTaskClient(), commandUnitsProgress);
      // TODO: check later
      // if (taskParameters.isLocalOverrideFeatureFlag()) {
      //   taskParameters.setScript(delegateLocalConfigService.replacePlaceholdersWithLocalConfig(taskParameters.getScript()));
      // }
      ExecuteCommandResponse executeCommandResponse =
          executor.executeCommandString(taskParameters.getScript(), taskParameters.getOutputVars());
      return ShellScriptTaskResponseNG.builder()
          .executeCommandResponse(executeCommandResponse)
          .status(executeCommandResponse.getStatus())
          .errorMessage(getErrorMessage(executeCommandResponse.getStatus()))
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();
    } else {
      try {
        SshSessionConfig sshSessionConfig = getSshSessionConfig(taskParameters);
        ScriptSshExecutor executor =
            sshExecutorFactoryNG.getExecutor(sshSessionConfig, this.getLogStreamingTaskClient(), commandUnitsProgress);
        ExecuteCommandResponse executeCommandResponse =
            executor.executeCommandString(taskParameters.getScript(), taskParameters.getOutputVars());
        return ShellScriptTaskResponseNG.builder()
            .executeCommandResponse(executeCommandResponse)
            .status(executeCommandResponse.getStatus())
            .errorMessage(getErrorMessage(executeCommandResponse.getStatus()))
            .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
            .build();
      } catch (Exception e) {
        log.error("Bash Script Failed to execute.", e);
        return ShellScriptTaskResponseNG.builder()
            .status(CommandExecutionStatus.FAILURE)
            .errorMessage("Bash Script Failed to execute. Reason: " + e.getMessage())
            .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
            .build();
      } finally {
        SshSessionManager.evictAndDisconnectCachedSession(taskParameters.getExecutionId(), taskParameters.getHost());
      }
    }
  }

  private ShellScriptTaskResponseNG runPowerShellScript(ShellScriptTaskParametersNG taskParameters) {
    CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

    try {
      WinRmSessionConfigBuilder configBuilder = WinRmSessionConfig.builder()
                                                    .accountId(taskParameters.getAccountId())
                                                    .executionId(taskParameters.getExecutionId())
                                                    .workingDirectory(taskParameters.getWorkingDirectory())
                                                    .commandUnitName(COMMAND_UNIT)
                                                    .environment(taskParameters.getEnvironmentVariables())
                                                    .hostname(taskParameters.getHost())
                                                    .timeout(SESSION_TIMEOUT);

      WinRmSessionConfig config =
          winRmConfigAuthEnhancer.configureAuthentication((WinRmCredentialsSpecDTO) taskParameters.getSshKeySpecDTO(),
              taskParameters.getEncryptionDetails(), configBuilder, taskParameters.isUseWinRMKerberosUniqueCacheFile());

      WinRmExecutor executor = winRmExecutorFactoryNG.getExecutor(
          config, taskParameters.isDisableCommandEncoding(), this.getLogStreamingTaskClient(), commandUnitsProgress);

      ExecuteCommandResponse executeCommandResponse =
          executor.executeCommandString(taskParameters.getScript(), taskParameters.getOutputVars());

      return ShellScriptTaskResponseNG.builder()
          .executeCommandResponse(executeCommandResponse)
          .status(executeCommandResponse.getStatus())
          .errorMessage(getErrorMessage(executeCommandResponse.getStatus()))
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();
    } catch (Exception e) {
      log.error("PowerShell Script Failed to execute.", e);
      return ShellScriptTaskResponseNG.builder()
          .status(CommandExecutionStatus.FAILURE)
          .errorMessage("Power Shell Script Failed to execute. Reason: " + e.getMessage())
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();
    }
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

  private SshSessionConfig getSshSessionConfig(ShellScriptTaskParametersNG taskParameters) {
    if (!(taskParameters.getSshKeySpecDTO() instanceof SSHKeySpecDTO)) {
      throw new IllegalArgumentException(
          String.format("Invalid secret type provided %s", taskParameters.getSshKeySpecDTO().getClass()));
    }

    SshSessionConfig sshSessionConfig = sshSessionConfigMapper.getSSHSessionConfig(
        (SSHKeySpecDTO) taskParameters.getSshKeySpecDTO(), taskParameters.getEncryptionDetails());

    sshSessionConfig.setAccountId(taskParameters.getAccountId());
    sshSessionConfig.setExecutionId(taskParameters.getExecutionId());
    sshSessionConfig.setHost(taskParameters.getHost());
    sshSessionConfig.setWorkingDirectory(taskParameters.getWorkingDirectory());
    sshSessionConfig.setCommandUnitName(COMMAND_UNIT);
    return sshSessionConfig;
  }

  private ShellExecutorConfig getShellExecutorConfig(ShellScriptTaskParametersNG taskParameters) {
    String kubeConfigFileContent = taskParameters.getScript().contains(K8sConstants.HARNESS_KUBE_CONFIG_PATH)
            && taskParameters.getK8sInfraDelegateConfig() != null
        ? containerDeploymentDelegateBaseHelper.getKubeconfigFileContent(taskParameters.getK8sInfraDelegateConfig())
        : "";

    return ShellExecutorConfig.builder()
        .accountId(taskParameters.getAccountId())
        .executionId(taskParameters.getExecutionId())
        .commandUnitName(COMMAND_UNIT)
        .workingDirectory(taskParameters.getWorkingDirectory())
        .environment(taskParameters.getEnvironmentVariables())
        .kubeConfigContent(kubeConfigFileContent)
        .scriptType(taskParameters.getScriptType())
        .build();
  }
}
