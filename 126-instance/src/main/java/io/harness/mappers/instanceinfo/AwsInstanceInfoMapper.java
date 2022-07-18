/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.mappers.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instanceinfo.AwsInstanceInfoDTO;
import io.harness.entities.instanceinfo.AwsInstanceInfo;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class AwsInstanceInfoMapper {
  public AwsInstanceInfoDTO toDTO(AwsInstanceInfo instanceInfo) {
    return AwsInstanceInfoDTO.builder().host(instanceInfo.getHost()).build();
  }

  public AwsInstanceInfo toEntity(AwsInstanceInfoDTO instanceInfoDTO) {
    return AwsInstanceInfo.builder().host(instanceInfoDTO.getHost()).build();
  }
}
