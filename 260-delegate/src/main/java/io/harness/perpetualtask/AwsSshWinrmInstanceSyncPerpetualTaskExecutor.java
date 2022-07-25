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
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.SshWinrmInstanceSyncPerpetualTaskResponse;
import io.harness.delegate.beans.instancesync.info.AwsServerInstanceInfo;
import io.harness.delegate.task.aws.AwsASGDelegateTaskHelper;
import io.harness.delegate.task.aws.AwsListEC2InstancesDelegateTaskHelper;
import io.harness.delegate.task.ssh.AwsSshWinrmInfraDelegateConfig;
import io.harness.exception.InvalidArgumentsException;
import io.harness.grpc.utils.AnyUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.perpetualtask.instancesync.AwsSshInstanceSyncPerpetualTaskParamsNg;
import io.harness.serializer.KryoSerializer;

import software.wings.service.impl.aws.model.AwsEC2Instance;
import software.wings.utils.HostValidationService;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class AwsSshWinrmInstanceSyncPerpetualTaskExecutor implements PerpetualTaskExecutor {
  private static final String SUCCESS_RESPONSE_MSG = "success";

  private static final Set<String> VALID_SERVICE_TYPES =
      Collections.unmodifiableSet(new HashSet(Arrays.asList(ServiceSpecType.SSH, ServiceSpecType.WINRM)));

  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;
  @Inject private HostValidationService hostValidationService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private AwsListEC2InstancesDelegateTaskHelper awsListEC2InstancesDelegateTaskHelper;
  @Inject private AwsASGDelegateTaskHelper awsASGDelegateTaskHelper;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    log.info("Running the Aws InstanceSync perpetual task executor for task id: {}", taskId);
    AwsSshInstanceSyncPerpetualTaskParamsNg taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), AwsSshInstanceSyncPerpetualTaskParamsNg.class);

    if (!VALID_SERVICE_TYPES.contains(taskParams.getServiceType())) {
      throw new InvalidArgumentsException(
          format("Invalid serviceType provided %s . Expected: %s", taskParams.getServiceType(), VALID_SERVICE_TYPES));
    }

    return executeTask(taskId, taskParams);
  }

  private PerpetualTaskResponse executeTask(
      PerpetualTaskId taskId, AwsSshInstanceSyncPerpetualTaskParamsNg taskParams) {
    List<AwsEC2Instance> awsEC2Instances = getAwsEC2Instance(taskParams);
    List<String> awsHosts = awsEC2Instances.stream().map(AwsEC2Instance::getPublicDnsName).collect(Collectors.toList());
    List<String> instanceHosts =
        taskParams.getDeployedHostsList().stream().filter(awsHosts::contains).collect(Collectors.toList());

    List<ServerInstanceInfo> serverInstanceInfos =
        getServerInstanceInfoList(instanceHosts, taskParams.getServiceType());

    log.info("Aws Instance sync Instances: {}, task id: {}",
        isEmpty(serverInstanceInfos) ? 0 : serverInstanceInfos.size(), taskId);

    String instanceSyncResponseMsg =
        publishInstanceSyncResult(taskId, taskParams.getAccountId(), serverInstanceInfos, taskParams.getServiceType());
    return PerpetualTaskResponse.builder().responseCode(SC_OK).responseMessage(instanceSyncResponseMsg).build();
  }

  private List<AwsEC2Instance> getAwsEC2Instance(AwsSshInstanceSyncPerpetualTaskParamsNg taskParams) {
    AwsSshWinrmInfraDelegateConfig infraConfig = (AwsSshWinrmInfraDelegateConfig) kryoSerializer.asObject(
        taskParams.getAwsSshWinrmInfraDelegateConfig().toByteArray());

    if (StringUtils.isNotEmpty(infraConfig.getAutoScalingGroupName())) { // ASG
      return awsASGDelegateTaskHelper.getInstances(infraConfig.getAwsConnectorDTO(),
          infraConfig.getEncryptionDataDetails(), infraConfig.getRegion(), infraConfig.getAutoScalingGroupName());
    } else {
      boolean isWinRm = ServiceSpecType.WINRM.equals(taskParams.getServiceType());
      return awsListEC2InstancesDelegateTaskHelper.getInstances(infraConfig.getAwsConnectorDTO(),
          infraConfig.getEncryptionDataDetails(), infraConfig.getRegion(), infraConfig.getVpcIds(),
          infraConfig.getTags(), isWinRm);
    }
  }

  private List<ServerInstanceInfo> getServerInstanceInfoList(List<String> hosts, String serviceType) {
    return hosts.stream().map(h -> mapToAwsServerInstanceInfo(serviceType, h, hosts)).collect(Collectors.toList());
  }

  private String publishInstanceSyncResult(
      PerpetualTaskId taskId, String accountId, List<ServerInstanceInfo> serverInstanceInfos, String serviceType) {
    InstanceSyncPerpetualTaskResponse instanceSyncResponse = SshWinrmInstanceSyncPerpetualTaskResponse.builder()
                                                                 .serviceType(serviceType)
                                                                 .serverInstanceDetails(serverInstanceInfos)
                                                                 .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                                 .build();

    try {
      execute(delegateAgentManagerClient.processInstanceSyncNGResult(taskId.getId(), accountId, instanceSyncResponse));
    } catch (Exception e) {
      String errorMsg = format(
          "Failed to publish Aws instance sync result PerpetualTaskId [%s], accountId [%s]", taskId.getId(), accountId);
      log.error(errorMsg + ", serverInstanceInfos: {}", serverInstanceInfos, e);
      return errorMsg;
    }
    return SUCCESS_RESPONSE_MSG;
  }

  private ServerInstanceInfo mapToAwsServerInstanceInfo(String serviceType, String host, List<String> deployedHosts) {
    return AwsServerInstanceInfo.builder().serviceType(serviceType).host(host).deployedHosts(deployedHosts).build();
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
