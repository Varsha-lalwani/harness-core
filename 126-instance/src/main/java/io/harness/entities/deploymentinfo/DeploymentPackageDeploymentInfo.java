package io.harness.entities.deploymentinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.codehaus.commons.nullanalysis.NotNull;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.DX)
public class DeploymentPackageDeploymentInfo  extends DeploymentInfo {
    @NotNull
    private String instanceFetchScriptHash;
    private String instanceFetchScript;
    private String scriptOutput;
    private List<String> tags;
    private String artifactName;
    private String artifactSourceName;
    private String artifactStreamId;
    private String artifactBuildNum;
}
