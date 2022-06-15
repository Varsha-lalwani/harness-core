/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.cdng.provision.azure.AzureCreateStepConfigurationParameters.AzureCreateStepConfigurationParametersBuilder;
import static io.harness.rule.OwnerRule.NGONZALEZ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.steps.StepHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AzureCommonHelperTest extends CategoryTest {
  @Mock private CDStepHelper cdStepHelper;
  @Mock private SecretManagerClientService secretManagerClientService;
  @Mock private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;

  @Mock private StepHelper stepHelper;
  AzureHelperTest azureHelperTest = new AzureHelperTest();
  @InjectMocks private AzureCommonHelper azureCommonHelper;

  @Before
  public void setUpMocks() {
    MockitoAnnotations.initMocks(this);
    ConnectorInfoDTO connectorInfoDTO = azureHelperTest.createAzureConnectorDTO();

    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());
    ConnectorInfoDTO gitConnectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(GitConfigDTO.builder().gitConnectionType(GitConnectionType.REPO).build())
            .build();
    doReturn(gitConnectorInfoDTO).when(cdStepHelper).getConnector(any(), any());
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();

    doReturn(sshKeySpecDTO).when(gitConfigAuthenticationInfoHelper).getSSHKey(any(), any(), any(), any());
    List<EncryptedDataDetail> apiEncryptedDataDetails = new ArrayList<>();
    doReturn(apiEncryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(), any());
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  // Create a test that validates getTemplateGitFetchFileConfig
  public void testGetTemplateGitFetchFileConfigForARM() {
    AzureCreateTemplateFile azureCreateTemplateFile = new AzureCreateTemplateFile();
    StoreConfigWrapper configStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .paths(ParameterField.createValueField(Collections.singletonList("path")))
                      .build())
            .build();
    azureCreateTemplateFile.setStore(configStoreConfigWrapper);

    GitFetchFilesConfig gitFetchFilesConfig = azureCommonHelper.getTemplateGitFetchFileConfig(
        azureHelperTest.getAmbiance(), azureCreateTemplateFile, AzureDeploymentTypes.ARM);

    assertThat(gitFetchFilesConfig.getIdentifier()).isEqualTo("templateFile");
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getGitConfigDTO().getConnectorType())
        .isEqualTo(ConnectorType.GIT);
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths().get(0)).isEqualTo("path");
  }
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testGetTemplateGitFetchFileConfigForBP() {
    AzureCreateTemplateFile azureCreateTemplateFile = new AzureCreateTemplateFile();
    StoreConfigWrapper configStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .folderPath(ParameterField.createValueField("folderPath"))
                      .build())
            .build();
    azureCreateTemplateFile.setStore(configStoreConfigWrapper);

    GitFetchFilesConfig gitFetchFilesConfig = azureCommonHelper.getTemplateGitFetchFileConfig(
        azureHelperTest.getAmbiance(), azureCreateTemplateFile, AzureDeploymentTypes.BLUEPRINT);

    assertThat(gitFetchFilesConfig.getIdentifier()).isEqualTo("bluePrint");
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getGitConfigDTO().getConnectorType())
        .isEqualTo(ConnectorType.GIT);
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths().get(0)).isEqualTo("folderPath");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testHasGitStoredParameters() {
    AzureCreateTemplateFile azureCreateTemplateFile = new AzureCreateTemplateFile();
    AzureCreateParameterFile azureCreateParameterFile = new AzureCreateParameterFile();
    StoreConfigWrapper configStoreConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .connectorRef(ParameterField.createValueField("template-connector-ref"))
                      .folderPath(ParameterField.createValueField("folderPath"))
                      .build())
            .build();
    azureCreateTemplateFile.setStore(configStoreConfigWrapper);
    azureCreateParameterFile.setStore(configStoreConfigWrapper);

    AzureCreateStepConfigurationParametersBuilder azureCreateStepConfigurationParameters =
        AzureCreateStepConfigurationParameters.builder()
            .templateFile(azureCreateTemplateFile)
            .parameters(azureCreateParameterFile)
            .connectorRef(ParameterField.createValueField("connectorRef"));

    boolean result = azureCommonHelper.hasGitStoredParameters(azureCreateStepConfigurationParameters.build());
    assertThat(result).isTrue();

    AzureCreateParameterFile azureCreateParameterFileNotGit = new AzureCreateParameterFile();
    ParameterField<List<String>> harnessStoreFiles = ParameterField.createValueField(Collections.singletonList("path"));
    StoreConfigWrapper configStoreConfigWrapperNotGit =
            StoreConfigWrapper.builder()
                    .spec(HarnessStore.builder()
                            .files(harnessStoreFiles)
                            .build())
                    .build();
    azureCreateParameterFileNotGit.setStore(configStoreConfigWrapperNotGit);
    azureCreateStepConfigurationParameters.parameters(azureCreateParameterFileNotGit);
    result = azureCommonHelper.hasGitStoredParameters(azureCreateStepConfigurationParameters.build());
    assertThat(result).isFalse();
  }
}
