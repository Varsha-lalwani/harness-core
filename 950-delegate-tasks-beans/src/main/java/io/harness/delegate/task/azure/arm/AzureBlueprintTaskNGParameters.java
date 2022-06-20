package io.harness.delegate.task.azure.arm;


import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

import static io.harness.expression.Expression.ALLOW_SECRETS;

@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
public class AzureBlueprintTaskNGParameters extends AzureTaskNGParameters {
    @Expression(ALLOW_SECRETS) private String blueprintJson;
    @Expression(ALLOW_SECRETS) private Map<String, String> artifacts;
    @Expression(ALLOW_SECRETS) private String assignmentJson;
    private final String assignmentName;

    @Builder
    public AzureBlueprintTaskNGParameters (String accountId,
                                           AzureARMTaskType taskType,
                                           AzureConnectorDTO connectorDTO,
                                           long timeoutInMs,
                                           String blueprintJson,
                                           Map<String, String> artifacts,
                                           String assignmentJson,
                                           String assignmentName) {
        super(accountId, taskType, connectorDTO, timeoutInMs);
        this.blueprintJson = blueprintJson;
        this.artifacts = artifacts;
        this.assignmentJson = assignmentJson;
        this.assignmentName = assignmentName;
    }
}
