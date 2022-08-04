/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.rule.OwnerRule.NGONZALEZ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDP)

public class AzureCreateStepInfoTest extends CategoryTest {
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testValidateParams() {
    AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);

    StoreConfigWrapper fileStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("parameters-connector-ref"))
                      .build())
            .build();
    AzureCreateParameterFile parametersFileBuilder = new AzureCreateParameterFile();
    parametersFileBuilder.setStore(fileStoreConfigWrapper);

    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .parameters(parametersFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureResourceGroupSpec.builder()
                                 .subscription(ParameterField.createValueField("foobar"))
                                 .resourceGroup(ParameterField.createValueField("bar"))
                                 .mode(ParameterField.createValueField("INCREMENTAL"))
                                 .build())
                       .build())
            .build());
    azureCreateStepInfo.validateSpecParameters();
    assertThat(azureCreateStepInfo.getCreateStepConfiguration().getConnectorRef().getValue()).isEqualTo("connectorRef");
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
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);

    StoreConfigWrapper fileStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("parameters-connector-ref"))
                      .build())
            .build();
    AzureCreateParameterFile parametersFileBuilder = new AzureCreateParameterFile();
    parametersFileBuilder.setStore(fileStoreConfigWrapper);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .template(templateFileBuilder)
            .parameters(parametersFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureResourceGroupSpec.builder()
                                 .subscription(ParameterField.createValueField("foobar"))
                                 .resourceGroup(ParameterField.createValueField("bar"))
                                 .mode(ParameterField.createValueField("INCREMENTAL"))
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
    StoreConfigWrapper fileStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("parameters-connector-ref"))
                      .build())
            .build();
    AzureCreateParameterFile parametersFileBuilder = new AzureCreateParameterFile();
    parametersFileBuilder.setStore(fileStoreConfigWrapper);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .parameters(parametersFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureResourceGroupSpec.builder()
                                 .subscription(ParameterField.createValueField("foobar"))
                                 .resourceGroup(ParameterField.createValueField("bar"))
                                 .mode(ParameterField.createValueField("INCREMENTAL"))
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
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(AzureCreateStepConfiguration.builder()
                                                       .template(templateFileBuilder)
                                                       .connectorRef(ParameterField.createValueField("connectorRef"))
                                                       .build());
    assertThatThrownBy(azureCreateStepInfo::validateSpecParameters).hasMessageContaining("Scope can't be empty");
  }
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testValidateParamsWithNoSubscriptionAtRGLevel() {
    AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureResourceGroupSpec.builder()
                                 .resourceGroup(ParameterField.createValueField("bar"))
                                 .mode(ParameterField.createValueField("INCREMENTAL"))
                                 .build())
                       .build())
            .build());
    assertThatThrownBy(azureCreateStepInfo::validateSpecParameters).hasMessageContaining("subscription can't be null");
  }
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testValidateParamsWithNoResourceGroupAtRGLEvel() {
    AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureResourceGroupSpec.builder()
                                 .subscription(ParameterField.createValueField("foobar"))
                                 .mode(ParameterField.createValueField("INCREMENTAL"))
                                 .build())
                       .build())
            .build());
    assertThatThrownBy(azureCreateStepInfo::validateSpecParameters).hasMessageContaining("resourceGroup can't be null");
  }
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testValidateParamsWithNoModeAtRGLevel() {
    AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureResourceGroupSpec.builder()
                                 .subscription(ParameterField.createValueField("foobar"))
                                 .resourceGroup(ParameterField.createValueField("bar"))
                                 .build())
                       .build())
            .build());
    assertThatThrownBy(azureCreateStepInfo::validateSpecParameters).hasMessageContaining("mode can't be null");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testValidateParamsWithNoSubscriptionAtSubLevel() {
    AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(
                AzureCreateStepScope.builder()
                    .spec(AzureSubscriptionSpec.builder().location(ParameterField.createValueField("foobar")).build())
                    .build())
            .build());
    assertThatThrownBy(azureCreateStepInfo::validateSpecParameters).hasMessageContaining("subscription can't be null");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testValidateParamsWithNoLocationAtSubLevel() {
    AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(
                AzureCreateStepScope.builder()
                    .spec(
                        AzureSubscriptionSpec.builder().subscription(ParameterField.createValueField("foobar")).build())
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
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureSubscriptionSpec.builder()
                                 .location(ParameterField.createValueField("foobar"))
                                 .subscription(ParameterField.createValueField("foobar"))
                                 .build())
                       .build())
            .build());
    assertThatThrownBy(azureCreateStepInfo::validateSpecParameters).hasMessageContaining("mode can't be null");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testValidateParamsWithNoManagementAtManagementLevel() {
    AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureManagementSpec.builder().location(ParameterField.createValueField("foobar")).build())
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
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureManagementSpec.builder()
                                 .managementGroupId(ParameterField.createValueField("foobar"))
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
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureManagementSpec.builder()
                                 .location(ParameterField.createValueField("foobar"))
                                 .managementGroupId(ParameterField.createValueField("foobar"))
                                 .build())
                       .build())
            .build());
    assertThatThrownBy(azureCreateStepInfo::validateSpecParameters).hasMessageContaining("mode can't be null");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testValidateParamsWithNoLocationAtTenantLevel() {
    AzureCreateStepInfo azureCreateStepInfo = new AzureCreateStepInfo();
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureTenantSpec.builder().location(ParameterField.createValueField("foobar")).build())
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
    StoreConfigWrapper templateStore =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .paths(ParameterField.createValueField(new ArrayList<>()))
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .build())
            .build();

    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    templateFileBuilder.setStore(templateStore);
    azureCreateStepInfo.setCreateStepConfiguration(
        AzureCreateStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("connectorRef"))
            .template(templateFileBuilder)
            .scope(AzureCreateStepScope.builder()
                       .spec(AzureTenantSpec.builder().location(ParameterField.createValueField("foobar")).build())
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
    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    AzureCreateParameterFile parameterFileBuilder = new AzureCreateParameterFile();
    StoreConfigWrapper fileStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(
                GithubStore.builder().connectorRef(ParameterField.createValueField("parameters-connector-ref")).build())
            .build();
    StoreConfigWrapper configStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder().connectorRef(ParameterField.createValueField("template-connector-ref")).build())
            .build();
    parameterFileBuilder.setStore(fileStoreConfigWrapper);
    templateFileBuilder.setStore(configStoreConfigWrapper);
    azureCreateStepInfo.setCreateStepConfiguration(AzureCreateStepConfiguration.builder()
                                                       .template(templateFileBuilder)
                                                       .parameters(parameterFileBuilder)
                                                       .connectorRef(ParameterField.createValueField("azConnectorRef"))
                                                       .build());
    Map<String, ParameterField<String>> parameterFieldMap = azureCreateStepInfo.extractConnectorRefs();
    assertThat(parameterFieldMap.size()).isEqualTo(3);
    assertThat(parameterFieldMap.get("configuration.spec.connectorRef").getValue()).isEqualTo("azConnectorRef");
    assertThat(parameterFieldMap.get("configuration.spec.templateFile.store.spec.connectorRef").getValue())
        .isEqualTo("template-connector-ref");
    assertThat(
        parameterFieldMap.get("configuration.spec.parameters.parameters-identifier.store.spec.connectorRef").getValue())
        .isEqualTo("parameters-connector-ref");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testExtractConnectorRefForBP() {
    AzureCreateBPStepInfo azureCreateStepInfo = new AzureCreateBPStepInfo();
    AzureCreateTemplateFile templateFileBuilder = new AzureCreateTemplateFile();
    StoreConfigWrapper configStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder().connectorRef(ParameterField.createValueField("template-connector-ref")).build())
            .build();
    templateFileBuilder.setStore(configStoreConfigWrapper);
    azureCreateStepInfo.setCreateStepBPConfiguration(
        AzureCreateBPStepConfiguration.builder()
            .template(templateFileBuilder)
            .connectorRef(ParameterField.createValueField("azConnectorRef"))
            .build());
    Map<String, ParameterField<String>> parameterFieldMap = azureCreateStepInfo.extractConnectorRefs();
    assertThat(parameterFieldMap.size()).isEqualTo(2);
    assertThat(parameterFieldMap.get("configuration.spec.connectorRef").getValue()).isEqualTo("azConnectorRef");
    assertThat(parameterFieldMap.get("configuration.spec.templateFile.store.spec.connectorRef").getValue())
        .isEqualTo("template-connector-ref");
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
