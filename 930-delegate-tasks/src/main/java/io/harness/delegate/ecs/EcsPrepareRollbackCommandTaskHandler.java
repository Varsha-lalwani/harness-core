package io.harness.delegate.ecs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import io.harness.aws.v2.ecs.EcsMapper;
import io.harness.delegate.beans.ecs.EcsPrepareRollbackDataResult;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.ecs.EcsCommandTaskNGHelper;
import io.harness.delegate.task.ecs.EcsInfraConfig;
import io.harness.delegate.task.ecs.EcsTaskHelperBase;
import io.harness.delegate.task.ecs.request.EcsCommandRequest;
import io.harness.delegate.task.ecs.request.EcsPrepareRollbackDataRequest;
import io.harness.delegate.task.ecs.response.EcsCommandResponse;
import io.harness.delegate.task.ecs.response.EcsPrepareRollbackDataResponse;
import io.harness.ecs.EcsCommandUnitConstants;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.serializer.YamlUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.applicationautoscaling.model.DescribeScalableTargetsResponse;
import software.amazon.awssdk.services.applicationautoscaling.model.DescribeScalingPoliciesResponse;
import software.amazon.awssdk.services.ecs.model.CreateServiceRequest;
import software.amazon.awssdk.services.ecs.model.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class EcsPrepareRollbackCommandTaskHandler extends EcsCommandTaskNGHandler {
  @Inject
  private EcsCommandTaskNGHelper ecsCommandTaskHelper;
  @Inject
  EcsMapper ecsMapper;
  @Inject private EcsTaskHelperBase ecsTaskHelperBase;

  private EcsInfraConfig ecsInfraConfig;
  private long timeoutInMillis;

  @Override
  protected EcsCommandResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest, ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(ecsCommandRequest instanceof EcsPrepareRollbackDataRequest)) {
      throw new InvalidArgumentsException(
              Pair.of("ecsCommandRequest", "Must be instance of EcsPrepareRollbackRequest"));
    }

    EcsPrepareRollbackDataRequest ecsPrepareRollbackRequest = (EcsPrepareRollbackDataRequest) ecsCommandRequest;

    timeoutInMillis = ecsPrepareRollbackRequest.getTimeoutIntervalInMin() * 60000;
    ecsInfraConfig = ecsPrepareRollbackRequest.getEcsInfraConfig();

    LogCallback prepareRollbackDataLogCallback = ecsTaskHelperBase.getLogCallback(
            iLogStreamingTaskClient, EcsCommandUnitConstants.prepareRollbackData.toString(), true, commandUnitsProgress);

    // Get Ecs Service Name
    String ecsServiceDefinitionManifestContent = ecsPrepareRollbackRequest.getEcsServiceDefinitionManifestContent();
    CreateServiceRequest.Builder createServiceRequestBuilder = ecsCommandTaskHelper.parseYamlAsObject(ecsServiceDefinitionManifestContent, CreateServiceRequest.serializableBuilderClass());
    CreateServiceRequest createServiceRequest = createServiceRequestBuilder.build();
    String serviceName = createServiceRequest.serviceName();

    prepareRollbackDataLogCallback.saveExecutionLog(format("Fetching Service Definition Details for Service %s..", serviceName), LogLevel.INFO);
    // Describe ecs service and get service details
    Optional<Service> optionalService = ecsCommandTaskHelper.describeService(ecsInfraConfig.getCluster(), serviceName, ecsInfraConfig.getRegion(), ecsInfraConfig.getAwsConnectorDTO());

    if ((optionalService.isPresent() && ecsCommandTaskHelper.isServiceActive(optionalService.get()))) { // If service exists
      Service service = optionalService.get();

      // Get createServiceRequestBuilderString from service
      String createServiceRequestBuilderString = ecsMapper.createCreateServiceRequestFromService(service);
      prepareRollbackDataLogCallback.saveExecutionLog(format("Fetched Service Definition Details for Service %s", serviceName), LogLevel.INFO);

      // Get registerScalableTargetRequestBuilderStrings if present
      prepareRollbackDataLogCallback.saveExecutionLog(format("Fetching Scalable Target Details for Service %s..", serviceName), LogLevel.INFO);
      DescribeScalableTargetsResponse describeScalableTargetsResponse = ecsCommandTaskHelper.listScalableTargets(ecsInfraConfig.getAwsConnectorDTO(), service.serviceArn(), ecsInfraConfig.getRegion());


      List<String> registerScalableTargetRequestBuilderStrings = null;
      if (describeScalableTargetsResponse != null && CollectionUtils.isNotEmpty(describeScalableTargetsResponse.scalableTargets())) {
        registerScalableTargetRequestBuilderStrings = describeScalableTargetsResponse.scalableTargets().stream().map(scalableTarget -> {
          try {
            return ecsMapper.createRegisterScalableTargetRequestFromScalableTarget(scalableTarget);
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
          return null;
        }).collect(Collectors.toList());
        prepareRollbackDataLogCallback.saveExecutionLog(format("Fetched Scalable Target Details for Service %s", serviceName), LogLevel.INFO);
      } else {
        prepareRollbackDataLogCallback.saveExecutionLog(format("Didn't find Scalable Target Details for Service %s", serviceName), LogLevel.INFO);
      }

      // Get registerScalingPolicyRequestBuilderStrings if present
      prepareRollbackDataLogCallback.saveExecutionLog(format("Fetching Scaling Policy Details for Service %s..", serviceName), LogLevel.INFO);
      DescribeScalingPoliciesResponse describeScalingPoliciesResponse = ecsCommandTaskHelper.listScalingPolicies(ecsInfraConfig.getAwsConnectorDTO(), service.serviceArn(), ecsInfraConfig.getRegion());

      List<String> registerScalingPolicyRequestBuilderStrings = null;
      if (describeScalingPoliciesResponse != null && CollectionUtils.isNotEmpty(describeScalingPoliciesResponse.scalingPolicies())) {
        registerScalingPolicyRequestBuilderStrings = describeScalingPoliciesResponse.scalingPolicies().stream().map(scalingPolicy -> {
          try {
            return ecsMapper.createPutScalingPolicyRequestFromScalingPolicy(scalingPolicy);
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
          return null;
        }).collect(Collectors.toList());
        prepareRollbackDataLogCallback.saveExecutionLog(format("Fetched Scaling Policy Details for Service %s", serviceName), LogLevel.INFO);
      } else {
        prepareRollbackDataLogCallback.saveExecutionLog(format("Didn't find Scaling Policy Details for Service %s", serviceName), LogLevel.INFO);
      }

      EcsPrepareRollbackDataResult ecsPrepareRollbackDataResult = EcsPrepareRollbackDataResult.builder()
              .isFirstDeployment(false)
              .createServiceRequestBuilderString(createServiceRequestBuilderString)
              .registerScalableTargetRequestBuilderStrings(registerScalableTargetRequestBuilderStrings)
              .registerScalingPolicyRequestBuilderStrings(registerScalingPolicyRequestBuilderStrings)
              .build();

      EcsPrepareRollbackDataResponse ecsPrepareRollbackDataResponse = EcsPrepareRollbackDataResponse.builder()
              .ecsPrepareRollbackDataResult(ecsPrepareRollbackDataResult)
              .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
              .build();

      prepareRollbackDataLogCallback.saveExecutionLog(format("Preparing Rollback Data complete"), LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      return ecsPrepareRollbackDataResponse;

    } else { // If service doesn't exist
      prepareRollbackDataLogCallback.saveExecutionLog(format("Service %s doesn't exist. Skipping Prepare Rollback Data..", serviceName)
              , LogLevel.INFO, CommandExecutionStatus.SUCCESS);

      // Send EcsPrepareRollbackDataResult with isFirstDeployment as true
      EcsPrepareRollbackDataResult ecsPrepareRollbackDataResult = EcsPrepareRollbackDataResult.builder()
              .isFirstDeployment(true)
              .build();

      EcsPrepareRollbackDataResponse ecsPrepareRollbackResponse = EcsPrepareRollbackDataResponse.builder()
              .ecsPrepareRollbackDataResult(ecsPrepareRollbackDataResult)
              .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
              .build();

      return ecsPrepareRollbackResponse;
    }

  }
}
