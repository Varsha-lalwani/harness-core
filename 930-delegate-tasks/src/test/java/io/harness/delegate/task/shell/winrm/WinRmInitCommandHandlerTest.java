/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.shell.winrm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.BOJAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.shell.WinrmTaskParameters;
import io.harness.delegate.task.ssh.NgInitCommandUnit;
import io.harness.delegate.task.ssh.WinRmInfraDelegateConfig;
import io.harness.delegate.task.winrm.WinRmExecutorFactoryNG;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import software.wings.core.winrm.executors.WinRmExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@OwnedBy(CDP)
@RunWith(MockitoJUnitRunner.Silent.class)
public class WinRmInitCommandHandlerTest {
  @Mock private WinRmExecutorFactoryNG winRmExecutorFactoryNG;
  @Mock private WinRmConfigAuthEnhancer winRmConfigAuthEnhancer;
  @Mock private ILogStreamingTaskClient iLogStreamingTaskClient;
  @Mock private Map<String, Object> taskContext;

  @InjectMocks private WinRmInitCommandHandler winRmInitCommandHandler;

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testShouldExecuteInitCommandWithWinRmExecutor() {
    List<String> outputVariables = Collections.singletonList("variable");
    WinRmInfraDelegateConfig winRmInfraDelegateConfig = mock(WinRmInfraDelegateConfig.class);
    NgInitCommandUnit initCommandUnit = NgInitCommandUnit.builder().build();
    WinrmTaskParameters winrmTaskParameters = WinrmTaskParameters.builder()
                                                  .commandUnits(Arrays.asList(initCommandUnit))
                                                  .winRmInfraDelegateConfig(winRmInfraDelegateConfig)
                                                  .executeOnDelegate(true)
                                                  .disableWinRMCommandEncodingFFSet(true)
                                                  .outputVariables(outputVariables)
                                                  .build();
    WinRmExecutor executor = mock(WinRmExecutor.class);
    when(winRmExecutorFactoryNG.getExecutor(any(), anyBoolean(), any(), any())).thenReturn(executor);
    when(executor.executeCommandString(any(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus result = winRmInitCommandHandler.handle(winrmTaskParameters, initCommandUnit,
        iLogStreamingTaskClient, CommandUnitsProgress.builder().build(), taskContext);
    assertThat(result).isEqualTo(CommandExecutionStatus.SUCCESS);
  }
}
