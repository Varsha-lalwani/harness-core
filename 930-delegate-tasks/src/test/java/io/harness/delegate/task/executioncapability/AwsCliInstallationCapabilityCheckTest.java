/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.AwsCliInstallationCapability;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDP)
public class AwsCliInstallationCapabilityCheckTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @InjectMocks private AwsCliInstallationCapabilityCheck awsCliInstallationCapabilityCheck;
  private ExecutionCapability executionCapability = AwsCliInstallationCapability.builder().criteria("a").build();

  @Test
  @Owner(developers = OwnerRule.PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void performCapabilityCheckTest() {
    CapabilityResponse capabilityResponse =
        awsCliInstallationCapabilityCheck.performCapabilityCheck(executionCapability);
    assertThat(capabilityResponse.getDelegateCapability()).isInstanceOf(AwsCliInstallationCapability.class);
  }
}
