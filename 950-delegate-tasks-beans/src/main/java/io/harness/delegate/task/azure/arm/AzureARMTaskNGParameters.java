/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure.arm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureDeploymentMode;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.expression.Expression;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
// TODO: Draft
public class AzureARMTaskNGParameters extends AzureTaskNGParameters {
  @Expression(ALLOW_SECRETS) String templateBody;
  @Expression(ALLOW_SECRETS) String parametersBody;
@NonNull List<EncryptedDataDetail> encryptedDataDetails;  ARMScopeType deploymentScope;
  AzureDeploymentMode deploymentMode;
  String managementGroupId;
  String subscriptionId;
  String resourceGroupName;

  String deploymentDataLocation;


    @Builder
    public AzureARMTaskNGParameters(String accountId,
                                    AzureARMTaskType taskType,
                                    AzureConnectorDTO connectorDTO,
                                    String templateBody,
                                    String parametersBody,
                                    ARMScopeType scopeType,
                                    AzureDeploymentMode deploymentMode,
                                    String managementGroupId,
                                    String subscriptionId,
                                    String resourceGroupName,
                                    String deploymentDataLocation,
                                    @NotNull List<EncryptedDataDetail> encryptedDataDetails,
                                    long timeoutInMs) {
        super(accountId, taskType, connectorDTO, timeoutInMs);
        this.templateBody = templateBody;
        this.parametersBody = parametersBody;
        this.deploymentScope = scopeType;
        this.deploymentMode = deploymentMode;
        this.managementGroupId = managementGroupId;
        this.subscriptionId = subscriptionId;
        this.resourceGroupName = resourceGroupName;
        this.encryptedDataDetails = encryptedDataDetails;
        this.deploymentDataLocation = deploymentDataLocation;
    }
}
