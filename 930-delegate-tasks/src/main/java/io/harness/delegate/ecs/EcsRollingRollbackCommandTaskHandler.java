package io.harness.delegate.ecs;

import com.google.inject.Inject;
import io.harness.delegate.beans.ecs.EcsRollingDeployResult;
import io.harness.delegate.beans.ecs.EcsRollingRollbackResult;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.exception.EcsNGException;
import io.harness.delegate.task.ecs.EcsCommandTaskNGHelper;
import io.harness.delegate.task.ecs.EcsInfraConfig;
import io.harness.delegate.task.ecs.EcsInfraConfigHelper;
import io.harness.delegate.task.ecs.EcsRollingRollbackConfig;
import io.harness.delegate.task.ecs.EcsTaskHelperBase;
import io.harness.delegate.task.ecs.request.EcsCommandRequest;
import io.harness.delegate.task.ecs.request.EcsRollingRollbackRequest;
import io.harness.delegate.task.ecs.response.EcsCommandResponse;
import io.harness.delegate.task.ecs.response.EcsRollingDeployResponse;
import io.harness.delegate.task.ecs.response.EcsRollingRollbackResponse;
import io.harness.ecs.EcsCommandUnitConstants;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.ecs.model.CreateServiceRequest;
import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;

import java.util.List;

import static java.lang.String.format;
import static software.wings.beans.LogHelper.color;

public class EcsRollingRollbackCommandTaskHandler extends EcsCommandTaskNGHandler {

  @Inject private EcsTaskHelperBase ecsTaskHelperBase;
  @Inject
  private EcsInfraConfigHelper ecsInfraConfigHelper;
  @Inject private EcsCommandTaskNGHelper ecsCommandTaskHelper;

  private EcsInfraConfig ecsInfraConfig;
  private long timeoutInMillis;

  @Override
  protected EcsCommandResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest, ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(ecsCommandRequest instanceof EcsRollingRollbackRequest)) {
      throw new InvalidArgumentsException(
              Pair.of("ecsCommandRequest", "Must be instance of EcsRollingRollbackRequest"));
    }

    EcsRollingRollbackRequest ecsRollingRollbackRequest = (EcsRollingRollbackRequest) ecsCommandRequest;
    timeoutInMillis = ecsRollingRollbackRequest.getTimeoutIntervalInMin() * 60000;
    ecsInfraConfig = ecsRollingRollbackRequest.getEcsInfraConfig();

    LogCallback rollbackLogCallback = ecsTaskHelperBase.getLogCallback(
            iLogStreamingTaskClient, EcsCommandUnitConstants.rollback.toString(), true, commandUnitsProgress);

    try {
      EcsRollingRollbackConfig ecsRollingRollbackConfig = ecsRollingRollbackRequest.getEcsRollingRollbackConfig();


      CreateServiceRequest.Builder createServiceRequestBuilder = ecsCommandTaskHelper.parseYamlAsObject(ecsRollingRollbackConfig.getCreateServiceRequestBuilderString(),
              CreateServiceRequest.serializableBuilderClass());
      // replace cluster
      CreateServiceRequest createServiceRequest = createServiceRequestBuilder.cluster(ecsInfraConfig.getCluster()).build();

      rollback(createServiceRequest,
              ecsRollingRollbackConfig.getRegisterScalableTargetRequestBuilderStrings(),
              ecsRollingRollbackConfig.getRegisterScalingPolicyRequestBuilderStrings(),
              ecsInfraConfig,
              rollbackLogCallback, timeoutInMillis);
      EcsRollingRollbackResult ecsRollingRollbackResult = EcsRollingRollbackResult.builder()
              .region(ecsInfraConfig.getRegion())
              .ecsTasks(ecsCommandTaskHelper.getRunningEcsTasks(ecsInfraConfig.getAwsConnectorDTO(), ecsInfraConfig.getCluster(),
                      createServiceRequest.serviceName(), ecsInfraConfig.getRegion()))
              .infrastructureKey(ecsInfraConfig.getInfraStructureKey())
              .build();
      EcsRollingRollbackResponse ecsRollingRollbackResponse =
              EcsRollingRollbackResponse.builder()
                      .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                      .ecsRollingRollbackResult(ecsRollingRollbackResult)
                      .build();

      rollbackLogCallback.saveExecutionLog(color(format("%n Rollback Successful."), LogColor.Green, LogWeight.Bold),
              LogLevel.INFO, CommandExecutionStatus.SUCCESS);

      return ecsRollingRollbackResponse;
    } catch (Exception ex) {
      rollbackLogCallback.saveExecutionLog(color(format("%n Rollback Failed."), LogColor.Red, LogWeight.Bold),
              LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw new EcsNGException(ex);
    }
  }

  private void rollback(CreateServiceRequest createServiceRequest,
                                      List<String> ecsScalableTargetManifestContentList,
                                      List<String> ecsScalingPolicyManifestContentList,
                                      EcsInfraConfig ecsInfraConfig,
                                      LogCallback rollbackLogCallback, long timeoutInMillis) {
    ecsCommandTaskHelper.createOrUpdateService(createServiceRequest, ecsScalableTargetManifestContentList,
            ecsScalingPolicyManifestContentList, ecsInfraConfig, rollbackLogCallback, timeoutInMillis);
  }
}
