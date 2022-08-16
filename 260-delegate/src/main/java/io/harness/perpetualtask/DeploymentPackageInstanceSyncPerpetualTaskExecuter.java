package io.harness.perpetualtask;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.DeploymentPackageServerInstanceInfo;
import io.harness.delegate.beans.instancesync.mapper.DeploymentPackageInstanceSyncPerpetualTaskResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.grpc.utils.AnyUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.shell.ExecuteCommandResponse;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ScriptType;
import io.harness.shell.ShellExecutorConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.SafeHttpCall.execute;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import io.harness.perpetualtask.instancesync.DeploymentPackageInstanceSyncPerpetualTaskParams;
import software.wings.api.shellscript.provision.ShellScriptProvisionExecutionData;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.sm.states.customdeployment.InstanceMapperUtils;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class DeploymentPackageInstanceSyncPerpetualTaskExecuter implements PerpetualTaskExecutor {
    private static final String SUCCESS_RESPONSE_MSG = "success";
    @Inject
    private ShellExecutorFactory shellExecutorFactory;
    @Inject private DelegateAgentManagerClient delegateAgentManagerClient;
    static Function<InstanceMapperUtils.HostProperties, DeploymentPackageServerInstanceInfo> jsonMapper = hostProperties
            -> DeploymentPackageServerInstanceInfo.builder()
            .hostId(hostProperties.getHostName())
            .hostName(hostProperties.getHostName())
            .properties(hostProperties.getOtherPropeties())
            .build();
    @Override
    public PerpetualTaskResponse runOnce(
            PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
        log.info("Running the DeploymentPackage InstanceSync perpetual task executor for task id: {}", taskId);
        DeploymentPackageInstanceSyncPerpetualTaskParams taskParams =
                AnyUtils.unpack(params.getCustomizedParams(), DeploymentPackageInstanceSyncPerpetualTaskParams.class);
        return executeDeploymentPackageInstanceSyncTask(taskId, taskParams);
    }

        private PerpetualTaskResponse executeDeploymentPackageInstanceSyncTask(
            PerpetualTaskId taskId, DeploymentPackageInstanceSyncPerpetualTaskParams taskParams) {

        final ShellScriptProvisionExecutionData response = executeScript(taskParams, taskId.getId());
        response.setStatus(response.getExecutionStatus());
        String scriptOutput = response.getOutput();
        final List<DeploymentPackageServerInstanceInfo> deploymentPackageserverInstanceInfos =
                    InstanceMapperUtils.mapJsonToInstanceElements(taskParams.getInstanceAttributesMap(),
                           taskParams.getInstancesListPath(), scriptOutput, jsonMapper);

        List<ServerInstanceInfo> serverInstanceInfos = deploymentPackageserverInstanceInfos.stream().map(ServerInstanceInfo.class::cast).collect(Collectors.toList());

        log.info("DeploymentPackage Instance sync nInstances: {}, task id: {}",
                isEmpty(serverInstanceInfos) ? 0 : serverInstanceInfos.size(), taskId);

        String instanceSyncResponseMsg = publishInstanceSyncResult(taskId, taskParams.getAccountId(), serverInstanceInfos);
        return PerpetualTaskResponse.builder().responseCode(SC_OK).responseMessage(instanceSyncResponseMsg).build();
    }
    private String publishInstanceSyncResult(
            PerpetualTaskId taskId, String accountId, List<ServerInstanceInfo> serverInstanceInfos) {
        DeploymentPackageInstanceSyncPerpetualTaskResponse instanceSyncResponse =
                DeploymentPackageInstanceSyncPerpetualTaskResponse.builder()
                        .serverInstanceDetails(serverInstanceInfos)
                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                        .build();
        try {
            execute(delegateAgentManagerClient.processInstanceSyncNGResult(taskId.getId(), accountId, instanceSyncResponse));
        } catch (Exception e) {
            String errorMsg = format(
                    "Failed to publish DeploymentPackage instance sync result PerpetualTaskId [%s], accountId [%s]", taskId.getId(), accountId);
            log.error(errorMsg + ", serverInstanceInfos: {}", serverInstanceInfos, e);
            return errorMsg;
        }
        return SUCCESS_RESPONSE_MSG;
    }
    private ShellScriptProvisionExecutionData executeScript(
            DeploymentPackageInstanceSyncPerpetualTaskParams taskParams, String taskId) {
        ShellScriptProvisionExecutionData response = null;
        String outputPath = null;
        try {
            outputPath = Files.createTempFile("customDeployment", "InstanceSync.json").toString();
            final ShellExecutorConfig shellExecutorConfig =
                    ShellExecutorConfig.builder()
                            .accountId(taskParams.getAccountId())
                            .environment(ImmutableMap.of(taskParams.getOutputPathKey(), outputPath))
                            .scriptType(ScriptType.BASH)
                            .executionId(taskId)
                            .commandUnitName("custom-deployment-instance-sync")
                            .build();

            final ScriptProcessExecutor executor = shellExecutorFactory.getExecutor(shellExecutorConfig);
            final ExecuteCommandResponse executeCommandResponse =
                    executor.executeCommandString(taskParams.getScript(), emptyList(), emptyList(), null);

            if (CommandExecutionStatus.SUCCESS == executeCommandResponse.getStatus()) {
                return ShellScriptProvisionExecutionData.builder()
                        .executionStatus(ExecutionStatus.SUCCESS)
                        .output(new String(Files.readAllBytes(Paths.get(outputPath)), StandardCharsets.UTF_8))
                        .build();
            } else {
                log.error("Error Occured While Running Custom Deployment Perpetual Task:{}", taskId);
                return ShellScriptProvisionExecutionData.builder().executionStatus(ExecutionStatus.FAILED).build();
            }
        } catch (Exception ex) {
            log.error("Exception Occured While Running Custom Deployment Perpetual Task:{}, Message: {}", taskId,
                    ExceptionUtils.getMessage(ex));
            return ShellScriptProvisionExecutionData.builder()
                    .executionStatus(ExecutionStatus.FAILED)
                    .errorMsg(ExceptionUtils.getMessage(ex))
                    .build();
        } finally {
            deleteSilently(outputPath);
        }
    }
    private void deleteSilently(String path) {
        if (path != null) {
            try {
                Files.deleteIfExists(Paths.get(path));
            } catch (IOException e) {
                log.error("Failed to delete file " + path, e);
            }
        }
    }

    @Override
    public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
        return false;
    }
}
