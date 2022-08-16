package io.harness.delegate.beans.instancesync.info;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;
@JsonTypeName("K8sServerInstanceInfo")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
public class DeploymentPackageServerInstanceInfo extends ServerInstanceInfo {
    private String hostId;
    private String hostName;
    private String instanceFetchScript;
    private Map<String, Object> properties;
}
