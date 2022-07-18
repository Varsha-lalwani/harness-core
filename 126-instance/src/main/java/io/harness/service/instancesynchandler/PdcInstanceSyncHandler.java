/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.instancesynchandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.PdcInfrastructureOutcome;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.PdcServerInstanceInfo;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.PdcDeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.PdcSshDeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.PdcWinrmDeploymentInfoDTO;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.dtos.instanceinfo.PdcInstanceInfoDTO;
import io.harness.entities.InstanceType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.models.infrastructuredetails.InfrastructureDetails;
import io.harness.models.infrastructuredetails.PdcInfrastructureDetails;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.perpetualtask.PerpetualTaskType;

import com.google.inject.Singleton;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class PdcInstanceSyncHandler extends AbstractInstanceSyncHandler {
  @Override
  public String getPerpetualTaskType() {
    return PerpetualTaskType.PDC_INSTANCE_SYNC_NG;
  }

  @Override
  public InstanceType getInstanceType() {
    return InstanceType.PHYSICAL_HOST_INSTANCE;
  }

  @Override
  public String getInfrastructureKind() {
    return InfrastructureKind.PDC;
  }

  @Override
  public InfrastructureDetails getInfrastructureDetails(InstanceInfoDTO instanceInfoDTO) {
    if (!(instanceInfoDTO instanceof PdcInstanceInfoDTO)) {
      throw new InvalidArgumentsException(
          Pair.of("instanceInfoDTO", "Must be instance of " + PdcInstanceInfoDTO.class));
    }

    PdcInstanceInfoDTO pdcInstanceInfoDTO = (PdcInstanceInfoDTO) instanceInfoDTO;
    return PdcInfrastructureDetails.builder()
        .host(pdcInstanceInfoDTO.getHost())
        .filteredInfraHosts(pdcInstanceInfoDTO.getFilteredInfraHosts())
        .build();
  }

  @Override
  protected InstanceInfoDTO getInstanceInfoForServerInstance(ServerInstanceInfo serverInstanceInfo) {
    if (!(serverInstanceInfo instanceof PdcServerInstanceInfo)) {
      throw new InvalidArgumentsException(
          Pair.of("serverInstanceInfo", "Must be instance of " + PdcServerInstanceInfo.class));
    }

    PdcServerInstanceInfo pdcServerInstanceInfo = (PdcServerInstanceInfo) serverInstanceInfo;
    return PdcInstanceInfoDTO.builder()
        .host(pdcServerInstanceInfo.getHost())
        .filteredInfraHosts(pdcServerInstanceInfo.getFilteredInfraHosts())
        .build();
  }

  @Override
  public DeploymentInfoDTO getDeploymentInfo(
      InfrastructureOutcome infrastructureOutcome, List<ServerInstanceInfo> serverInstanceInfoList) {
    if (isEmpty(serverInstanceInfoList)) {
      throw new InvalidArgumentsException("Parameter serverInstanceInfoList cannot be null or empty");
    }
    if (!(infrastructureOutcome instanceof PdcInfrastructureOutcome)) {
      throw new InvalidArgumentsException(
          Pair.of("infrastructureOutcome", "Must be instance of " + PdcInfrastructureOutcome.class));
    }
    if (!(serverInstanceInfoList.get(0) instanceof PdcServerInstanceInfo)) {
      throw new InvalidArgumentsException(
          Pair.of("serverInstanceInfo", "Must be instance of " + PdcServerInstanceInfo.class));
    }

    List<PdcServerInstanceInfo> pdcServerInstanceInfos = (List<PdcServerInstanceInfo>) (List<?>) serverInstanceInfoList;
    PdcServerInstanceInfo pdcServerInstanceInfo = pdcServerInstanceInfos.get(0);
    List<String> hosts = pdcServerInstanceInfo.getFilteredInfraHosts();

    //    PdcDeploymentInfoDTO.PdcDeploymentInfoDTOBuilder builder =
    //        ServiceSpecType.SSH.equals(pdcServerInstanceInfo.getServiceType()) ? PdcSshDeploymentInfoDTO.builder()
    //                                                                           : PdcWinrmDeploymentInfoDTO.builder();

    return PdcSshDeploymentInfoDTO.builder()
        .infraIdentifier(infrastructureOutcome.getInfrastructureKey())
        .hosts(hosts)
        .build();
  }
}
