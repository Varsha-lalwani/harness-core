package io.harness.cdng.infra.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("deploymentTemplateInfraMapping")
@JsonTypeName("deploymentTemplateInfraMapping")
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.infra.beans.DeploymentTemplateInfraMapping")
public class DeploymentTemplateInfraMapping implements InfraMapping {
  @Id private String uuid;
  private String accountId;
  Map<String, String> variables;
}
