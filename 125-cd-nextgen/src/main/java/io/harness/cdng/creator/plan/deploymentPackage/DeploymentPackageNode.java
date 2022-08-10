/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.deploymentPackage;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.deploymentPackage.AbstractDeploymentPackageNode;
import io.harness.plancreator.deploymentPackage.DeploymentPackageInfoConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.DEPLOYMENT_TYPE_DEPLOYMENT_PACKAGE)
@TypeAlias("DeploymentPackageNode")
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.creator.plan.stage.DeploymentPackageNode")
public class DeploymentPackageNode extends AbstractDeploymentPackageNode {
  @JsonProperty("type") @NotNull DeploymentPackageType type = DeploymentPackageType.Deployment;

  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  DeploymentPackageConfig deploymentPackageConfig;
  @Override
  public String getType() {
    return StepSpecTypeConstants.DEPLOYMENT_TYPE_DEPLOYMENT_PACKAGE;
  }

  @Override
  public DeploymentPackageInfoConfig getDeploymentPackageInfoConfig() {
    return deploymentPackageConfig;
  }

  public enum DeploymentPackageType {
    Deployment(StepSpecTypeConstants.DEPLOYMENT_TYPE_DEPLOYMENT_PACKAGE);
    @Getter String name;
    DeploymentPackageType(String name) {
      this.name = name;
    }
  }
}
