package io.harness.entities.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Map;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DeploymentPackageInstanceInfo extends InstanceInfo{
    @NotNull
    private String hostname;
    @NotNull private String instanceFetchScript;
    private Map<String, Object> properties;

}
