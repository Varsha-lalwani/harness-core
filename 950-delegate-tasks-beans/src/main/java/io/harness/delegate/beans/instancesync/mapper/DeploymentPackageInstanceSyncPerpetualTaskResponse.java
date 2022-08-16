package io.harness.delegate.beans.instancesync.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.k8s.ServiceSpecType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class DeploymentPackageInstanceSyncPerpetualTaskResponse implements InstanceSyncPerpetualTaskResponse {
    private DelegateMetaInfo delegateMetaInfo;
    private List<ServerInstanceInfo> serverInstanceDetails;
    private String errorMessage;
    private CommandExecutionStatus commandExecutionStatus;

    @Override
    public String getDeploymentType() {
        return ServiceSpecType.DEPLOYMENT_PACKAGE;
    }
}
