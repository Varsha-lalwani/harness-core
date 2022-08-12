/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice.webapp;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.azure.model.AzureConstants.FETCH_ARTIFACT_FILE;
import static io.harness.rule.OwnerRule.TMACARI;
import static io.harness.rule.OwnerRule.VLICA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.azure.appservice.AzureAppServiceResourceUtilities;
import io.harness.delegate.task.azure.appservice.deployment.AzureAppServiceDeploymentService;
import io.harness.delegate.task.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
import io.harness.delegate.task.azure.artifact.ArtifactDownloadContext;
import io.harness.delegate.task.azure.artifact.AzurePackageArtifactConfig;
import io.harness.delegate.task.azure.common.AutoCloseableWorkingDirectory;
import io.harness.delegate.task.azure.common.AzureLogCallbackProvider;
import io.harness.rule.Owner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDP)
public class AzureAppServiceResourceUtilitiesTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private AzureAppServiceDeploymentService azureAppServiceDeploymentService;
  @InjectMocks AzureAppServiceResourceUtilities azureWebAppTaskHelper;

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testToArtifactNgDownloadContext() {
    AzureLogCallbackProvider azureLogCallbackProvider = mock(AzureLogCallbackProvider.class);
    AzurePackageArtifactConfig artifactConfig = AzurePackageArtifactConfig.builder().build();
    AutoCloseableWorkingDirectory autoCloseableWorkingDirectory =
        new AutoCloseableWorkingDirectory("repositoryPath", "rootWorkingDirPath");

    ArtifactDownloadContext artifactDownloadContext = azureWebAppTaskHelper.toArtifactNgDownloadContext(
        artifactConfig, autoCloseableWorkingDirectory, azureLogCallbackProvider);

    assertThat(artifactDownloadContext.getArtifactConfig()).isEqualTo(artifactConfig);
    assertThat(artifactDownloadContext.getWorkingDirectory().getPath()).startsWith("rootWorkingDirPath");
    assertThat(artifactDownloadContext.getLogCallbackProvider()).isEqualTo(azureLogCallbackProvider);
    assertThat(artifactDownloadContext.getCommandUnitName()).isEqualTo(FETCH_ARTIFACT_FILE);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testExecuteSwapSlots() {
    AzureLogCallbackProvider azureLogCallbackProvider = mock(AzureLogCallbackProvider.class);
    AzureWebClientContext azureWebClientContext = AzureWebClientContext.builder()
                                                      .resourceGroupName("resourceGroupName")
                                                      .subscriptionId("subscriptionId")
                                                      .azureConfig(AzureConfig.builder().build())
                                                      .build();

    azureWebAppTaskHelper.swapSlots(
        azureWebClientContext, azureLogCallbackProvider, "deploymentSlot", "targetSlot", 10, null);

    ArgumentCaptor<AzureAppServiceDeploymentContext> azureAppServiceDeploymentContextCaptor =
        ArgumentCaptor.forClass(AzureAppServiceDeploymentContext.class);

    verify(azureAppServiceDeploymentService)
        .swapSlotsUsingCallback(
            azureAppServiceDeploymentContextCaptor.capture(), eq("targetSlot"), eq(azureLogCallbackProvider), null);
    AzureAppServiceDeploymentContext azureAppServiceDeploymentContext =
        azureAppServiceDeploymentContextCaptor.getValue();
    assertThat(azureAppServiceDeploymentContext.getAzureWebClientContext()).isEqualTo(azureWebClientContext);
    assertThat(azureAppServiceDeploymentContext.getLogCallbackProvider()).isEqualTo(azureLogCallbackProvider);
    assertThat(azureAppServiceDeploymentContext.getSlotName()).isEqualTo("deploymentSlot");
    assertThat(azureAppServiceDeploymentContext.getSteadyStateTimeoutInMin()).isEqualTo(10);
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testExecuteSwapSlotsWhenSourceSlotIsProduction() {
    AzureLogCallbackProvider azureLogCallbackProvider = mock(AzureLogCallbackProvider.class);
    AzureWebClientContext azureWebClientContext = AzureWebClientContext.builder()
                                                      .resourceGroupName("resourceGroupName")
                                                      .subscriptionId("subscriptionId")
                                                      .azureConfig(AzureConfig.builder().build())
                                                      .build();

    azureWebAppTaskHelper.swapSlots(azureWebClientContext, azureLogCallbackProvider, "production", "testing", 10, null);

    ArgumentCaptor<AzureAppServiceDeploymentContext> azureAppServiceDeploymentContextCaptor =
        ArgumentCaptor.forClass(AzureAppServiceDeploymentContext.class);

    verify(azureAppServiceDeploymentService)
        .swapSlotsUsingCallback(
            azureAppServiceDeploymentContextCaptor.capture(), eq("production"), eq(azureLogCallbackProvider), null);
    AzureAppServiceDeploymentContext azureAppServiceDeploymentContext =
        azureAppServiceDeploymentContextCaptor.getValue();
    assertThat(azureAppServiceDeploymentContext.getAzureWebClientContext()).isEqualTo(azureWebClientContext);
    assertThat(azureAppServiceDeploymentContext.getLogCallbackProvider()).isEqualTo(azureLogCallbackProvider);
    assertThat(azureAppServiceDeploymentContext.getSlotName()).isEqualTo("testing");
    assertThat(azureAppServiceDeploymentContext.getSteadyStateTimeoutInMin()).isEqualTo(10);
  }
}
