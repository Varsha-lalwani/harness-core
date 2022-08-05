package io.harness.aws.v2.ecs;


import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.v2.AwsClientHelper;
import io.harness.aws.beans.AwsInternalConfig;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.applicationautoscaling.ApplicationAutoScalingClient;
import software.amazon.awssdk.services.applicationautoscaling.model.DeleteScalingPolicyRequest;
import software.amazon.awssdk.services.applicationautoscaling.model.DeleteScalingPolicyResponse;
import software.amazon.awssdk.services.applicationautoscaling.model.DeregisterScalableTargetRequest;
import software.amazon.awssdk.services.applicationautoscaling.model.DeregisterScalableTargetResponse;
import software.amazon.awssdk.services.applicationautoscaling.model.DescribeScalableTargetsRequest;
import software.amazon.awssdk.services.applicationautoscaling.model.DescribeScalableTargetsResponse;
import software.amazon.awssdk.services.applicationautoscaling.model.DescribeScalingPoliciesRequest;
import software.amazon.awssdk.services.applicationautoscaling.model.DescribeScalingPoliciesResponse;
import software.amazon.awssdk.services.applicationautoscaling.model.PutScalingPolicyRequest;
import software.amazon.awssdk.services.applicationautoscaling.model.PutScalingPolicyResponse;
import software.amazon.awssdk.services.applicationautoscaling.model.RegisterScalableTargetRequest;
import software.amazon.awssdk.services.applicationautoscaling.model.RegisterScalableTargetResponse;
import software.amazon.awssdk.services.ecs.model.CreateServiceRequest;
import software.amazon.awssdk.services.ecs.model.CreateServiceResponse;
import software.amazon.awssdk.services.ecs.model.DeleteServiceRequest;
import software.amazon.awssdk.services.ecs.model.DeleteServiceResponse;
import software.amazon.awssdk.services.ecs.model.DescribeServicesRequest;
import software.amazon.awssdk.services.ecs.model.DescribeServicesResponse;
import software.amazon.awssdk.services.ecs.model.DescribeTaskDefinitionRequest;
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest;
import software.amazon.awssdk.services.ecs.model.DescribeTasksResponse;
import software.amazon.awssdk.services.ecs.model.ListTasksRequest;
import software.amazon.awssdk.services.ecs.model.ListTasksResponse;
import software.amazon.awssdk.services.ecs.model.RegisterTaskDefinitionRequest;
import software.amazon.awssdk.services.ecs.model.RegisterTaskDefinitionResponse;
import software.amazon.awssdk.services.ecs.model.Service;
import software.amazon.awssdk.services.ecs.model.TaskDefinition;
import software.amazon.awssdk.services.ecs.model.UpdateServiceRequest;
import software.amazon.awssdk.services.ecs.model.UpdateServiceResponse;
import software.amazon.awssdk.services.ecs.waiters.EcsWaiter;
import software.amazon.awssdk.services.ecs.EcsClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class EcsV2ClientImpl extends AwsClientHelper implements EcsV2Client {

    @Override
    public CreateServiceResponse createService(AwsInternalConfig awsConfig, CreateServiceRequest createServiceRequest, String region) {
        try(EcsClient ecsClient = (EcsClient)getClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return ecsClient.createService(createServiceRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return CreateServiceResponse.builder().build();
    }

    @Override
    public UpdateServiceResponse updateService(AwsInternalConfig awsConfig, UpdateServiceRequest updateServiceRequest, String region) {
        try(EcsClient ecsClient = (EcsClient)getClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return ecsClient.updateService(updateServiceRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return UpdateServiceResponse.builder().build();
    }

    @Override
    public DeleteServiceResponse deleteService(AwsInternalConfig awsConfig, DeleteServiceRequest deleteServiceRequest, String region) {
        try(EcsClient ecsClient = getEcsClient(awsConfig, region)) {
            return ecsClient.deleteService(deleteServiceRequest);
        }
        catch(AwsServiceException awsServiceException) {
            awsApiV2ExceptionHandler.handleAwsServiceException(awsServiceException);
        }
        catch(SdkException sdkException) {
            awsApiV2ExceptionHandler.handleSdkException(sdkException);
        }
        catch(Exception exception) {
            awsApiV2ExceptionHandler.handleException(exception);
        }
        return DeleteServiceResponse.builder().build();
    }

    @Override
    public RegisterTaskDefinitionResponse createTask(AwsInternalConfig awsConfig, RegisterTaskDefinitionRequest registerTaskDefinitionRequest, String region) {
        try(EcsClient ecsClient = (EcsClient)getClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return ecsClient.registerTaskDefinition(registerTaskDefinitionRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return RegisterTaskDefinitionResponse.builder().build();
    }

    @Override
    public  WaiterResponse<DescribeServicesResponse> ecsServiceSteadyStateCheck(AwsInternalConfig awsConfig,
                                           DescribeServicesRequest describeServicesRequest, String region,
                                           int serviceSteadyStateTimeout) {
        // Polling interval of 10 sec with total waiting done till a timeout of <serviceSteadyStateTimeout> min
        int delayInSeconds=10;
        int maxAttempts = (int) TimeUnit.MINUTES.toSeconds(serviceSteadyStateTimeout) / delayInSeconds;
        try(EcsClient ecsClient = (EcsClient)getClient(awsConfig, region);
            EcsWaiter ecsWaiter = getEcsWaiter(ecsClient, delayInSeconds, maxAttempts)){
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return ecsWaiter.waitUntilServicesStable(describeServicesRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return null;
    }

    @Override
    public RegisterScalableTargetResponse registerScalableTarget(AwsInternalConfig awsConfig,
                                                                 RegisterScalableTargetRequest registerScalableTargetRequest, String region) {
       try(ApplicationAutoScalingClient applicationAutoScalingClient =
                   getApplicationAutoScalingClient(awsConfig, region)) {
           super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
           return applicationAutoScalingClient.registerScalableTarget(registerScalableTargetRequest);
       }
       catch(Exception exception) {
           super.handleException(exception);
       }
       return RegisterScalableTargetResponse.builder().build();
    }

    @Override
    public DeregisterScalableTargetResponse deregisterScalableTarget(AwsInternalConfig awsConfig,
                                                                     DeregisterScalableTargetRequest deregisterScalableTargetRequest, String region) {
        try(ApplicationAutoScalingClient applicationAutoScalingClient =
                    getApplicationAutoScalingClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return applicationAutoScalingClient.deregisterScalableTarget(deregisterScalableTargetRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return DeregisterScalableTargetResponse.builder().build();
    }

    @Override
    public PutScalingPolicyResponse attachScalingPolicy(AwsInternalConfig awsConfig, PutScalingPolicyRequest putScalingPolicyRequest, String region) {
        try(ApplicationAutoScalingClient applicationAutoScalingClient =
                    getApplicationAutoScalingClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return applicationAutoScalingClient.putScalingPolicy(putScalingPolicyRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return PutScalingPolicyResponse.builder().build();
    }

    @Override
    public DeleteScalingPolicyResponse deleteScalingPolicy(AwsInternalConfig awsConfig, DeleteScalingPolicyRequest deleteScalingPolicyRequest, String region) {
        try(ApplicationAutoScalingClient applicationAutoScalingClient =
                    getApplicationAutoScalingClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return applicationAutoScalingClient.deleteScalingPolicy(deleteScalingPolicyRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return DeleteScalingPolicyResponse.builder().build();
    }

    @Override
    public DescribeScalableTargetsResponse listScalableTargets(AwsInternalConfig awsConfig,
                                                               DescribeScalableTargetsRequest describeScalableTargetsRequest, String region) {
        try(ApplicationAutoScalingClient applicationAutoScalingClient =
                    getApplicationAutoScalingClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return applicationAutoScalingClient.describeScalableTargets(describeScalableTargetsRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return null;
    }

    @Override
    public DescribeScalingPoliciesResponse listScalingPolicies(AwsInternalConfig awsConfig,
                                                               DescribeScalingPoliciesRequest describeScalingPoliciesRequest, String region) {
        try(ApplicationAutoScalingClient applicationAutoScalingClient =
                    getApplicationAutoScalingClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return applicationAutoScalingClient.describeScalingPolicies(describeScalingPoliciesRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return DescribeScalingPoliciesResponse.builder().build();
    }

    @Override
    public DescribeServicesResponse describeService(AwsInternalConfig awsConfig, String clusterName, String serviceName, String region) {
        DescribeServicesRequest describeServicesRequest = DescribeServicesRequest.builder()
                .services(Collections.singletonList(serviceName))
                .cluster(clusterName)
                .build();

        try(EcsClient ecsClient = (EcsClient)getClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return ecsClient.describeServices(describeServicesRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return null;
    }

    @Override
    public TaskDefinition getTaskDefinitionForService(AwsInternalConfig awsConfig, Service service, String region) {
        DescribeTaskDefinitionRequest describeTaskDefinitionRequest = DescribeTaskDefinitionRequest.builder()
                .taskDefinition(service.taskDefinition())
                .build();
        try(EcsClient ecsClient = (EcsClient)getClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return ecsClient.describeTaskDefinition(describeTaskDefinitionRequest).taskDefinition();
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return TaskDefinition.builder().build();
    }

    @Override
    public ListTasksResponse listTaskArns(AwsInternalConfig awsConfig, ListTasksRequest listTasksRequest,
                                          String region) {

        try(EcsClient ecsClient = (EcsClient)getClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return ecsClient.listTasks(listTasksRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return ListTasksResponse.builder().build();
    }

    @Override
    public DescribeTasksResponse getTasks(AwsInternalConfig awsConfig, String clusterName, List<String> taskArns,
                                          String region) {
        DescribeTasksRequest describeTasksRequest = DescribeTasksRequest.builder()
                .cluster(clusterName)
                .tasks(taskArns)
                .build();
        try(EcsClient ecsClient = (EcsClient)getClient(awsConfig, region)) {
            super.logCall(client(), Thread.currentThread().getStackTrace()[1].getMethodName());
            return ecsClient.describeTasks(describeTasksRequest);
        }
        catch(Exception exception) {
            super.handleException(exception);
        }
        return DescribeTasksResponse.builder().build();
    }

    private EcsWaiter getEcsWaiter(software.amazon.awssdk.services.ecs.EcsClient ecsClient, int delayInSeconds, int maxAttempts) {
        return EcsWaiter.builder()
                .client(ecsClient)
                .overrideConfiguration(WaiterOverrideConfiguration.builder()
                        .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(delayInSeconds)))
                        .maxAttempts(maxAttempts)
                        .build())
                .build();
    }

    private ApplicationAutoScalingClient getApplicationAutoScalingClient(AwsInternalConfig awsConfig, String region) {
        return ApplicationAutoScalingClient.builder()
                .credentialsProvider(getAwsCredentialsProvider(awsConfig))
                .region(Region.of(region))
                .overrideConfiguration(getClientOverrideConfiguration(awsConfig))
                .build();
    }

    @Override
    public SdkClient getClient(AwsInternalConfig awsConfig, String region) {
        return software.amazon.awssdk.services.ecs.EcsClient.builder()
                .credentialsProvider(getAwsCredentialsProvider(awsConfig))
                .region(Region.of(region))
                .overrideConfiguration(getClientOverrideConfiguration(awsConfig))
                .build();
    }

    @Override
    public String client() {
        return "ECS";
    }
}
