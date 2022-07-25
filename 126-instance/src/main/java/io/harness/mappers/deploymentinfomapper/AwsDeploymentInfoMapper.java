/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.mappers.deploymentinfomapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.AwsDeploymentInfoDTO;
import io.harness.entities.deploymentinfo.AwsDeploymentInfo;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class AwsDeploymentInfoMapper {
  public AwsDeploymentInfoDTO toDTO(AwsDeploymentInfo awsDeploymentInfo) {
    return AwsDeploymentInfoDTO.builder()
        .serviceType(awsDeploymentInfo.getServiceType())
        .infraIdentifier(awsDeploymentInfo.getInfraIdentifier())
        .hosts(awsDeploymentInfo.getHosts())
        .build();
  }

  public AwsDeploymentInfo toEntity(AwsDeploymentInfoDTO awsDeploymentInfoDTO) {
    return AwsDeploymentInfo.builder()
        .serviceType(awsDeploymentInfoDTO.getServiceType())
        .infraIdentifier(awsDeploymentInfoDTO.getInfraIdentifier())
        .hosts(awsDeploymentInfoDTO.getHosts())
        .build();
  }
}
