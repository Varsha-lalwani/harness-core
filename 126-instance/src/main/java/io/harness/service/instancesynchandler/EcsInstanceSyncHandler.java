package io.harness.service.instancesynchandler;

import io.harness.cdng.infra.beans.EcsInfrastructureOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.ServerlessAwsLambdaInfrastructureOutcome;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.EcsServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.ServerlessAwsLambdaServerInstanceInfo;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.EcsDeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.ServerlessAwsLambdaDeploymentInfoDTO;
import io.harness.dtos.instanceinfo.EcsInstanceInfoDTO;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.dtos.instanceinfo.ServerlessAwsLambdaInstanceInfoDTO;
import io.harness.entities.InstanceType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.models.infrastructuredetails.EcsInfrastructureDetails;
import io.harness.models.infrastructuredetails.InfrastructureDetails;
import io.harness.models.infrastructuredetails.ServerlessAwsLambdaInfrastructureDetails;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.perpetualtask.PerpetualTaskType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

public class EcsInstanceSyncHandler extends AbstractInstanceSyncHandler{
    @Override
    public String getPerpetualTaskType() {
        return PerpetualTaskType.ECS_INSTANCE_SYNC;
    }

    @Override
    public InstanceType getInstanceType() {
        return InstanceType.ECS_INSTANCE;
    }

    @Override
    public String getInfrastructureKind() {
        return InfrastructureKind.ECS;
    }

    @Override
    public InfrastructureDetails getInfrastructureDetails(InstanceInfoDTO instanceInfoDTO) {
        if (!(instanceInfoDTO instanceof EcsInstanceInfoDTO)) {
            throw new InvalidArgumentsException(
                    Pair.of("instanceInfoDTO", "Must be instance of EcsInstanceInfoDTO"));
        }
        EcsInstanceInfoDTO ecsInstanceInfoDTO =
                (EcsInstanceInfoDTO) instanceInfoDTO;
        return EcsInfrastructureDetails.builder()
                .region(ecsInstanceInfoDTO.getRegion())
                .clusterArn(ecsInstanceInfoDTO.getClusterArn())
                .build();
    }

    @Override
    public DeploymentInfoDTO getDeploymentInfo(InfrastructureOutcome infrastructureOutcome, List<ServerInstanceInfo> serverInstanceInfoList) {
        if (isEmpty(serverInstanceInfoList)) {
            throw new InvalidArgumentsException("Parameter serverInstanceInfoList cannot be null or empty");
        }
        if (!(infrastructureOutcome instanceof EcsInfrastructureOutcome)) {
            throw new InvalidArgumentsException(
                    Pair.of("infrastructureOutcome", "Must be instance of EcsInfrastructureOutcome"));
        }
        if (!(serverInstanceInfoList.get(0) instanceof EcsServerInstanceInfo)) {
            throw new InvalidArgumentsException(
                    Pair.of("serverInstanceInfo", "Must be instance of EcsServerInstanceInfo"));
        }

        EcsServerInstanceInfo ecsServerInstanceInfo =
                (EcsServerInstanceInfo) serverInstanceInfoList.get(0);

        return EcsDeploymentInfoDTO.builder()
                .serviceName(ecsServerInstanceInfo.getServiceName())
                .region(ecsServerInstanceInfo.getRegion())
                .clusterArn(ecsServerInstanceInfo.getClusterArn())
                .infraStructureKey(ecsServerInstanceInfo.getInfraStructureKey()).build();
    }

    @Override
    protected InstanceInfoDTO getInstanceInfoForServerInstance(ServerInstanceInfo serverInstanceInfo) {
        if (!(serverInstanceInfo instanceof EcsServerInstanceInfo)) {
            throw new InvalidArgumentsException(
                    Pair.of("serverInstanceInfo", "Must be instance of EcsServerInstanceInfo"));
        }

        EcsServerInstanceInfo ecsServerInstanceInfo =
                (EcsServerInstanceInfo) serverInstanceInfo;

        return EcsInstanceInfoDTO.builder()
                .serviceName(ecsServerInstanceInfo.getServiceName())
                .clusterArn(ecsServerInstanceInfo.getClusterArn())
                .launchType(ecsServerInstanceInfo.getLaunchType())
                .region(ecsServerInstanceInfo.getRegion())
                .taskArn(ecsServerInstanceInfo.getTaskArn())
                .taskDefinitionArn(ecsServerInstanceInfo.getTaskDefinitionArn())
                .containers(ecsServerInstanceInfo.getContainers())
                .startedBy(ecsServerInstanceInfo.getStartedBy())
                .startedAt(ecsServerInstanceInfo.getStartedAt())
                .version(ecsServerInstanceInfo.getVersion())
                .infraStructureKey(ecsServerInstanceInfo.getInfraStructureKey())
                .build();
    }
}
