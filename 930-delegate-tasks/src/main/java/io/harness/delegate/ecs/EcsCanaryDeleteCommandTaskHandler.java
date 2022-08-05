package io.harness.delegate.ecs;

import com.google.inject.Inject;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.ecs.EcsCommandTaskNGHelper;
import io.harness.delegate.task.ecs.EcsInfraConfig;
import io.harness.delegate.task.ecs.EcsInfraConfigHelper;
import io.harness.delegate.task.ecs.EcsTaskHelperBase;
import io.harness.delegate.task.ecs.request.EcsCanaryDeleteRequest;
import io.harness.delegate.task.ecs.request.EcsCommandRequest;
import io.harness.delegate.task.ecs.response.EcsCanaryDeleteResponse;
import io.harness.delegate.task.ecs.response.EcsCommandResponse;
import io.harness.ecs.EcsCommandUnitConstants;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.ecs.model.CreateServiceRequest;
import software.amazon.awssdk.services.ecs.model.Service;

import static java.lang.String.format;

import java.util.Optional;

public class EcsCanaryDeleteCommandTaskHandler extends EcsCommandTaskNGHandler {

  @Inject private EcsTaskHelperBase ecsTaskHelperBase;
  @Inject
  private EcsInfraConfigHelper ecsInfraConfigHelper;
  @Inject private EcsCommandTaskNGHelper ecsCommandTaskHelper;

  private EcsInfraConfig ecsInfraConfig;
  private long timeoutInMillis;

  @Override
  protected EcsCommandResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest, ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(ecsCommandRequest instanceof EcsCanaryDeleteRequest)) {
      throw new InvalidArgumentsException(
              Pair.of("ecsCommandRequest", "Must be instance of EcsCanaryDeleteRequest"));
    }

    EcsCanaryDeleteRequest ecsCanaryDeleteRequest = (EcsCanaryDeleteRequest) ecsCommandRequest;
    timeoutInMillis = ecsCanaryDeleteRequest.getTimeoutIntervalInMin() * 60000;
    ecsInfraConfig = ecsCanaryDeleteRequest.getEcsInfraConfig();

    LogCallback canaryDeleteLogCallback = ecsTaskHelperBase.getLogCallback(
            iLogStreamingTaskClient, EcsCommandUnitConstants.deleteService.toString(), true, commandUnitsProgress);

    String ecsServiceDefinitionManifestContent = ecsCanaryDeleteRequest.getEcsServiceDefinitionManifestContent();

    CreateServiceRequest.Builder createServiceRequestBuilder = ecsCommandTaskHelper.parseYamlAsObject(ecsServiceDefinitionManifestContent, CreateServiceRequest.serializableBuilderClass());

    CreateServiceRequest createServiceRequest = createServiceRequestBuilder.build();

    String serviceName = createServiceRequest.serviceName();

    String canaryServiceName = serviceName + "Canary";

    Optional<Service> optionalService = ecsCommandTaskHelper.describeService(createServiceRequest.cluster(), canaryServiceName, ecsInfraConfig.getRegion(), ecsInfraConfig.getAwsConnectorDTO());

    if (optionalService.isPresent() && ecsCommandTaskHelper.isServiceActive(optionalService.get())) {
      ecsCommandTaskHelper.deleteService(canaryServiceName, ecsInfraConfig.getCluster(), ecsInfraConfig.getRegion(), ecsInfraConfig.getAwsConnectorDTO());
      canaryDeleteLogCallback.saveExecutionLog(format("Canary service %s deleted", canaryServiceName), LogLevel.INFO, CommandExecutionStatus.SUCCESS);

      return EcsCanaryDeleteResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    } else {
      canaryDeleteLogCallback.saveExecutionLog(format("Canary service %s doesn't exist", canaryServiceName), LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      return EcsCanaryDeleteResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    }

  }

}
