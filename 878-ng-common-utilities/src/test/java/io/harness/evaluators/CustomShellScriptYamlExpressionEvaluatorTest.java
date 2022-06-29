/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.evaluators;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceYamlDTO;
import io.harness.pms.yaml.YamlUtils;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CustomShellScriptYamlExpressionEvaluatorTest extends CategoryTest {
  private String yaml = "---\n"
      + "type: CustomShellScript\n"
      + "name: <+>\n"
      + "identifier: customShellScriptIdentifier\n"
      + "spec:\n"
      + "  shell: Bash\n"
      + "  onDelegate: 'true'\n"
      + "  source:\n"
      + "    __uuid: XXXuuid1XXX\n"
      + "    type: Inline\n"
      + "    spec:\n"
      + "      script: echo 1\n"
      + "  environmentVariables:\n"
      + "  - name: e1\n"
      + "    value: v1\n"
      + "    type: String\n"
      + "    __uuid: XXXuuid2XXX\n"
      + "  - name: e2\n"
      + "    value: v1\n"
      + "    type: String\n"
      + "    __uuid: XXXuuid3XXX\n"
      + "  outputVariables:\n"
      + "  - name: o1\n"
      + "    value: v1\n"
      + "    type: String\n"
      + "    __uuid: XXXuuid4XXX\n"
      + "  __uuid: XXXuuid1\5XXX\n"
      + "timeout: 10s\n"
      + "__uuid: XXXuuid6XXX";
}
