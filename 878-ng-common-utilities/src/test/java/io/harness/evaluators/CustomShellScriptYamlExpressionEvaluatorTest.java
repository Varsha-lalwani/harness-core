/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.evaluators;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CustomShellScriptBaseDTO;
import io.harness.beans.CustomShellScriptYamlDTO;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.template.TemplateEntityConstants;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
@OwnedBy(CDC)
public class CustomShellScriptYamlExpressionEvaluatorTest extends CategoryTest {
  private String yaml = "---\n"
      + "customShellScript:\n"
      + "  type: CustomShellScript\n"
      + "  name: customShellScriptStep\n"
      + "  identifier: customShellScriptStep\n"
      + "  spec:\n"
      + "    timeout: 10\n"
      + "    shell: Bash\n"
      + "    dummy: <+customShellScript.spec.environmentVariables.e1> \n"
      + "    onDelegate: 'true'\n"
      + "    source:\n"
      + "      __uuid: neREpx2mmQ14G7y3pKAQzW\n"
      + "      type: Inline\n"
      + "      spec:\n"
      + "        script: echo 1\n"
      + "    environmentVariables:\n"
      + "    - name: e1\n"
      + "      value: <+customShellScript.spec.environmentVariables.e2>\n"
      + "      type: String\n"
      + "      __uuid: 4G7y3pKAQzW-neREpx2mmQ1\n"
      + "    - name: e2\n"
      + "      value: dummyValue2\n"
      + "      type: String\n"
      + "      __uuid: 4G7y3pKAQzW-neREpx2mmQ2\n"
      + "    outputVariables:\n"
      + "    - name: o1\n"
      + "      value: v1\n"
      + "      type: String\n"
      + "      __uuid: 4G7y3pKAQzW-neREpx2mmQ3\n"
      + "    __uuid: M6HHtApvRa6cscRUnJ5NqA\n"
      + "  __uuid: xtkQAaoNRkCgtI5mU8KnEQ\n";

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testResolve() throws Exception {
    ShellScriptYamlExpressionEvaluator shellScriptYamlExpressionEvaluator =
        new ShellScriptYamlExpressionEvaluator(yaml);
    CustomShellScriptBaseDTO shellScriptBaseDTO =
        YamlUtils.read(yaml, CustomShellScriptYamlDTO.class).getCustomShellScriptBaseDTO();
    shellScriptBaseDTO =
        (CustomShellScriptBaseDTO) shellScriptYamlExpressionEvaluator.resolve(shellScriptBaseDTO, false);
    assertThat(shellScriptBaseDTO.getType()).isEqualTo(TemplateEntityConstants.CUSTOM_SHELL_SCRIPT);
    assertThat(shellScriptBaseDTO.getCustomShellScriptSpec().getDummy()).isEqualTo("dummyValue2");
  }
}
