/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.shell.winrm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.shell.winrm.WinRmCommandConstants.SESSION_TIMEOUT;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.NO_CONFIG_FILE_PROVIDED;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.NO_CONFIG_FILE_PROVIDED_EXPLANATION;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.NO_CONFIG_FILE_PROVIDED_HINT;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.NO_DESTINATION_PATH_SPECIFIED;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.NO_DESTINATION_PATH_SPECIFIED_EXPLANATION;
import static io.harness.delegate.task.ssh.exception.SshExceptionConstants.NO_DESTINATION_PATH_SPECIFIED_HINT;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.storeconfig.HarnessStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfigType;
import io.harness.delegate.task.shell.CommandTaskParameters;
import io.harness.delegate.task.shell.WinrmTaskParameters;
import io.harness.delegate.task.shell.ssh.CommandHandler;
import io.harness.delegate.task.ssh.CopyCommandUnit;
import io.harness.delegate.task.ssh.NgCommandUnit;
import io.harness.delegate.task.ssh.config.ConfigFileParameters;
import io.harness.delegate.task.ssh.config.SecretConfigFile;
import io.harness.delegate.task.winrm.FileBasedWinRmExecutorNG;
import io.harness.delegate.task.winrm.WinRmExecutorFactoryNG;
import io.harness.delegate.task.winrm.WinRmSessionConfig;
import io.harness.delegate.task.winrm.WinRmSessionConfig.WinRmSessionConfigBuilder;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.runtime.WinRmCommandExecutionException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.ssh.FileSourceType;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
@Singleton
public class WinRmCopyCommandHandler implements CommandHandler {
  @Inject private WinRmExecutorFactoryNG winRmExecutorFactoryNG;
  @Inject private WinRmConfigAuthEnhancer winRmConfigAuthEnhancer;
  @Inject private SecretDecryptionService secretDecryptionService;

  @Override
  public CommandExecutionStatus handle(CommandTaskParameters parameters, NgCommandUnit commandUnit,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    if (!(parameters instanceof WinrmTaskParameters)) {
      throw new InvalidRequestException("Invalid task parameters submitted for command task.");
    }
    WinrmTaskParameters winRmCommandTaskParameters = (WinrmTaskParameters) parameters;
    if (!(commandUnit instanceof CopyCommandUnit)) {
      throw new InvalidRequestException("Invalid command unit specified for command task.");
    }

    CopyCommandUnit copyCommandUnit = (CopyCommandUnit) commandUnit;
    WinRmSessionConfigBuilder configBuilder = WinRmSessionConfig.builder()
                                                  .accountId(winRmCommandTaskParameters.getAccountId())
                                                  .executionId(winRmCommandTaskParameters.getExecutionId())
                                                  .workingDirectory(copyCommandUnit.getWorkingDirectory())
                                                  .commandUnitName(copyCommandUnit.getName())
                                                  .environment(winRmCommandTaskParameters.getEnvironmentVariables())
                                                  .hostname(winRmCommandTaskParameters.getHost())
                                                  .timeout(SESSION_TIMEOUT);

    WinRmSessionConfig config =
        winRmConfigAuthEnhancer.configureAuthentication(winRmCommandTaskParameters, configBuilder);
    FileBasedWinRmExecutorNG executor = winRmExecutorFactoryNG.getFiledBasedWinRmExecutor(config,
        winRmCommandTaskParameters.isDisableWinRMCommandEncodingFFSet(), logStreamingTaskClient, commandUnitsProgress);

    if (EmptyPredicate.isEmpty(copyCommandUnit.getDestinationPath())) {
      log.info("Destination path no provided for copy command unit");
      throw NestedExceptionUtils.hintWithExplanationException(
          format(NO_DESTINATION_PATH_SPECIFIED_HINT, copyCommandUnit.getName()),
          format(NO_DESTINATION_PATH_SPECIFIED_EXPLANATION, copyCommandUnit.getName()),
          new WinRmCommandExecutionException(NO_DESTINATION_PATH_SPECIFIED));
    }

    if (FileSourceType.ARTIFACT.equals(copyCommandUnit.getSourceType())) {
      return copyArtifact(winRmCommandTaskParameters, copyCommandUnit, executor);
    } else if (FileSourceType.CONFIG.equals(copyCommandUnit.getSourceType())) {
      return copyConfigFiles(winRmCommandTaskParameters, copyCommandUnit, executor);
    }

    return CommandExecutionStatus.SUCCESS;
  }

  private CommandExecutionStatus copyConfigFiles(WinrmTaskParameters winRmCommandTaskParameters,
      CopyCommandUnit copyCommandUnit, FileBasedWinRmExecutorNG executor) {
    CommandExecutionStatus result = CommandExecutionStatus.SUCCESS;
    List<ConfigFileParameters> configFiles = getConfigFileParameters(winRmCommandTaskParameters);
    for (ConfigFileParameters configFile : configFiles) {
      log.info(format("Copying config file : %s, isEncrypted: %b", configFile.getFileName(), configFile.isEncrypted()));
      if (configFile.isEncrypted()) {
        SecretConfigFile secretConfigFile = (SecretConfigFile) secretDecryptionService.decrypt(
            configFile.getSecretConfigFile(), configFile.getEncryptionDataDetails());
        String fileData = new String(secretConfigFile.getEncryptedConfigFile().getDecryptedValue());
        configFile.setFileContent(fileData);
        configFile.setFileSize(fileData.getBytes(StandardCharsets.UTF_8).length);
      }
      configFile.setDestinationPath(copyCommandUnit.getDestinationPath());
      result = executor.copyConfigFiles(configFile);
      if (CommandExecutionStatus.FAILURE.equals(result)) {
        log.info("Failed to copy config file: " + configFile.getFileName());
        break;
      }
    }
    return result;
  }

  private CommandExecutionStatus copyArtifact(
      WinrmTaskParameters taskParameters, CopyCommandUnit copyCommandUnit, FileBasedWinRmExecutorNG executor) {
    log.info("About to copy artifact");
    CommandExecutionStatus result;
    try {
      result = executor.copyArtifacts(taskParameters, copyCommandUnit);
    } catch (Exception e) {
      result = CommandExecutionStatus.FAILURE;
    }
    if (CommandExecutionStatus.FAILURE.equals(result)) {
      log.info("Failed to copy artifact with id: " + taskParameters.getArtifactDelegateConfig().getIdentifier());
    }
    return result;
  }

  private List<ConfigFileParameters> getConfigFileParameters(WinrmTaskParameters winrmTaskParameters) {
    if (winrmTaskParameters.getFileDelegateConfig() == null) {
      throw NestedExceptionUtils.hintWithExplanationException(NO_CONFIG_FILE_PROVIDED_HINT,
          NO_CONFIG_FILE_PROVIDED_EXPLANATION, new WinRmCommandExecutionException(NO_CONFIG_FILE_PROVIDED));
    }

    return winrmTaskParameters.getFileDelegateConfig()
        .getStores()
        .stream()
        .filter(storeDelegateConfig -> StoreDelegateConfigType.HARNESS.equals(storeDelegateConfig.getType()))
        .map(storeDelegateConfig -> (HarnessStoreDelegateConfig) storeDelegateConfig)
        .findFirst()
        .map(harnessStoreDelegateConfig -> Lists.newArrayList(harnessStoreDelegateConfig.getConfigFiles()))
        .orElse(Lists.newArrayList());
  }
}
