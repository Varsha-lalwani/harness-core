package io.harness.mappers.deploymentinfomapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.deploymentinfo.EcsDeploymentInfoDTO;
import io.harness.entities.deploymentinfo.EcsDeploymentInfo;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class EcsDeploymentInfoMapper {

    public EcsDeploymentInfoDTO toDTO(EcsDeploymentInfo ecsDeploymentInfo) {
        return EcsDeploymentInfoDTO.builder()
                .serviceName(ecsDeploymentInfo.getServiceName())
                .clusterArn(ecsDeploymentInfo.getClusterArn())
                .launchType(ecsDeploymentInfo.getLaunchType())
                .region(ecsDeploymentInfo.getRegion())
                .infraStructureKey(ecsDeploymentInfo.getInfraStructureKey())
                .build();
    }

    public EcsDeploymentInfo toEntity(EcsDeploymentInfoDTO ecsDeploymentInfoDTO) {
        return EcsDeploymentInfo.builder()
                .serviceName(ecsDeploymentInfoDTO.getServiceName())
                .clusterArn(ecsDeploymentInfoDTO.getClusterArn())
                .launchType(ecsDeploymentInfoDTO.getLaunchType())
                .region(ecsDeploymentInfoDTO.getRegion())
                .infraStructureKey(ecsDeploymentInfoDTO.getInfraStructureKey())
                .build();
    }
}
