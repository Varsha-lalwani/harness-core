package io.harness.cdng.infra.yaml;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.infra.beans.DeploymentTemplateInfraMapping;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.InfrastructureDetailsAbstract;
import io.harness.filters.ConnectorRefExtractorHelper;
import io.harness.filters.WithConnectorRef;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(InfrastructureKind.DEPLOYMENT_TEMPLATE)
@SimpleVisitorHelper(helperClass = ConnectorRefExtractorHelper.class)
@TypeAlias("deploymentTemplateInfrastructure")
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.infra.yaml.DeploymentTemplateInfrastructure")
public class DeploymentTemplateInfrastructure
    extends InfrastructureDetailsAbstract implements Infrastructure, Visitable, WithConnectorRef {
  @NotNull
  @NotEmpty
  @ApiModelProperty(dataType = SwaggerConstants.STRING_MAP_CLASSPATH)
  @Wither
  ParameterField<Map<String, String>> variables;

  @Override
  public InfraMapping getInfraMapping() {
    return DeploymentTemplateInfraMapping.builder().variables(variables.getValue()).build();
  }

  @Override
  public ParameterField<String> getConnectorReference() {
    // TODO - need to figure out connector ref details
    return null;
  }

  @Override
  public String[] getInfrastructureKeyValues() {
    return variables.getValue().keySet().toArray(new String[0]);
  }

  @Override
  public String getKind() {
    return InfrastructureKind.DEPLOYMENT_TEMPLATE;
  }

  @Override
  public Infrastructure applyOverrides(Infrastructure overrideConfig) {
    DeploymentTemplateInfrastructure config = (DeploymentTemplateInfrastructure) overrideConfig;
    DeploymentTemplateInfrastructure resultantInfra = this;
    if (!ParameterField.isNull(config.getVariables())) {
      resultantInfra = resultantInfra.withVariables(config.getVariables());
    }
    return resultantInfra;
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    // TODO - need to figure out connector ref details
    // connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }
}
