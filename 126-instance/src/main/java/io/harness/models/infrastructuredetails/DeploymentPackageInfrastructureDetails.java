package io.harness.models.infrastructuredetails;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
public class DeploymentPackageInfrastructureDetails extends InfrastructureDetails {
    private String hostname;
}
