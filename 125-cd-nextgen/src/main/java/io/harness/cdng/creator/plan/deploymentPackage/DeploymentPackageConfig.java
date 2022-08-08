/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.deploymentPackage;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.cdng.visitor.helpers.deploymentPackage.DeploymentPackageVisitorHelper;
import io.harness.plancreator.deploymentPackage.DeploymentPackageInfoConfig;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.beans.VisitableChild;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.VariableExpression;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName("Deploy")
@TypeAlias("deploymentPackageConfig")
@SimpleVisitorHelper(helperClass = DeploymentPackageVisitorHelper.class)
public class DeploymentPackageConfig implements DeploymentPackageInfoConfig, Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;

  ServiceDefinitionType deploymentType;
  Boolean gitOpsEnabled;

  // New Environment Yaml
  // skipping variable creation from framework since these are supported through outcomes
  //  @VariableExpression(skipVariableExpression = true) EnvironmentYamlV2 environment;
  //
  //  // Environment Group yaml
  //  // todo: add expressions from env group outcomes
  //  @VariableExpression(skipVariableExpression = true) EnvironmentGroupYaml environmentGroup;

  PipelineInfrastructure infrastructure;
  @NotNull @VariableExpression(skipVariableExpression = true) ExecutionElementConfig execution;

  // For Visitor Framework Impl
  //  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    List<VisitableChild> children = new ArrayList<>();
    //    children.add(VisitableChild.builder().value(infrastructure).fieldName("infrastructure").build());
    //    if (environment != null) {
    //      children.add(VisitableChild.builder().value(environment).fieldName("environment").build());
    //    }
    //    if (environmentGroup != null) {
    //      children.add(VisitableChild.builder().value(environmentGroup).fieldName("environmentGroup").build());
    //    }
    return VisitableChildren.builder().visitableChildList(children).build();
  }

  public boolean getGitOpsEnabled() {
    return gitOpsEnabled == Boolean.TRUE;
  }
}
