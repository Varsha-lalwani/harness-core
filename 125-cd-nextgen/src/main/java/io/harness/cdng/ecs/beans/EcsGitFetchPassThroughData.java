package io.harness.cdng.ecs.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.expression.Expression;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;

import static io.harness.expression.Expression.ALLOW_SECRETS;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
@TypeAlias("ecsGitFetchPassThroughData")
@RecasterAlias("io.harness.cdng.ecs.beans.EcsGitFetchPassThroughData")
public class EcsGitFetchPassThroughData implements PassThroughData {
  InfrastructureOutcome infrastructureOutcome;
}
