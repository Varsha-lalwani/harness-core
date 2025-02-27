/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.dtos.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.EqualsAndHashCode;

@OwnedBy(HarnessTeam.CDP)
@EqualsAndHashCode(callSuper = true)
public class AzureSshWinrmInstanceInfoDTO extends SshWinrmInstanceInfoDTO {
  @Builder
  public AzureSshWinrmInstanceInfoDTO(String serviceType, String infrastructureKey, String host) {
    super(serviceType, infrastructureKey, host);
  }

  @Override
  public String getType() {
    return "AzureSshWinrm";
  }
}
