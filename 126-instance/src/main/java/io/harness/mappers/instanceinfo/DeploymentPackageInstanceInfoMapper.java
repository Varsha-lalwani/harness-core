package io.harness.mappers.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instanceinfo.DeploymentPackageInstanceInfoDTO;
import io.harness.entities.instanceinfo.DeploymentPackageInstanceInfo;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class DeploymentPackageInstanceInfoMapper {

    public DeploymentPackageInstanceInfoDTO toDTO(DeploymentPackageInstanceInfo deploymentPackageInstanceInfo) {
        return DeploymentPackageInstanceInfoDTO.builder()
                .instanceFetchScript(deploymentPackageInstanceInfo.getInstanceFetchScript())
                .hostname(deploymentPackageInstanceInfo.getHostname())
                .properties(deploymentPackageInstanceInfo.getProperties())
                .build();
    }
    public DeploymentPackageInstanceInfo toEntity(DeploymentPackageInstanceInfoDTO deploymentPackageInstanceInfoDTO) {
        return DeploymentPackageInstanceInfo.builder()
                .instanceFetchScript(deploymentPackageInstanceInfoDTO.getInstanceFetchScript())
                .hostname(deploymentPackageInstanceInfoDTO.getHostname())
                .properties(deploymentPackageInstanceInfoDTO.getProperties())
                .build();
    }


}
