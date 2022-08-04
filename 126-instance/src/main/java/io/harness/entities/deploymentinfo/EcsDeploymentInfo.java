package io.harness.entities.deploymentinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
public class EcsDeploymentInfo extends DeploymentInfo {
    @NotNull private String region;
    @NotNull private String clusterArn;
    @NotNull private String serviceName;
    private String launchType;
    @NotNull private String infraStructureKey;
}
