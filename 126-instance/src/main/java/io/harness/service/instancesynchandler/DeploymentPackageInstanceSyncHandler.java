package io.harness.service.instancesynchandler;

import io.harness.cdng.infra.beans.DeploymentPackageInfrastructureOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.DeploymentPackageServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.K8sServerInstanceInfo;
import io.harness.dtos.deploymentinfo.DeploymentPackageDeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.instanceinfo.DeploymentPackageInstanceInfoDTO;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.entities.InstanceType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.models.infrastructuredetails.DeploymentPackageInfrastructureDetails;
import io.harness.models.infrastructuredetails.InfrastructureDetails;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.perpetualtask.PerpetualTaskType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

public class DeploymentPackageInstanceSyncHandler extends AbstractInstanceSyncHandler{
    @Override
    public String getPerpetualTaskType() {
        return PerpetualTaskType.DEPLOYMENT_PACKAGE_INSTANCE_SYNC_NG;
    }

    @Override
    public InstanceType getInstanceType() {
        return InstanceType.DEPLOYMENT_PACKAGE_INSTANCE;
    }

    @Override
    public String getInfrastructureKind() {
        return InfrastructureKind.DEPLOYMENT_PACKAGE;
    }

    @Override
    public InfrastructureDetails getInfrastructureDetails(InstanceInfoDTO instanceInfoDTO) {
        if (!(instanceInfoDTO instanceof DeploymentPackageInstanceInfoDTO)) {
            throw new InvalidArgumentsException(Pair.of("instanceInfoDTO", "Must be instance of CustomDeploymentInstanceInfoDTO"));
        }
        DeploymentPackageInstanceInfoDTO deploymentPackageInstanceInfoDTO = (DeploymentPackageInstanceInfoDTO) instanceInfoDTO;
        return DeploymentPackageInfrastructureDetails.builder()
                .hostname(deploymentPackageInstanceInfoDTO.getHostname())
                .build();
    }

    @Override
    protected InstanceInfoDTO getInstanceInfoForServerInstance(ServerInstanceInfo serverInstanceInfo) {
        if (!(serverInstanceInfo instanceof K8sServerInstanceInfo)) {
            throw new InvalidArgumentsException(Pair.of("serverInstanceInfo", "Must be instance of K8sServerInstanceInfo"));
        }

        DeploymentPackageServerInstanceInfo deploymentPackageServerInstanceInfo = (DeploymentPackageServerInstanceInfo) serverInstanceInfo;

        return DeploymentPackageInstanceInfoDTO.builder()
                .hostname(deploymentPackageServerInstanceInfo.getHostName())
                .properties(deploymentPackageServerInstanceInfo.getProperties())
                .build();
    }

    @Override
    public DeploymentInfoDTO getDeploymentInfo(
            InfrastructureOutcome infrastructureOutcome, List<ServerInstanceInfo> serverInstanceInfoList) {
        if (isEmpty(serverInstanceInfoList)) {
            throw new InvalidArgumentsException("Parameter serverInstanceInfoList cannot be null or empty");
        }
        if (!(infrastructureOutcome instanceof DeploymentPackageInfrastructureOutcome)) {
            throw new InvalidArgumentsException(Pair.of("infrastructureOutcome",
                    "Must be instance of CustomDeploymentInfrastructureOutcome"));
        }
        if (!(serverInstanceInfoList.get(0) instanceof DeploymentPackageServerInstanceInfo)) {
            throw new InvalidArgumentsException(Pair.of("serverInstanceInfo", "Must be instance of CustomDeploymentServerInstanceInfo"));
        }

        DeploymentPackageServerInstanceInfo deploymentPackageServerInstanceInfo = (DeploymentPackageServerInstanceInfo) serverInstanceInfoList.get(0);

        return DeploymentPackageDeploymentInfoDTO.builder()
                .instanceFetchScript(deploymentPackageServerInstanceInfo.getInstanceFetchScript())
                .build();
    }

}
