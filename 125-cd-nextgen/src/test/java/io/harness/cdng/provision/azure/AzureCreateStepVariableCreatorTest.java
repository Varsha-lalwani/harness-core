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
import io.harness.cdng.provision.azure.variablecreator.AzureCreateStepVariableCreator;
import io.harness.cdng.provision.cloudformation.CloudformationCreateStackStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDP)
public class AzureCreateStepVariableCreatorTest extends CategoryTest {
  private final AzureCreateStepVariableCreator creator = new AzureCreateStepVariableCreator();
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testGetSupportedTypes() {
    assertThat(creator.getSupportedStepTypes())
        .isEqualTo(Collections.singleton(StepSpecTypeConstants.AZURE_CREATE_RESOURCE));
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testGetFieldClass() {
    assertThat(creator.getFieldClass()).isEqualTo(AzureCreateStepNode.class);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testCreateVariablesForParentNodeV2AzureCreateBlueprint() throws IOException {
    List<String> fqnPropertiesList = StepVariableCreatorTestUtils.getInfraFqnPropertiesForParentNodeV2(
        "cdng/variables/pipelineAzureBP.json", creator, AzureCreateStepNode.class);
    assertThat(fqnPropertiesList)
        .containsOnly(
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.timeout",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.name",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.description",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.provisionerIdentifier",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.delegateSelectors",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.scope",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.assignmentName",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.repoName",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.commitId",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.branch",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.paths",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.folderPath");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testCreateVariablesForParentNodeV2AzureCreateARMRemote() throws IOException {
    List<String> fqnPropertiesList = StepVariableCreatorTestUtils.getInfraFqnPropertiesForParentNodeV2(
        "cdng/variables/pipelineAzureARMRemote.json", creator, AzureCreateStepNode.class);
    assertThat(fqnPropertiesList)
        .containsOnly(
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.timeout",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.name",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.description",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.provisionerIdentifier",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.delegateSelectors",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.scope.spec.subscription",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.scope.spec.resourceGroup",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.scope.spec.mode",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.templateFile.spec.store.spec.repoName",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.templateFile.spec.store.spec.commitId",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.templateFile.spec.store.spec.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.templateFile.spec.store.spec.branch",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.templateFile.spec.store.spec.paths",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.templateFile.spec.store.spec.folderPath",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.parameters.spec.store.spec.repoName",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.parameters.spec.store.spec.commitId",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.parameters.spec.store.spec.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.parameters.spec.store.spec.branch",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.parameters.spec.store.spec.paths",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.spec.parameters.spec.store.spec.folderPath");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testCreateVariablesForParentNodeV2AzureCreateARMInlineParameters() throws IOException {
    List<String> fqnPropertiesList = StepVariableCreatorTestUtils.getInfraFqnPropertiesForParentNodeV2(
        "cdng/variables/pipelineAzureARMInlineParameters.json", creator, AzureCreateStepNode.class);
    assertThat(fqnPropertiesList)
        .containsOnly(
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.timeout",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.name",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.description",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.provisionerIdentifier",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.delegateSelectors",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.scope",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.subscription",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.deploymentDataLocation",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.managementGroupId",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.resourceGroup",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.mode",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.deploymentDataLocation",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.repoName",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.commitId",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.branch",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.paths",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.store.spec.folderPath",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.parameters.spec.parameterBody");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testCreateVariablesForParentNodeV2AzureCreateARMInlineTemplate() throws IOException {
    List<String> fqnPropertiesList = StepVariableCreatorTestUtils.getInfraFqnPropertiesForParentNodeV2(
        "cdng/variables/pipelineAzureARMInlineTemplate.json", creator, AzureCreateStepNode.class);
    assertThat(fqnPropertiesList)
        .containsOnly(
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.timeout",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.name",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.description",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.provisionerIdentifier",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.delegateSelectors",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.scope",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.subscription",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.resourceGroup",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.mode",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.managementGroupId",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.resourceGroup",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.deploymentDataLocation",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.templateFile.spec.templateBody",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.parameters.spec.store.spec.repoName",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.parameters.spec.store.spec.commitId",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.parameters.spec.store.spec.connectorRef",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.parameters.spec.store.spec.branch",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.parameters.spec.store.spec.paths",
            "pipeline.stages.cfstagetest.spec.infrastructure.infrastructureDefinition.provisioner.steps.asdf.spec.configuration.azureCreateDeployment.spec.parameters.spec.store.spec.folderPath");
  }
}
