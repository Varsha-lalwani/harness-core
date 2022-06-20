package io.harness.cdng.provision.azure;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.delegate.task.azure.arm.request.AzureARMDeploymentParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

import static io.harness.rule.OwnerRule.NGONZALEZ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@OwnedBy(HarnessTeam.CDP)

public class AzureCreateStepInfoTest extends CategoryTest {
    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParams() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
            AzureCreateStepConfiguration.builder()
                    .spec(AzureARMDeploymentSpec.builder()
                                    .connectorRef("connectorRef")
                            .templateFile(AzureCreateTemplateFile.builder().build())
                            .scope(AzureCreateStepScope.builder()
                                    .spec(
                                            AzureResourceGroupSpec.builder()
                                                    .subscription(ParameterField.createValueField("foobar"))
                                                    .resourceGroup(ParameterField.createValueField("bar"))
                                                    .mode(ParameterField.createValueField("INCREMENTAL"))
                                                    .build())
                                    .build())
                            .build())
                .build());
        azureCreateStepInfo.validateSpecParameters();
        assertThat(azureCreateStepInfo.getCreateStepConfiguration().getSpec().getConnectorRef()).isEqualTo("connectorRef");
    }
    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoDeploymentInfo() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
            .hasMessageContaining("AzureCreateResource Step configuration is null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoConfiguration() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("AzureCreateResource Step configuration is null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoConnectorRef() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureResourceGroupSpec.builder()
                                                        .subscription(ParameterField.createValueField("foobar"))
                                                        .resourceGroup(ParameterField.createValueField("bar"))
                                                        .mode(ParameterField.createValueField("INCREMENTAL"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("Connector ref can't be empty");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoTemplateFiles() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureResourceGroupSpec.builder()
                                                        .subscription(ParameterField.createValueField("foobar"))
                                                        .resourceGroup(ParameterField.createValueField("bar"))
                                                        .mode(ParameterField.createValueField("INCREMENTAL"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("Template file can't be empty");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoScope() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .connectorRef("connectorRef")
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("Scope can't be empty");
    }
    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoSubscriptionAtRGLevel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureResourceGroupSpec.builder()
                                                        .resourceGroup(ParameterField.createValueField("bar"))
                                                        .mode(ParameterField.createValueField("INCREMENTAL"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("subscription can't be null");
    }
    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoResourceGroupAtRGLEvel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureResourceGroupSpec.builder()
                                                        .subscription(ParameterField.createValueField("foobar"))
                                                        .mode(ParameterField.createValueField("INCREMENTAL"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("resourceGroup can't be null");
    }
    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoModeAtRGLevel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureResourceGroupSpec.builder()
                                                        .subscription(ParameterField.createValueField("foobar"))
                                                        .resourceGroup(ParameterField.createValueField("bar"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("mode can't be null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoSubscriptionAtSubLevel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureSubscritionSpec.builder()
                                                        .deploymentDataLocation(ParameterField.createValueField("foobar"))
                                                        .mode(ParameterField.createValueField("INCREMENTAL"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("subscription can't be null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoLocationAtSubLevel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureSubscritionSpec.builder()
                                                        .subscription(ParameterField.createValueField("foobar"))
                                                        .mode(ParameterField.createValueField("INCREMENTAL"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("deploymentDataLocation can't be null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoModeAtSubLevel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureSubscritionSpec.builder()
                                                        .deploymentDataLocation(ParameterField.createValueField("foobar"))
                                                        .subscription(ParameterField.createValueField("foobar"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("mode can't be null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoManagementAtManagementLevel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureManagementSpec.builder()
                                                        .deploymentDataLocation(ParameterField.createValueField("foobar"))
                                                        .mode(ParameterField.createValueField("INCREMENTAL"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("managementGroupId can't be null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoLocationAtManagementLevel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureManagementSpec.builder()
                                                        .managementGroupId(ParameterField.createValueField("foobar"))
                                                        .mode(ParameterField.createValueField("INCREMENTAL"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("deploymentDataLocation can't be null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoModeAtManagementLevel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureManagementSpec.builder()
                                                        .deploymentDataLocation(ParameterField.createValueField("foobar"))
                                                        .managementGroupId(ParameterField.createValueField("foobar"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("mode can't be null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoLocationAtTenantLevel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureTenantSpec.builder()
                                                        .managementGroupId(ParameterField.createValueField("foobar"))
                                                        .mode(ParameterField.createValueField("INCREMENTAL"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("deploymentDataLocation can't be null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testValidateParamsWithNoManagementAtTenantLevel() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .spec(AzureARMDeploymentSpec.builder()
                                .connectorRef("connectorRef")
                                .templateFile(AzureCreateTemplateFile.builder().build())
                                .scope(AzureCreateStepScope.builder()
                                        .spec(
                                                AzureTenantSpec.builder()
                                                        .deploymentDataLocation(ParameterField.createValueField("foobar"))
                                                        .mode(ParameterField.createValueField("INCREMENTAL"))
                                                        .build())
                                        .build())
                                .build())
                        .build());
        assertThatThrownBy(azureCreateStepInfo::validateSpecParameters)
                .hasMessageContaining("managementGroupId can't be null");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testExtractConnectorRefForARM() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        AzureRemoteParametersFileSpec azureRemoteParametersFileSpec = new AzureRemoteParametersFileSpec();
        AzureRemoteTemplateFileSpec azureRemoteTemplateFileSpec = new AzureRemoteTemplateFileSpec();
        StoreConfigWrapper fileStoreConfigWrapper =
                StoreConfigWrapper.builder()
                        .spec(GithubStore.builder().connectorRef(ParameterField.createValueField("parameters-connector-ref")).build())
                        .build();
        StoreConfigWrapper configStoreConfigWrapper =
                StoreConfigWrapper.builder()
                        .spec(GithubStore.builder().connectorRef(ParameterField.createValueField("template-connector-ref")).build())
                        .build();
        azureRemoteParametersFileSpec.setStore(fileStoreConfigWrapper);
        azureRemoteTemplateFileSpec.setStore(configStoreConfigWrapper);
        azureRemoteParametersFileSpec.setIdentifier("parameters-identifier");
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .type(AzureDeploymentTypes.ARM)
                        .spec(AzureARMDeploymentSpec.builder()
                                .templateFile(AzureCreateTemplateFile.builder()
                                        .type(AzureCreateTemplateFileTypes.Remote)
                                        .spec(azureRemoteTemplateFileSpec)
                                        .build())
                                .parameters(AzureCreateParameterFile.builder()
                                        .type(AzureARMParametersFileTypes.Remote)
                                        .spec(azureRemoteParametersFileSpec)
                                        .build())
                                .connectorRef("azConnectorRef")
                                .build())
                        .build());
        Map<String, ParameterField<String>> parameterFieldMap = azureCreateStepInfo.extractConnectorRefs();
        assertThat(parameterFieldMap.size()).isEqualTo(3);
        assertThat(parameterFieldMap.get("configuration.spec.connectorRef").getValue()).isEqualTo("azConnectorRef");
        assertThat(parameterFieldMap.get("configuration.spec.templateFile.store.spec.connectorRef").getValue()).isEqualTo("template-connector-ref");
        assertThat(parameterFieldMap.get("configuration.spec.parameters.parameters-identifier.store.spec.connectorRef").getValue()).isEqualTo("parameters-connector-ref");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testExtractConnectorRefForBP() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        AzureRemoteTemplateFileSpec azureRemoteTemplateFileSpec = new AzureRemoteTemplateFileSpec();
        StoreConfigWrapper configStoreConfigWrapper =
                StoreConfigWrapper.builder()
                        .spec(GithubStore.builder().connectorRef(ParameterField.createValueField("template-connector-ref")).build())
                        .build();
        azureRemoteTemplateFileSpec.setStore(configStoreConfigWrapper);
        azureCreateStepInfo.setCreateStepConfiguration(
                AzureCreateStepConfiguration.builder()
                        .type(AzureDeploymentTypes.BLUEPRINT)
                        .spec(AzureBluePrintDeploymentSpec.builder()
                                .templateFile(AzureCreateTemplateFile.builder()
                                        .spec(azureRemoteTemplateFileSpec)
                                        .build())
                                .connectorRef("azConnectorRef")
                                .build())
                        .build());
        Map<String, ParameterField<String>> parameterFieldMap = azureCreateStepInfo.extractConnectorRefs();
        assertThat(parameterFieldMap.size()).isEqualTo(2);
        assertThat(parameterFieldMap.get("configuration.spec.connectorRef").getValue()).isEqualTo("azConnectorRef");
        assertThat(parameterFieldMap.get("configuration.spec.templateFile.store.spec.connectorRef").getValue()).isEqualTo("template-connector-ref");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testGetFacilitatorType() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        String response = azureCreateStepInfo.getFacilitatorType();
        assertThat(response).isEqualTo("TASK_CHAIN");
    }

    @Test
    @Owner(developers = NGONZALEZ)
    @Category(UnitTests.class)
    public void testGetStepType() {
        AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
        StepType response = azureCreateStepInfo.getStepType();
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("AzureCreateResource");
    }
}
