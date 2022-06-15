package io.harness.cdng.provision.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
@TypeAlias("CreateStepInfo")
@JsonTypeName(StepSpecTypeConstants.AZURE_CREATE_RESOURCE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@RecasterAlias("io.harness.cdng.provision.azure.CreateStepInfo")
public class AzureCreateStepInfo extends AzureCreateBaseStepInfo implements CDStepInfo, Visitable, WithConnectorRef {
    @NotNull
    @JsonProperty("configuration")
    AzureCreateStepConfiguration createStepConfiguration;

    @Builder(builderMethodName = "infoBuilder")
    public AzureCreateStepInfo(ParameterField<String> provisionerIdentifier,
                               ParameterField<List<TaskSelectorYaml>> delegateSelector,
                               AzureCreateStepConfiguration createStepConfiguration,
                               String uuid) {
        super(provisionerIdentifier, delegateSelector, uuid);
        this.createStepConfiguration = createStepConfiguration;
    }

    @Override
    public Map<String, ParameterField<String>> extractConnectorRefs() {
        Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
        // Azure connector
        if (createStepConfiguration.getConnectorRef() != null) {
            connectorRefMap.put("configuration.connectorRef", createStepConfiguration.getConnectorRef());
        }
        if (Objects.equals(createStepConfiguration.getAzureCreateDeployment().getType(), AzureAzureDeploymentTypes.ARM)){
            AzureARMDeploymentSpec specs = (AzureARMDeploymentSpec) createStepConfiguration.getAzureCreateDeployment().getSpec();
            if (isNotEmpty(specs.getTemplateFile().getSpec().getType()) &&
                    specs.getTemplateFile().getSpec().getType().equals(AzureCreateTemplateFileTypes.Remote)) {
                AzureRemoteTemplateFileSpec remoteTemplateFile =
                        (AzureRemoteTemplateFileSpec) specs.getTemplateFile().getSpec();
                connectorRefMap.put("configuration.deployment.spec.templateFile.store.spec.connectorRef",
                        remoteTemplateFile.getStore().getSpec().getConnectorReference());
            }
            if (isNotEmpty(specs.getParametersFilesSpecs())) {
                specs.getParametersFilesSpecs().forEach(createParametersFileSpec
                        -> connectorRefMap.put("configuration.deployment.spec.parameters." + createParametersFileSpec.getIdentifier()
                                + ".store.spec.connectorRef",
                        createParametersFileSpec.getStore().getSpec().getConnectorReference()));
            }

        } else {
            AzureBluePrintDeploymentSpec specs = (AzureBluePrintDeploymentSpec) createStepConfiguration.getAzureCreateDeployment().getSpec();
            if (isNotEmpty(specs.getTemplateFile().getSpec().getType()) &&
                    specs.getTemplateFile().getSpec().getType().equals(AzureCreateTemplateFileTypes.Remote)) {
                AzureRemoteTemplateFileSpec remoteTemplateFile =
                        (AzureRemoteTemplateFileSpec) specs.getTemplateFile().getSpec();
                connectorRefMap.put("configuration.deployment.spec.templateFile.store.spec.connectorRef",
                        remoteTemplateFile.getStore().getSpec().getConnectorReference());
            }
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
