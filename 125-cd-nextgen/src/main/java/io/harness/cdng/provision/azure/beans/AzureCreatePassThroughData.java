package io.harness.cdng.provision.azure.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@RecasterAlias("io.harness.cdng.provision.azure.beans.CreatePassThroughData")
public class AzureCreatePassThroughData implements PassThroughData {
    String templateBody;
    String parametersBody;
    String blueprintBody;
    String assignBody;
    @Builder.Default
    Map<String, String> artifacts = new HashMap<>();
    @Accessors(fluent = true) boolean hasGitFiles;
}
