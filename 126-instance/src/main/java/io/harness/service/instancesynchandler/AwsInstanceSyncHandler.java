/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.instancesynchandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.SshWinRmAwsInfrastructureOutcome;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.AwsServerInstanceInfo;
import io.harness.dtos.deploymentinfo.AwsDeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.instanceinfo.AwsInstanceInfoDTO;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.entities.InstanceType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.models.infrastructuredetails.AwsInfrastructureDetails;
import io.harness.models.infrastructuredetails.InfrastructureDetails;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.perpetualtask.PerpetualTaskType;

import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class AwsInstanceSyncHandler extends AbstractInstanceSyncHandler {
  private static final Set<String> VALID_SERVICE_TYPES =
      Collections.unmodifiableSet(new HashSet(Arrays.asList(ServiceSpecType.SSH, ServiceSpecType.WINRM)));

  @Override
  public String getPerpetualTaskType() {
    return PerpetualTaskType.AWS_INSTANCE_SYNC_NG;
  }

  @Override
  public InstanceType getInstanceType() {
    return InstanceType.AWS_INSTANCE;
  }

  @Override
  public String getInfrastructureKind() {
    return InfrastructureKind.SSH_WINRM_AWS;
  }

  @Override
  public InfrastructureDetails getInfrastructureDetails(InstanceInfoDTO instanceInfoDTO) {
    if (!(instanceInfoDTO instanceof AwsInstanceInfoDTO)) {
      throw new InvalidArgumentsException(
          Pair.of("instanceInfoDTO", "Must be instance of " + AwsInstanceInfoDTO.class));
    }

    AwsInstanceInfoDTO awsInstanceInfoDTO = (AwsInstanceInfoDTO) instanceInfoDTO;
    return AwsInfrastructureDetails.builder().host(awsInstanceInfoDTO.getHost()).build();
  }

  @Override
  protected InstanceInfoDTO getInstanceInfoForServerInstance(ServerInstanceInfo serverInstanceInfo) {
    if (!(serverInstanceInfo instanceof AwsServerInstanceInfo)) {
      throw new InvalidArgumentsException(
          Pair.of("serverInstanceInfo", "Must be instance of " + AwsServerInstanceInfo.class));
    }

    AwsServerInstanceInfo awsServerInstanceInfo = (AwsServerInstanceInfo) serverInstanceInfo;
    return AwsInstanceInfoDTO.builder()
        .serviceType(awsServerInstanceInfo.getServiceType())
        .host(awsServerInstanceInfo.getHost())
        .deployedHosts(awsServerInstanceInfo.getDeployedHosts())
        .build();
  }

  @Override
  public DeploymentInfoDTO getDeploymentInfo(
      InfrastructureOutcome infrastructureOutcome, List<ServerInstanceInfo> serverInstanceInfoList) {
    if (isEmpty(serverInstanceInfoList)) {
      throw new InvalidArgumentsException("Parameter serverInstanceInfoList cannot be null or empty");
    }
    if (!(infrastructureOutcome instanceof SshWinRmAwsInfrastructureOutcome)) {
      throw new InvalidArgumentsException(
          Pair.of("infrastructureOutcome", "Must be instance of " + SshWinRmAwsInfrastructureOutcome.class));
    }
    if (!(serverInstanceInfoList.get(0) instanceof AwsServerInstanceInfo)) {
      throw new InvalidArgumentsException(
          Pair.of("serverInstanceInfo", "Must be instance of " + AwsServerInstanceInfo.class));
    }

    List<AwsServerInstanceInfo> awsServerInstanceInfos = (List<AwsServerInstanceInfo>) (List<?>) serverInstanceInfoList;
    AwsServerInstanceInfo awsServerInstanceInfo = awsServerInstanceInfos.get(0);
    List<String> hosts = awsServerInstanceInfo.getDeployedHosts();

    if (!VALID_SERVICE_TYPES.contains(awsServerInstanceInfo.getServiceType())) {
      throw new InvalidArgumentsException(format("Invalid serviceType provided %s . Must be one of %s",
          awsServerInstanceInfo.getServiceType(), VALID_SERVICE_TYPES));
    }

    return AwsDeploymentInfoDTO.builder()
        .serviceType(awsServerInstanceInfo.getServiceType())
        .infraIdentifier(infrastructureOutcome.getInfrastructureKey())
        .hosts(hosts)
        .build();
  }
}
