package io.harness.cdng.ecs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CdAbstractStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.yaml.core.StepSpecType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.ECS_CANARY_DEPLOY)
@TypeAlias("ecsCanaryDeployStepNode")
@RecasterAlias("io.harness.cdng.ecs.EcsCanaryDeployStepNode")
public class EcsCanaryDeployStepNode extends CdAbstractStepNode {
  @JsonProperty("type") @NotNull EcsCanaryDeployStepNode.StepType type = StepType.EcsCanaryDeploy;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  EcsCanaryDeployStepInfo ecsCanaryDeployStepInfo;

  @Override
  public String getType() {
    return StepSpecTypeConstants.ECS_CANARY_DEPLOY;
  }

  @Override
  public StepSpecType getStepSpecType() {
    return ecsCanaryDeployStepInfo;
  }

  enum StepType {
    EcsCanaryDeploy(StepSpecTypeConstants.ECS_CANARY_DEPLOY);
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
