/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.inputset.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.VED;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.context.GlobalContext;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.manage.GlobalContextManager;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.exceptions.InvalidInputSetException;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PipelineCRUDErrorResponse;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({OverlayInputSetValidationHelper.class})
@OwnedBy(PIPELINE)
public class InputSetValidationHelperTest extends CategoryTest {
  @Mock PMSInputSetService inputSetService;
  @Mock PMSPipelineService pipelineService;

  String accountId = "accountId";
  String orgId = "orgId";
  String projectId = "projectId";
  String pipelineId = "Test_Pipline11";
  String pipelineYaml;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    String pipelineFile = "pipeline-extensive.yml";
    pipelineYaml = readFile(pipelineFile);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateInputSetForNonExistentPipeline() {
    doReturn(Optional.empty()).when(pipelineService).get(accountId, orgId, projectId, pipelineId, false);
    InputSetEntity inputSetEntity = InputSetEntity.builder()
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .pipelineIdentifier(pipelineId)
                                        .build();
    assertThatThrownBy(
        () -> InputSetValidationHelper.validateInputSet(inputSetService, pipelineService, inputSetEntity, false))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(orgId, projectId, pipelineId));
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateInputSetForInvalidStoreType() {
    doReturn(Optional.of(PipelineEntity.builder().storeType(StoreType.INLINE).build()))
        .when(pipelineService)
        .get(accountId, orgId, projectId, pipelineId, false);
    setupGitContext(GitEntityInfo.builder().storeType(StoreType.REMOTE).build());
    InputSetEntity inputSetEntity = InputSetEntity.builder()
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .pipelineIdentifier(pipelineId)
                                        .build();
    assertThatThrownBy(
        () -> InputSetValidationHelper.validateInputSet(inputSetService, pipelineService, inputSetEntity, true))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Input Set should have the same Store Type as the Pipeline it is for");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateInputSetWithoutIdentifier() {
    doReturn(Optional.of(PipelineEntity.builder().storeType(StoreType.INLINE).build()))
        .when(pipelineService)
        .get(accountId, orgId, projectId, pipelineId, false);
    String yaml = "inputSet:\n"
        + "  name: abc";
    InputSetEntity inputSetEntity = InputSetEntity.builder()
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .pipelineIdentifier(pipelineId)
                                        .yaml(yaml)
                                        .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                        .build();
    assertThatThrownBy(() -> InputSetValidationHelper.validateInputSet(null, pipelineService, inputSetEntity, false))
        .hasMessage("Identifier cannot be empty");
  }

