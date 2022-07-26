/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.instancesynchandlerfactory;

import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static io.harness.rule.OwnerRule.VIKYATH_HAREKAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.InstancesTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.rule.Owner;
import io.harness.service.instancesynchandler.AzureWebAppInstanceSyncHandler;
import io.harness.service.instancesynchandler.K8sInstanceSyncHandler;
import io.harness.service.instancesynchandler.NativeHelmInstanceSyncHandler;
import io.harness.service.instancesynchandler.ServerlessAwsLambdaInstanceSyncHandler;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class InstanceSyncHandlerFactoryServiceImplTest extends InstancesTestBase {
  @Mock K8sInstanceSyncHandler k8sInstanceSyncHandler;
  @Mock NativeHelmInstanceSyncHandler nativeHelmInstanceSyncHandler;
  @Mock ServerlessAwsLambdaInstanceSyncHandler serverlessAwsLambdaInstanceSyncHandler;
  @Mock AzureWebAppInstanceSyncHandler azureWebAppInstanceSyncHandler;
  @InjectMocks InstanceSyncHandlerFactoryServiceImpl instanceSyncHandlerFactoryService;

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void getInstanceSyncHandlerTestWhenDeploymentTypeIsKubernetes() {
    assertThat(instanceSyncHandlerFactoryService.getInstanceSyncHandler("Kubernetes"))
        .isEqualTo(k8sInstanceSyncHandler);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void getInstanceSyncHandlerTestWhenDeploymentTypeIsNativeHelm() {
    assertThat(instanceSyncHandlerFactoryService.getInstanceSyncHandler("NativeHelm"))
        .isEqualTo(nativeHelmInstanceSyncHandler);
  }

  @Test
  @Owner(developers = VIKYATH_HAREKAL)
  @Category(UnitTests.class)
  public void getInstanceSyncHandlerTestWhenDeploymentTypeIsServerlessAWSLambda() {
    assertThat(instanceSyncHandlerFactoryService.getInstanceSyncHandler(ServiceSpecType.SERVERLESS_AWS_LAMBDA))
        .isEqualTo(serverlessAwsLambdaInstanceSyncHandler);
  }

  @Test
  @Owner(developers = VIKYATH_HAREKAL)
  @Category(UnitTests.class)
  public void getInstanceSyncHandlerTestWhenDeploymentTypeIsAzureWebapp() {
    assertThat(instanceSyncHandlerFactoryService.getInstanceSyncHandler(ServiceSpecType.AZURE_WEBAPP))
        .isEqualTo(azureWebAppInstanceSyncHandler);
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void getInstanceSyncHandlerTestWhenDeploymentTypeIsNeitherK8sNorNativeHelm() {
    instanceSyncHandlerFactoryService.getInstanceSyncHandler("");
  }
}
