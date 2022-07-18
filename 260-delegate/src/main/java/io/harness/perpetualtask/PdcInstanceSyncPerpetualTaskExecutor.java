/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.SafeHttpCall.execute;

import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.instancesync.*;
import io.harness.delegate.beans.instancesync.info.PdcServerInstanceInfo;
import io.harness.grpc.utils.AnyUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.perpetualtask.instancesync.PdcInstanceSyncPerpetualTaskParamsNg;

import software.wings.beans.HostReachabilityInfo;
import software.wings.utils.HostValidationService;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class PdcInstanceSyncPerpetualTaskExecutor implements PerpetualTaskExecutor {
  private static final String SUCCESS_RESPONSE_MSG = "success";

  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;
  @Inject private HostValidationService hostValidationService;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    log.info("Running the Pdc InstanceSync perpetual task executor for task id: {}", taskId);
    PdcInstanceSyncPerpetualTaskParamsNg taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), PdcInstanceSyncPerpetualTaskParamsNg.class);
    return executeTask(taskId, taskParams);
  }

  private PerpetualTaskResponse executeTask(PerpetualTaskId taskId, PdcInstanceSyncPerpetualTaskParamsNg taskParams) {
    List<ServerInstanceInfo> serverInstanceInfos =
        getServerInstanceInfoList(taskParams.getHostsList(), taskParams.getPort(), taskParams.getServiceType());

    log.info("Pdc Instance sync nInstances: {}, task id: {}",
        isEmpty(serverInstanceInfos) ? 0 : serverInstanceInfos.size(), taskId);

    String instanceSyncResponseMsg =
        publishInstanceSyncResult(taskId, taskParams.getAccountId(), serverInstanceInfos, taskParams.getServiceType());
    return PerpetualTaskResponse.builder().responseCode(SC_OK).responseMessage(instanceSyncResponseMsg).build();
  }

  private List<ServerInstanceInfo> getServerInstanceInfoList(List<String> hosts, int port, String serviceType) {
    try {
      List<HostReachabilityInfo> hostReachabilityInfos = hostValidationService.validateReachability(hosts, port);
      return hostReachabilityInfos.stream()
          .filter(hr -> Boolean.TRUE.equals(hr.getReachable()))
          .map(o -> mapToPdcServerInstanceInfo(serviceType, o))
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.warn("Unable to get list of server instances, hosts: {}, port: {}", hosts, port, e);
      return Collections.emptyList();
    }
  }

  private String publishInstanceSyncResult(
      PerpetualTaskId taskId, String accountId, List<ServerInstanceInfo> serverInstanceInfos, String serviceType) {
    PdcInstanceSyncPerpetualTaskResponse.PdcInstanceSyncPerpetualTaskResponseBuilder builder =
        ServiceSpecType.SSH.equals(serviceType) ? PdcSshInstanceSyncPerpetualTaskResponse.builder()
                                                : PdcWinrmInstanceSyncPerpetualTaskResponse.builder();

    InstanceSyncPerpetualTaskResponse instanceSyncResponse = builder.serverInstanceDetails(serverInstanceInfos)
                                                                 .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                                 .build();

    try {
      execute(delegateAgentManagerClient.processInstanceSyncNGResult(taskId.getId(), accountId, instanceSyncResponse));
    } catch (Exception e) {
      String errorMsg = format(
          "Failed to publish Pdc instance sync result PerpetualTaskId [%s], accountId [%s]", taskId.getId(), accountId);
      log.error(errorMsg + ", serverInstanceInfos: {}", serverInstanceInfos, e);
      return errorMsg;
    }
    return SUCCESS_RESPONSE_MSG;
  }

  private ServerInstanceInfo mapToPdcServerInstanceInfo(String serviceType, HostReachabilityInfo hostReachabilityInfo) {
    return PdcServerInstanceInfo.builder().serviceType(serviceType).host(hostReachabilityInfo.getHostName()).build();
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
