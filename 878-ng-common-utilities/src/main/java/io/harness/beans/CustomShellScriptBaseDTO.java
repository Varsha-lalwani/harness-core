/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.beans.SwaggerConstants.BOOLEAN_CLASSPATH;
import static io.harness.beans.SwaggerConstants.INTEGER_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.*;

import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.core.VariableExpression;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("ShellScriptBaseStepInfo")
public class CustomShellScriptBaseDTO {
  @NotNull @ApiModelProperty(dataType = INTEGER_CLASSPATH) @YamlSchemaTypes({integer}) ParameterField<Integer> timeout;
  @JsonProperty("type") String type = "CUSTOM_SHELL_SCRIPT";
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;
  @NotNull ShellType shell;
  @NotNull
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH)
  @YamlSchemaTypes({string})
  ParameterField<Boolean> onDelegate;

  ExecutionTarget executionTarget;
  @YamlSchemaTypes(value = {runtime})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;
  @VariableExpression(skipVariableExpression = true) List<NGVariable> outputVariables;
  List<NGVariable> environmentVariables;
  @NotNull CustomShellScriptSourceWrapper source;
}
