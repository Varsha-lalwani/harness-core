/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.mappers.deploymentinfomapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.PdcDeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.PdcSshDeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.PdcWinrmDeploymentInfoDTO;
import io.harness.entities.deploymentinfo.PdcDeploymentInfo;
import io.harness.entities.deploymentinfo.PdcSshDeploymentInfo;
import io.harness.entities.deploymentinfo.PdcWinrmDeploymentInfo;
import io.harness.ng.core.k8s.ServiceSpecType;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class PdcDeploymentInfoMapper {
  public PdcSshDeploymentInfoDTO toDTO(PdcSshDeploymentInfo pdcDeploymentInfo) {
    //    PdcDeploymentInfoDTO.PdcDeploymentInfoDTOBuilder builder = pdcDeploymentInfo instanceof PdcSshDeploymentInfo
    //        ? PdcSshDeploymentInfoDTO.builder()
    //        : PdcWinrmDeploymentInfoDTO.builder();
    //
    //    return
    //    builder.infraIdentifier(pdcDeploymentInfo.getInfraIdentifier()).hosts(pdcDeploymentInfo.getHosts()).build();

    return PdcSshDeploymentInfoDTO.builder()
        .infraIdentifier(pdcDeploymentInfo.getInfraIdentifier())
        .hosts(pdcDeploymentInfo.getHosts())
        .build();
  }

  public PdcSshDeploymentInfo toEntity(PdcSshDeploymentInfoDTO pdcDeploymentInfoDTO) {
    //    PdcDeploymentInfo.PdcDeploymentInfoBuilder builder =
    //    ServiceSpecType.SSH.equals(pdcDeploymentInfoDTO.getType())
    //        ? PdcSshDeploymentInfo.builder()
    //        : PdcWinrmDeploymentInfo.builder();
    //
    //    return builder.infraIdentifier(pdcDeploymentInfoDTO.getInfraIdentifier())
    //        .hosts(pdcDeploymentInfoDTO.getHosts())
    //        .build();

    return PdcSshDeploymentInfo.builder()
        .infraIdentifier(pdcDeploymentInfoDTO.getInfraIdentifier())
        .hosts(pdcDeploymentInfoDTO.getHosts())
        .build();
  }
}
