/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.azure;

import static io.harness.rule.OwnerRule.NGONZALEZ;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.creator.variables.StepVariableCreatorTestUtils;
import io.harness.cdng.provision.azure.variablecreator.AzureCreateBPStepVariableCreator;
import io.harness.cdng.provision.azure.variablecreator.AzureCreateStepVariableCreator;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDP)
public class AzureCreateStepVariableCreatorTest extends CategoryTest {
  private final AzureCreateStepVariableCreator ARMCreator = new AzureCreateStepVariableCreator();
  private final AzureCreateBPStepVariableCreator BPCreator = new AzureCreateBPStepVariableCreator();

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testGetSupportedTypes() {
    assertThat(ARMCreator.getSupportedStepTypes())
        .isEqualTo(Collections.singleton(StepSpecTypeConstants.AZURE_CREATE_RESOURCE));
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testGetFieldClass() {
    assertThat(ARMCreator.getFieldClass()).isEqualTo(AzureCreateStepNode.class);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testCreateVariablesForParentNodeV2AzureCreateBlueprint() throws IOException {
    List<String> fqnPropertiesList = StepVariableCreatorTestUtils.getInfraFqnPropertiesForParentNodeV2(
        "cdng/variables/pipelineAzureBP.json", BPCreator, AzureCreateBPStepNode.class);
    assertThat(fqnPropertiesList)
        .containsOnly(
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.timeout",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.name",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.description",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.provisionerIdentifier",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.configuration.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.delegateSelectors",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.configuration.scope",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.configuration.assignmentName",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.configuration.template.store.spec.repoName",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.configuration.template.store.spec.commitId",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.configuration.template.store.spec.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.configuration.template.store.spec.branch",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.configuration.template.store.spec.paths",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdfg.spec.configuration.template.store.spec.folderPath");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testCreateVariablesForParentNodeV2AzureCreateARMRemote() throws IOException {
    List<String> fqnPropertiesList = StepVariableCreatorTestUtils.getInfraFqnPropertiesForParentNodeV2(
        "cdng/variables/pipelineAzureARMRemote.json", ARMCreator, AzureCreateStepNode.class);
    assertThat(fqnPropertiesList)
        .containsOnly(
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.timeout",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.name",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.description",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.provisionerIdentifier",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.delegateSelectors",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.scope.spec.subscription",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.scope.spec.resourceGroup",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.scope.spec.mode",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.template.store.spec.repoName",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.template.store.spec.commitId",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.template.store.spec.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.template.store.spec.branch",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.template.store.spec.paths",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.template.store.spec.folderPath",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.parameters.store.spec.repoName",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.parameters.store.spec.commitId",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.parameters.store.spec.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.parameters.store.spec.branch",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.parameters.store.spec.paths",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.parameters.store.spec.folderPath");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testCreateVariablesForParentNodeV2AzureCreateARMInlineParameters() throws IOException {}

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testCreateVariablesForParentNodeV2AzureCreateARMInlineTemplate() throws IOException {}
}
