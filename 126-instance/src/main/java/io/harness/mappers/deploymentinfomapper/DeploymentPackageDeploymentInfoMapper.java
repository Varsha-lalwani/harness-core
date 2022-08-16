package io.harness.mappers.deploymentinfomapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.DeploymentPackageDeploymentInfoDTO;
import io.harness.entities.deploymentinfo.DeploymentPackageDeploymentInfo;
import io.harness.entities.deploymentinfo.K8sDeploymentInfo;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class DeploymentPackageDeploymentInfoMapper {
    public DeploymentPackageDeploymentInfoDTO toDTO(DeploymentPackageDeploymentInfo deploymentPackageDeploymentInfo) {
        return DeploymentPackageDeploymentInfoDTO.builder()
                .instanceFetchScript(deploymentPackageDeploymentInfo.getInstanceFetchScript())
                .instanceFetchScriptHash(deploymentPackageDeploymentInfo.getInstanceFetchScriptHash())
                .artifactBuildNum(deploymentPackageDeploymentInfo.getArtifactBuildNum())
                .artifactName(deploymentPackageDeploymentInfo.getArtifactName())
                .artifactSourceName(deploymentPackageDeploymentInfo.getArtifactSourceName())
                .artifactStreamId(deploymentPackageDeploymentInfo.getArtifactStreamId())
                .scriptOutput(deploymentPackageDeploymentInfo.getScriptOutput())
                .tags(deploymentPackageDeploymentInfo.getTags())
                .build();
    }

    public DeploymentPackageDeploymentInfo toEntity(DeploymentPackageDeploymentInfoDTO deploymentPackageDeploymentInfoDTO) {
        return DeploymentPackageDeploymentInfo.builder()
                .instanceFetchScript(deploymentPackageDeploymentInfoDTO.getInstanceFetchScript())
                .artifactBuildNum(deploymentPackageDeploymentInfoDTO.getArtifactBuildNum())
                .artifactName(deploymentPackageDeploymentInfoDTO.getArtifactName())
                .artifactSourceName(deploymentPackageDeploymentInfoDTO.getArtifactSourceName())
                .artifactStreamId(deploymentPackageDeploymentInfoDTO.getArtifactStreamId())
                .instanceFetchScriptHash(deploymentPackageDeploymentInfoDTO.getInstanceFetchScriptHash())
                .scriptOutput(deploymentPackageDeploymentInfoDTO.getScriptOutput())
                .tags(deploymentPackageDeploymentInfoDTO.getTags())
                .build();
    }
}
