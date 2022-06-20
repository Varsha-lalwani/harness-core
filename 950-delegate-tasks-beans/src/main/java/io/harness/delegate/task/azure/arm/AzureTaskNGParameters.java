package io.harness.delegate.task.azure.arm;

import io.harness.delegate.beans.connector.azureconnector.AzureCapabilityHelper;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionReflectionUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AzureTaskNGParameters implements TaskParameters, ExecutionCapabilityDemander, ExpressionReflectionUtils.NestedAnnotationResolver{
    @NonNull String accountId;
    @NonNull AzureARMTaskType azureARMTaskType;
    @NonNull AzureConnectorDTO azureConnectorDTO;
    long timeoutInMs;
    @Override
    public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
        return AzureCapabilityHelper.fetchRequiredExecutionCapabilities(azureConnectorDTO, maskingEvaluator);
    }
}
