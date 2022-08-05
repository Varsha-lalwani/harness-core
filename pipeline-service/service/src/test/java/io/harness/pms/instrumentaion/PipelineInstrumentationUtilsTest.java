package io.harness.pms.instrumentaion;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.rule.Owner;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineInstrumentationUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void test() throws IOException {
    assertThat(PipelineInstrumentationUtils.getStageTypes(null)).isEmpty();
    PipelineEntity pipelineEntity = PipelineEntity.builder().build();
    assertThat(PipelineInstrumentationUtils.getStageTypes(pipelineEntity)).isEmpty();
    pipelineEntity.setYaml("abc");
    assertThat(PipelineInstrumentationUtils.getStageTypes(pipelineEntity)).isEmpty();
    pipelineEntity.setYaml("pipeline\n  stage1");
    assertThat(PipelineInstrumentationUtils.getStageTypes(pipelineEntity)).isEmpty();

    String yaml = Resources.toString(this.getClass().getClassLoader().getResource("pipeline.yml"), Charsets.UTF_8);
    pipelineEntity.setYaml(yaml);
    List<String> stageTypes = PipelineInstrumentationUtils.getStageTypes(pipelineEntity);
    assertThat(stageTypes).hasSize(1);
    assertThat(stageTypes.get(0)).isEqualTo("deployment");

    yaml = Resources.toString(
        this.getClass().getClassLoader().getResource("pipeline-with-two-stages.yaml"), Charsets.UTF_8);
    pipelineEntity.setYaml(yaml);
    stageTypes = PipelineInstrumentationUtils.getStageTypes(pipelineEntity);
    assertThat(stageTypes).hasSize(2);
    assertThat(stageTypes.get(0)).isEqualTo("deployment");
    assertThat(stageTypes.get(1)).isEqualTo("type2");
  }
}
