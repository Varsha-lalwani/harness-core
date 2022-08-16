package io.harness.cdng.infra.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.steps.environment.EnvironmentOutcome;
import io.harness.yaml.core.VariableExpression;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

import java.util.Map;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
@JsonTypeName(InfrastructureKind.DEPLOYMENT_PACKAGE)
@TypeAlias("cdng.infra.beans.CustomDeploymentInfrastructureOutcome")
@RecasterAlias("io.harness.cdng.infra.beans.CustomDeploymentInfrastructureOutcome")
public class DeploymentPackageInfrastructureOutcome extends InfrastructureDetailsAbstract implements InfrastructureOutcome{
    String instancesListPath ;
    Map<String, String> instanceAttributes;
    String instanceFetchScript;
    String connectorRef;
    @VariableExpression(skipVariableExpression = true)
    EnvironmentOutcome environment;
    String infrastructureKey;
    @Override
    public String getKind() {
        return InfrastructureKind.DEPLOYMENT_PACKAGE;
    }

}
