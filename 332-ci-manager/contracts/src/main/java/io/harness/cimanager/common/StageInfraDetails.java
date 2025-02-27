/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.sweepingoutputs;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "stageInfraDetails")
@HarnessEntity(exportable = true)
@TypeAlias("StageInfraDetails")
@JsonTypeName("stageInfraDetails")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.sweepingoutputs.StageInfraDetails")
public interface StageInfraDetails extends ExecutionSweepingOutput {
  enum Type { K8, VM, DLITE_VM }

  StageInfraDetails.Type getType();
  String STAGE_INFRA_DETAILS = "stageInfraDetails";
}
