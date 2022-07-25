/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.aws;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.infra.beans.AwsInstanceFilter;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.SshWinRmAwsInfrastructureOutcome;
import io.harness.cdng.serverless.ServerlessEntityHelper;
import io.harness.cdng.ssh.SshEntityHelper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityBulkOrExecutionCapability;
import io.harness.delegate.task.ssh.AwsSshWinrmInfraDelegateConfig;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.dtos.deploymentinfo.AwsDeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SecretSpecDTO;
import io.harness.ng.core.dto.secrets.WinRmCredentialsSpecDTO;
import io.harness.ng.core.models.Secret;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.instancesync.AwsSshInstanceSyncPerpetualTaskParamsNg;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.InstanceSyncPerpetualTaskHandler;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.CDP)
public class AwsInstanceSyncPerpetualTaskHandler extends InstanceSyncPerpetualTaskHandler {
  @Inject private NGSecretServiceV2 ngSecretServiceV2;
  @Inject private SshEntityHelper sshEntityHelper;
  @Inject private ServerlessEntityHelper serverlessEntityHelper;

  @Override
  public PerpetualTaskExecutionBundle getExecutionBundle(InfrastructureMappingDTO infrastructure,
      List<DeploymentInfoDTO> deploymentInfoDTOList, InfrastructureOutcome infrastructureOutcome) {
    SshWinRmAwsInfrastructureOutcome awsInfrastructureOutcome =
        (SshWinRmAwsInfrastructureOutcome) infrastructureOutcome;

    Secret secret = findSecret(infrastructure.getAccountIdentifier(), infrastructure.getOrgIdentifier(),
        infrastructure.getProjectIdentifier(), awsInfrastructureOutcome.getCredentialsRef());

    List<AwsDeploymentInfoDTO> awsDeploymentInfoDTOs = (List<AwsDeploymentInfoDTO>) (List<?>) deploymentInfoDTOList;
    List<String> hosts =
        new ArrayList(awsDeploymentInfoDTOs.stream().flatMap(x -> x.getHosts().stream()).collect(Collectors.toSet()));

    SecretSpecDTO secretSpecDTO = secret.getSecretSpec().toDTO();
    int port = getPort(secretSpecDTO);

    String serviceType = awsDeploymentInfoDTOs.get(0).getType();

    BaseNGAccess access = BaseNGAccess.builder()
                              .accountIdentifier(infrastructure.getAccountIdentifier())
                              .orgIdentifier(infrastructure.getOrgIdentifier())
                              .projectIdentifier(infrastructure.getProjectIdentifier())
                              .build();

    ConnectorInfoDTO connectorInfoDTO = sshEntityHelper.getConnectorInfoDTO(infrastructureOutcome, access);

    AwsConnectorDTO awsConnector = (AwsConnectorDTO) connectorInfoDTO.getConnectorConfig();

    List<EncryptedDataDetail> encryptedData = serverlessEntityHelper.getEncryptionDataDetails(connectorInfoDTO, access);

    AwsSshWinrmInfraDelegateConfig.AwsSshWinrmInfraDelegateConfigBuilder awsSshWinrmInfraDelegateConfigBuilder =
        AwsSshWinrmInfraDelegateConfig.builder()
            .awsConnectorDTO(awsConnector)
            .encryptionDataDetails(encryptedData)
            .serviceType(serviceType)
            .region(awsInfrastructureOutcome.getRegion())
            .autoScalingGroupName(awsInfrastructureOutcome.getAutoScalingGroupName());

    if (StringUtils.isEmpty(awsInfrastructureOutcome.getAutoScalingGroupName())
        && awsInfrastructureOutcome.getAwsInstanceFilter() != null) {
      AwsInstanceFilter awsInstanceFilter = awsInfrastructureOutcome.getAwsInstanceFilter();
      awsSshWinrmInfraDelegateConfigBuilder.vpcIds(awsInstanceFilter.getVpcs()).tags(awsInstanceFilter.getTags());
    }

    AwsSshInstanceSyncPerpetualTaskParamsNg awsInstanceSyncPerpetualTaskParamsNg =
        AwsSshInstanceSyncPerpetualTaskParamsNg.newBuilder()
            .setAccountId(infrastructure.getAccountIdentifier())
            .setServiceType(serviceType)
            .addAllDeployedHosts(hosts)
            .setPort(port)
            .setAwsSshWinrmInfraDelegateConfig(
                ByteString.copyFrom(kryoSerializer.asBytes(awsSshWinrmInfraDelegateConfigBuilder.build())))
            .build();

    Any perpetualTaskPack = Any.pack(awsInstanceSyncPerpetualTaskParamsNg);
    List<ExecutionCapability> executionCapabilities = getExecutionCapabilities(hosts, port);

    return createPerpetualTaskExecutionBundle(perpetualTaskPack, executionCapabilities,
        infrastructure.getOrgIdentifier(), infrastructure.getProjectIdentifier());
  }

  private Secret findSecret(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String secretIdentifierWithScope) {
    SecretRefData secretRefData = SecretRefHelper.createSecretRef(secretIdentifierWithScope);
    IdentifierRef secretIdentifiers = IdentifierRefHelper.getIdentifierRef(
        secretIdentifierWithScope, accountIdentifier, orgIdentifier, projectIdentifier);
    Optional<Secret> secret = ngSecretServiceV2.get(secretIdentifiers.getAccountIdentifier(),
        secretIdentifiers.getOrgIdentifier(), secretIdentifiers.getProjectIdentifier(), secretRefData.getIdentifier());

    if (!secret.isPresent()) {
      throw new InvalidRequestException(format("Secret ref %s is missing for %s", secretIdentifierWithScope,
          AwsInstanceSyncPerpetualTaskHandler.class.getSimpleName()));
    }

    return secret.get();
  }

  private List<ExecutionCapability> getExecutionCapabilities(List<String> hosts, int port) {
    return singletonList(SocketConnectivityBulkOrExecutionCapability.builder().hostNames(hosts).port(port).build());
  }

  private int getPort(SecretSpecDTO secretSpecDTO) {
    if (secretSpecDTO instanceof SSHKeySpecDTO) {
      return ((SSHKeySpecDTO) secretSpecDTO).getPort();
    } else if (secretSpecDTO instanceof WinRmCredentialsSpecDTO) {
      return ((WinRmCredentialsSpecDTO) secretSpecDTO).getPort();
    } else {
      throw new InvalidArgumentsException(format("Invalid subclass %s provided for %s",
          secretSpecDTO.getClass().getSimpleName(), SecretSpecDTO.class.getSimpleName()));
    }
  }
}
