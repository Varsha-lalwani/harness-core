package io.harness.cdng.provision.azure.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;

@Data
@Builder
@RecasterAlias("io.harness.cdng.provision.azure.beans.CreatePassThroughData")
public class CreatePassThroughData implements PassThroughData {
    String templateBody;
    String templateUrl;
    @Builder.Default
    LinkedHashMap<String, List<String>> parametersFilesContent = new LinkedHashMap<>();
}
