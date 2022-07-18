/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.mappers.deploymentinfomapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.*;
import io.harness.entities.deploymentinfo.*;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class DeploymentInfoMapper {
  public DeploymentInfoDTO toDTO(DeploymentInfo deploymentInfo) {
    if (deploymentInfo instanceof ReferenceK8sPodInfo) {
      return ReferenceK8sPodInfoMapper.toDTO((ReferenceK8sPodInfo) deploymentInfo);
    } else if (deploymentInfo instanceof K8sDeploymentInfo) {
      return K8sDeploymentInfoMapper.toDTO((K8sDeploymentInfo) deploymentInfo);
    } else if (deploymentInfo instanceof NativeHelmDeploymentInfo) {
      return NativeHelmDeploymentInfoMapper.toDTO((NativeHelmDeploymentInfo) deploymentInfo);
    } else if (deploymentInfo instanceof ServerlessAwsLambdaDeploymentInfo) {
      return ServerlessAwsLambdaDeploymentInfoMapper.toDTO((ServerlessAwsLambdaDeploymentInfo) deploymentInfo);
    } else if (deploymentInfo instanceof PdcSshDeploymentInfo) {
      return PdcDeploymentInfoMapper.toDTO((PdcSshDeploymentInfo) deploymentInfo);
    }
    throw new InvalidRequestException("No DeploymentInfoMapper toDTO found for deploymentInfo : {}" + deploymentInfo);
  }

  public DeploymentInfo toEntity(DeploymentInfoDTO deploymentInfoDTO) {
    if (deploymentInfoDTO instanceof ReferenceK8sPodInfoDTO) {
      return ReferenceK8sPodInfoMapper.toEntity((ReferenceK8sPodInfoDTO) deploymentInfoDTO);
    } else if (deploymentInfoDTO instanceof K8sDeploymentInfoDTO) {
      return K8sDeploymentInfoMapper.toEntity((K8sDeploymentInfoDTO) deploymentInfoDTO);
    } else if (deploymentInfoDTO instanceof NativeHelmDeploymentInfoDTO) {
      return NativeHelmDeploymentInfoMapper.toEntity((NativeHelmDeploymentInfoDTO) deploymentInfoDTO);
    } else if (deploymentInfoDTO instanceof ServerlessAwsLambdaDeploymentInfoDTO) {
      return ServerlessAwsLambdaDeploymentInfoMapper.toEntity((ServerlessAwsLambdaDeploymentInfoDTO) deploymentInfoDTO);
    } else if (deploymentInfoDTO instanceof PdcSshDeploymentInfoDTO) {
      return PdcDeploymentInfoMapper.toEntity((PdcSshDeploymentInfoDTO) deploymentInfoDTO);
    }
    throw new InvalidRequestException(
        "No DeploymentInfoMapper toEntity found for deploymentInfo : {}" + deploymentInfoDTO);
  }
}
