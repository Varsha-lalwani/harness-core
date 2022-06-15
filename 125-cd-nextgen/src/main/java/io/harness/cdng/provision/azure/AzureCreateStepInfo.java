/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.filters.WithConnectorRef;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.validation.Validator;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
@TypeAlias("CreateStepInfo")
@JsonTypeName(StepSpecTypeConstants.AZURE_CREATE_RESOURCE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@RecasterAlias("io.harness.cdng.provision.azure.CreateStepInfo")
public class AzureCreateStepInfo extends AzureCreateStepBaseStepInfo implements CDStepInfo, Visitable, WithConnectorRef {
  @NotNull @JsonProperty("configuration") AzureCreateStepConfiguration createStepConfiguration;

  @Builder(builderMethodName = "infoBuilder")
  public AzureCreateStepInfo(ParameterField<String> provisionerIdentifier,
      ParameterField<List<TaskSelectorYaml>> delegateSelector, AzureCreateStepConfiguration createStepConfiguration,
      String uuid) {
    super(provisionerIdentifier, delegateSelector, uuid);
    this.createStepConfiguration = createStepConfiguration;
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    // Azure connector
    if (createStepConfiguration.getConnectorRef() != null) {
      connectorRefMap.put("configuration.spec.connectorRef",
          ParameterField.createValueField(createStepConfiguration.getConnectorRef().getValue()));
    }

    if (createStepConfiguration.getTemplate() != null &&
        ManifestStoreType.isInGitSubset(createStepConfiguration.getTemplate().getStore().getSpec().getKind())) {
      connectorRefMap.put("configuration.spec.templateFile.store.spec.connectorRef",
              createStepConfiguration.getTemplate().getStore().getSpec().getConnectorReference());
    }
    if (createStepConfiguration.getParameters() != null &&
        ManifestStoreType.isInGitSubset(createStepConfiguration.getParameters().getStore().getSpec().getKind())) {
//      connectorRefMap.put("configuration.spec.parameters." + fileSpecs.getIdentifier() + ".store.spec.connectorRef",
//          fileSpecs.getStore().getSpec().getConnectorReference());
      // This is if we want a list of parameter files
      connectorRefMap.put("configuration.spec.parameters.store.spec.connectorRef",
          createStepConfiguration.getParameters().getStore().getSpec().getConnectorReference());
    }

    return connectorRefMap;
  }

  @Override
  public StepType getStepType() {
    return AzureCreateStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK_CHAIN;
  }
  @Override
  public SpecParameters getSpecParameters() {
    validateSpecParameters();
    return AzureCreateStepParameters.infoBuilder()
        .delegateSelectors(getDelegateSelectors())
        .provisionerIdentifier(getProvisionerIdentifier())
        .configuration(createStepConfiguration.toStepParameters())
        .build();
  }

  void validateSpecParameters() {
    Validator.notNullCheck("AzureCreateResource Step configuration is null", createStepConfiguration);
    createStepConfiguration.validateParams();
  }

  @Override
  public ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
    return getDelegateSelectors();
  }
}
