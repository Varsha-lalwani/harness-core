package io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.DeploymentPackage;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Any;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.DeploymentPackageDeploymentInfoDTO;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.instancesync.DeploymentPackageInstanceSyncPerpetualTaskParams;
import io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.InstanceSyncPerpetualTaskHandler;
import lombok.AllArgsConstructor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.expression.SecretFunctor;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;



@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject}))
@OwnedBy(HarnessTeam.CDP)
public class DeploymentPackageInstanceSyncPerpetualTaskHandler extends InstanceSyncPerpetualTaskHandler {
    public static final String OUTPUT_PATH_KEY = "INSTANCE_OUTPUT_PATH";
    @Inject private ManagerExpressionEvaluator expressionEvaluator;
    @Inject private ManagerDecryptionService managerDecryptionService;
    @Inject private SecretManager secretManager;
    @Override
    public PerpetualTaskExecutionBundle getExecutionBundle(InfrastructureMappingDTO infrastructureMappingDTO,
                                                           List<DeploymentInfoDTO> deploymentInfoDTOList, InfrastructureOutcome infrastructureOutcome) {

        DeploymentPackageDeploymentInfoDTO deploymentPackageDeploymentInfoDTO = (DeploymentPackageDeploymentInfoDTO) deploymentInfoDTOList.get(0);
        Any perpetualTaskPack =
                packDeploymentPackageInstanceSyncPerpetualTaskParams(infrastructureMappingDTO, infrastructureOutcome, deploymentPackageDeploymentInfoDTO);



        List<ExecutionCapability> executionCapabilities = getExecutionCapability(deploymentInfoDTOList);

        return createPerpetualTaskExecutionBundle(perpetualTaskPack, executionCapabilities,
                infrastructureMappingDTO.getOrgIdentifier(), infrastructureMappingDTO.getProjectIdentifier());
    }
    private Any packDeploymentPackageInstanceSyncPerpetualTaskParams(
            InfrastructureMappingDTO infrastructureMappingDTO, InfrastructureOutcome infrastructureOutcome, DeploymentPackageDeploymentInfoDTO deploymentPackageDeploymentInfoDTO) {
        return Any.pack(createDeploymentPackageInstanceSyncPerpetualTaskParams(infrastructureMappingDTO, infrastructureOutcome, deploymentPackageDeploymentInfoDTO));
    }
    private DeploymentPackageInstanceSyncPerpetualTaskParams createDeploymentPackageInstanceSyncPerpetualTaskParams(
            InfrastructureMappingDTO infrastructureMappingDTO, InfrastructureOutcome infrastructureOutcome, DeploymentPackageDeploymentInfoDTO deploymentPackageDeploymentInfoDTO) {
         return DeploymentPackageInstanceSyncPerpetualTaskParams.newBuilder()
                .setScript((String) expressionEvaluator.substitute(
                        deploymentPackageDeploymentInfoDTO.getInstanceFetchScript(), prepareContext(infrastructureMappingDTO)))
                .setAccountId(infrastructureMappingDTO.getAccountIdentifier())
                .setOutputPathKey(OUTPUT_PATH_KEY)
                .build();
    }
    private Map<String, Object> prepareContext(InfrastructureMappingDTO infrastructureMappingDTO) {
        return ImmutableMap.<String, Object>builder()
                .put("secrets",
                        SecretFunctor.builder()
                                .managerDecryptionService(managerDecryptionService)
                                .secretManager(secretManager)
                                .accountId(infrastructureMappingDTO.getAccountIdentifier())
                                .envId(infrastructureMappingDTO.getEnvIdentifier())
                                .disablePhasing(true)
                                .build())
                .build();
    }

    List<ExecutionCapability> getExecutionCapability(
            List<DeploymentInfoDTO> deploymentInfoDTOList)
         {
             return deploymentInfoDTOList.stream()
                     .filter(Objects::nonNull)
                     .map(DeploymentPackageDeploymentInfoDTO.class ::cast)
                     .map(deploymentPackageInfo->getSelectorCapability(deploymentPackageInfo))
                     .collect(Collectors.toList());
         }

    SelectorCapability getSelectorCapability(DeploymentPackageDeploymentInfoDTO deploymentPackageInfo)
    {
        List<String> tagsInDeploymentInfo = deploymentPackageInfo.getTags();
        Set<String> tags = new HashSet<>(tagsInDeploymentInfo);
        return SelectorCapability.builder().selectors(tags).build();
    }

}