  @Test
  @Owner(developers = VED)
  @Category(UnitTests.class)
  public void testForLengthCheckOnInputSetIdentifiers() {
    doReturn(Optional.of(PipelineEntity.builder().storeType(StoreType.INLINE).build()))
        .when(pipelineService)
        .get(accountId, orgId, projectId, pipelineId, false);
    String yaml = "inputSet:\n"
        + "  identifier: abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij";
    InputSetEntity inputSetEntity = InputSetEntity.builder()
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .pipelineIdentifier(pipelineId)
                                        .yaml(yaml)
                                        .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                        .build();
    assertThatThrownBy(() -> InputSetValidationHelper.validateInputSet(null, pipelineService, inputSetEntity, false))
        .hasMessage("Input Set identifier length cannot be more that 63 characters.");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateInputSetWithNoOrgAndProjectID() {
    doReturn(Optional.of(PipelineEntity.builder().storeType(StoreType.INLINE).build()))
        .when(pipelineService)
        .get(accountId, orgId, projectId, pipelineId, false);

    String inputSetFileWithNoProjOrOrg = "inputSet1.yml";
    String inputSetYamlWithNoProjOrOrg = readFile(inputSetFileWithNoProjOrOrg);
    InputSetEntity inputSetEntity = InputSetEntity.builder()
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .pipelineIdentifier(pipelineId)
                                        .yaml(inputSetYamlWithNoProjOrOrg)
                                        .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                        .build();
    assertThatThrownBy(
        () -> InputSetValidationHelper.validateInputSet(inputSetService, pipelineService, inputSetEntity, true))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Organization identifier is missing in the YAML. Please give a valid Organization identifier");

    String inputSetFileWithNoProj = "inputset1-with-org-id.yaml";
    String inputSetYamlWithNoProj = readFile(inputSetFileWithNoProj);
    InputSetEntity inputSetEntity1 = InputSetEntity.builder()
                                         .accountId(accountId)
                                         .orgIdentifier(orgId)
                                         .projectIdentifier(projectId)
                                         .pipelineIdentifier(pipelineId)
                                         .yaml(inputSetYamlWithNoProj)
                                         .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                         .build();
    assertThatThrownBy(
        () -> InputSetValidationHelper.validateInputSet(inputSetService, pipelineService, inputSetEntity1, true))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Project identifier is missing in the YAML. Please give a valid Project identifier");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateInputSetWithNoErrors() {
    setupGitContext(GitEntityInfo.builder().storeType(StoreType.REMOTE).build());
    PipelineEntity pipelineEntity = PipelineEntity.builder().yaml(pipelineYaml).storeType(StoreType.REMOTE).build();
    doReturn(Optional.of(pipelineEntity)).when(pipelineService).get(accountId, orgId, projectId, pipelineId, false);

    String inputSetFile = "inputset1-with-org-proj-id.yaml";
    String inputSetYaml = readFile(inputSetFile);
    InputSetEntity inputSetEntity = InputSetEntity.builder()
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .pipelineIdentifier(pipelineId)
                                        .yaml(inputSetYaml)
                                        .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                        .storeType(StoreType.REMOTE)
                                        .build();
    // no exception should be thrown
    InputSetValidationHelper.validateInputSet(inputSetService, pipelineService, inputSetEntity, true);

    setupGitContext(GitEntityInfo.builder().storeType(StoreType.REMOTE).isNewBranch(true).baseBranch("br").build());
    // no exception should be thrown
    InputSetValidationHelper.validateInputSet(inputSetService, pipelineService, inputSetEntity, true);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateInputSetWithErrors() {
    PipelineEntity pipelineEntity = PipelineEntity.builder().yaml(pipelineYaml).storeType(StoreType.INLINE).build();
    doReturn(Optional.of(pipelineEntity)).when(pipelineService).get(accountId, orgId, projectId, pipelineId, false);

    String inputSetFile = "inputSetWrong1.yml";
    String inputSetYaml = readFile(inputSetFile);
    InputSetEntity inputSetEntity = InputSetEntity.builder()
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .pipelineIdentifier(pipelineId)
                                        .yaml(inputSetYaml)
                                        .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                        .build();
    assertThatThrownBy(
        () -> InputSetValidationHelper.validateInputSet(inputSetService, pipelineService, inputSetEntity, true))
        .isInstanceOf(InvalidInputSetException.class);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateInputSetForOldGitSync() {
    PipelineEntity pipelineEntity = PipelineEntity.builder().yaml(pipelineYaml).build();
    doReturn(Optional.of(pipelineEntity)).when(pipelineService).get(accountId, orgId, projectId, pipelineId, false);

    String inputSetFile = "inputset1-with-org-proj-id.yaml";
    String inputSetYaml = readFile(inputSetFile);
    InputSetEntity inputSetEntity = InputSetEntity.builder()
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .pipelineIdentifier(pipelineId)
                                        .yaml(inputSetYaml)
                                        .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                        .build();

    // no exception should be thrown here
    InputSetValidationHelper.validateInputSetForOldGitSync(inputSetService, pipelineService, inputSetEntity, "", "");
    InputSetValidationHelper.validateInputSetForOldGitSync(
        inputSetService, pipelineService, inputSetEntity, "branch", "repo");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateInputSetForOldGitSyncWithErrors() {
    PipelineEntity pipelineEntity = PipelineEntity.builder().yaml(pipelineYaml).storeType(StoreType.INLINE).build();
    doReturn(Optional.of(pipelineEntity)).when(pipelineService).get(accountId, orgId, projectId, pipelineId, false);

    String inputSetFile = "inputSetWrong1.yml";
    String inputSetYaml = readFile(inputSetFile);
    InputSetEntity inputSetEntity = InputSetEntity.builder()
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .pipelineIdentifier(pipelineId)
                                        .yaml(inputSetYaml)
                                        .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                        .build();
    assertThatThrownBy(()
                           -> InputSetValidationHelper.validateInputSetForOldGitSync(
                               inputSetService, pipelineService, inputSetEntity, "", ""))
        .isInstanceOf(InvalidInputSetException.class);
    assertThatThrownBy(()
                           -> InputSetValidationHelper.validateInputSetForOldGitSync(
                               inputSetService, pipelineService, inputSetEntity, "branch", "repo"))
        .isInstanceOf(InvalidInputSetException.class);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateInputSetForOldGitSyncWithNonExistentPipeline() {
    doReturn(Optional.empty()).when(pipelineService).get(accountId, orgId, projectId, pipelineId, false);
    String inputSetYaml = "anything";
    InputSetEntity inputSetEntity = InputSetEntity.builder()
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .pipelineIdentifier(pipelineId)
                                        .yaml(inputSetYaml)
                                        .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                        .build();
    assertThatThrownBy(()
                           -> InputSetValidationHelper.validateInputSetForOldGitSync(
                               inputSetService, pipelineService, inputSetEntity, "", ""))
        .isInstanceOf(InvalidRequestException.class);
  }

  private String readFile(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      return Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not read resource file: " + filename);
    }
  }

  private void setupGitContext(GitEntityInfo branchInfo) {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(GitSyncBranchContext.builder().gitBranchInfo(branchInfo).build());
  }
}
