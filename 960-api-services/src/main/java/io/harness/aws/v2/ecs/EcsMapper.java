package io.harness.aws.v2.ecs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.harness.annotations.dev.OwnedBy;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.applicationautoscaling.model.PutScalingPolicyRequest;
import software.amazon.awssdk.services.applicationautoscaling.model.RegisterScalableTargetRequest;
import software.amazon.awssdk.services.applicationautoscaling.model.ScalableTarget;
import software.amazon.awssdk.services.applicationautoscaling.model.ScalingPolicy;
import software.amazon.awssdk.services.ecs.model.CreateServiceRequest;
import software.amazon.awssdk.services.ecs.model.Service;
import software.amazon.awssdk.services.ecs.model.UpdateServiceRequest;

import javax.inject.Singleton;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class EcsMapper {
    public UpdateServiceRequest createServiceRequestToUpdateServiceRequest(CreateServiceRequest createServiceRequest) {
        return UpdateServiceRequest.builder()
                .service(createServiceRequest.serviceName())
                .cluster(createServiceRequest.cluster())
                .desiredCount(createServiceRequest.desiredCount())
                .taskDefinition(createServiceRequest.taskDefinition())
                .capacityProviderStrategy(createServiceRequest.capacityProviderStrategy())
                .deploymentConfiguration(createServiceRequest.deploymentConfiguration())
                .networkConfiguration(createServiceRequest.networkConfiguration())
                .placementConstraints(createServiceRequest.placementConstraints())
                .placementStrategy(createServiceRequest.placementStrategy())
                .platformVersion(createServiceRequest.platformVersion())
                .forceNewDeployment(false) // need to confirm with Sainath
                .healthCheckGracePeriodSeconds(createServiceRequest.healthCheckGracePeriodSeconds())
                .enableExecuteCommand(createServiceRequest.enableExecuteCommand())
                .enableECSManagedTags(createServiceRequest.enableECSManagedTags())
                .loadBalancers(createServiceRequest.loadBalancers())
                .propagateTags(createServiceRequest.propagateTags())
                .serviceRegistries(createServiceRequest.serviceRegistries())
                .build();

    }

    public String createCreateServiceRequestFromService(Service service) throws JsonProcessingException {
        CreateServiceRequest.Builder createServiceRequestBuilder = CreateServiceRequest.builder()
                .serviceName(service.serviceName())
                .taskDefinition(service.taskDefinition())
                .capacityProviderStrategy(service.capacityProviderStrategy())
                .serviceRegistries(service.serviceRegistries())
                .deploymentConfiguration(service.deploymentConfiguration())
                .deploymentController(service.deploymentController())
                .desiredCount(service.desiredCount())
                .enableECSManagedTags(service.enableECSManagedTags())
                .enableExecuteCommand(service.enableExecuteCommand())
                .healthCheckGracePeriodSeconds(service.healthCheckGracePeriodSeconds())
                .launchType(service.launchType())
                .networkConfiguration(service.networkConfiguration())
                .loadBalancers(service.loadBalancers())
                .placementConstraints(service.placementConstraints())
                .placementStrategy(service.placementStrategy())
                .platformVersion(service.platformVersion())
                .propagateTags(service.propagateTags())
                .role(service.roleArn())
                .tags(service.tags());

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper.writeValueAsString(createServiceRequestBuilder);
    }

    public String createRegisterScalableTargetRequestFromScalableTarget(ScalableTarget scalableTarget) throws JsonProcessingException {
        RegisterScalableTargetRequest.Builder registerScalableTargetRequestBuilder = RegisterScalableTargetRequest.builder()
                .maxCapacity(scalableTarget.maxCapacity())
                .minCapacity(scalableTarget.minCapacity())
                .roleARN(scalableTarget.roleARN())
                .scalableDimension(scalableTarget.scalableDimension())
                .serviceNamespace(scalableTarget.serviceNamespace())
                .suspendedState(scalableTarget.suspendedState());
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper.writeValueAsString(registerScalableTargetRequestBuilder);
    }

    public String createPutScalingPolicyRequestFromScalingPolicy(ScalingPolicy scalingPolicy) throws JsonProcessingException {
        PutScalingPolicyRequest.Builder putScalingPolicyRequestRequestBuilder = PutScalingPolicyRequest.builder()
                .stepScalingPolicyConfiguration(scalingPolicy.stepScalingPolicyConfiguration())
                .targetTrackingScalingPolicyConfiguration(scalingPolicy.targetTrackingScalingPolicyConfiguration())
                .policyName(scalingPolicy.policyName())
                .policyType(scalingPolicy.policyType())
                .scalableDimension(scalingPolicy.scalableDimension())
                .serviceNamespace(scalingPolicy.serviceNamespace());

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper.writeValueAsString(putScalingPolicyRequestRequestBuilder);
    }

}
