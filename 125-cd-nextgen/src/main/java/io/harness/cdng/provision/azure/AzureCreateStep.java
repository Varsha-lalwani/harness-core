/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;

import io.harness.EntityType;
import io.harness.azure.model.ARMScopeType;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.provision.azure.beans.AzureCreatePassThroughData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.azure.arm.AzureTaskNGParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AzureCreateStep extends TaskChainExecutableWithRollbackAndRbac implements AzureCreateStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.AZURE_CREATE_RESOURCE.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Inject private PipelineRbacHelper pipelineRbacHelper;
  @Inject private KryoSerializer kryoSerializer;

  @Inject private StepHelper stepHelper;

  @Inject private AzureCreateStepHelper azureCreateStepHelper;
  @Inject private CDStepHelper cdStepHelper;

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    //        if (!cdFeatureFlagHelper.isEnabled(AmbianceUtils.getAccountId(ambiance), FeatureName.AZURE_ARM_NG)) {
    //            throw new AccessDeniedException(
    //                    "Azure NG is not enabled for this account. Please contact harness customer care.",
    //                    ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    //        }
    List<EntityDetail> entityDetailList = new ArrayList<>();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);

    // Template file connector
    AzureCreateStepParameters azureCreateStepParameters = (AzureCreateStepParameters) stepParameters.getSpec();
    AzureCreateTemplateFile azureCreateTemplateFile;
    azureCreateTemplateFile = azureCreateStepParameters.getConfiguration().getAzureDeploymentType().getTemplateSpecs();

    if (azureCreateTemplateFile.getType().equals(AzureCreateTemplateFileTypes.Remote)) {
      AzureRemoteTemplateFileSpec azureRemoteTemplateFileSpec =
          (AzureRemoteTemplateFileSpec) azureCreateTemplateFile.getSpec();
      String connectorRef =
          getParameterFieldValue(azureRemoteTemplateFileSpec.getStore().getSpec().getConnectorReference());
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
      EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
      entityDetailList.add(entityDetail);
    }

    // Parameters file connectors. This will be valid only if the type is ARM.
    if (Objects.equals(azureCreateStepParameters.getConfiguration().getAzureDeploymentType().getType(),
            AzureDeploymentTypes.ARM)) {
      AzureARMDeploymentSpec specs =
          (AzureARMDeploymentSpec) azureCreateStepParameters.getConfiguration().getAzureDeploymentType();
      if (specs.getParameters() != null
          && Objects.equals(specs.getParameters().getType(), AzureARMParametersFileTypes.Remote)) {
        AzureRemoteParametersFileSpec fileSpec = (AzureRemoteParametersFileSpec) specs.getParameters().getSpec();
        String connectorRef = getParameterFieldValue(fileSpec.getStore().getSpec().getConnectorReference());
        IdentifierRef identifierRef =
            IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
        EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
        entityDetailList.add(entityDetail);
      }
    }

    // Azure connector
    String connectorRef = azureCreateStepParameters.getConfiguration().getAzureDeploymentType().getConnectorRef();
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
    EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
    entityDetailList.add(entityDetail);

    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetailList, true);
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    return azureCreateStepHelper.executeNextLink(this, ambiance, stepParameters, passThroughData, responseSupplier);
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    if (passThroughData instanceof StepExceptionPassThroughData) {
      StepExceptionPassThroughData stepExceptionPassThroughData = (StepExceptionPassThroughData) passThroughData;
      return cdStepHelper.handleStepExceptionFailure(stepExceptionPassThroughData);
    }
    // TODO: To implement after the DelegateTask is implemented.
    return null;
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    return azureCreateStepHelper.startChainLink(this, ambiance, stepParameters);
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }
  @Override
  public TaskChainResponse executeCreateTask(Ambiance ambiance, StepElementParameters stepParameters,
      AzureTaskNGParameters parameters, AzureCreatePassThroughData passThroughData) {
    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.AZURE_NG_ARM_BLUEPRINT.name())
            .timeout(StepUtils.getTimeoutMillis(stepParameters.getTimeout(), AzureCreateStepHelper.DEFAULT_TIMEOUT))
            .parameters(new Object[] {parameters})
            .build();
    final TaskRequest taskRequest = StepUtils.prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        Collections.singletonList(AzureCommandUnit.Create.name()), TaskType.AZURE_NG_ARM_BLUEPRINT.getDisplayName(),
        TaskSelectorYaml.toTaskSelector(((AzureCreateStepParameters) stepParameters.getSpec()).getDelegateSelectors()),
        stepHelper.getEnvironmentType(ambiance));

    return TaskChainResponse.builder().taskRequest(taskRequest).passThroughData(passThroughData).chainEnd(true).build();
  }
}
