/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
@Builder
@JsonTypeName(ArtifactSourceConstants.AMAZON_S3_NAME)
@RecasterAlias("io.harness.ngpipeline.pipeline.executions.beans.S3ArtifactSummary")
public class S3ArtifactSummary implements ArtifactSummary {
  String bucketName;
  String filePath;

  @Override
  public String getDisplayName() {
    return bucketName + ":" + filePath;
  }

  @Override
  public String getType() {
    return ArtifactSourceConstants.AMAZON_S3_NAME;
  }
}
