package io.harness.ng;

import static io.harness.rule.OwnerRule.VIKAS_M;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.helper.HarnessManagedConnectorHelper;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorActivityService;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.encryption.SecretRefData;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.eventsframework.api.Producer;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.ng.opa.entities.connector.OpaConnectorService;
import io.harness.repositories.ConnectorRepository;
import io.harness.rule.Owner;
import io.harness.telemetry.helpers.ConnectorInstrumentationHelper;
import io.harness.utils.featureflaghelper.NGFeatureFlagHelperService;

import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConnectorServiceImplTest extends CategoryTest {
  private ConnectorServiceImpl connectorService;
  @Mock private ConnectorService defaultConnectorService;
  @Mock private ConnectorService secretManagerConnectorService;
  @Mock private ConnectorActivityService connectorActivityService;
  @Mock private ConnectorHeartbeatService connectorHeartbeatService;
  @Mock private ConnectorRepository connectorRepository;
  @Mock private Producer eventProducer;
  @Mock private ExecutorService executorService;
  @Mock private ConnectorErrorMessagesHelper connectorErrorMessagesHelper;
  @Mock private HarnessManagedConnectorHelper harnessManagedConnectorHelper;
  @Mock private NGErrorHelper ngErrorHelper;
  @Mock private GitSyncSdkService gitSyncSdkService;
  @Mock private ConnectorInstrumentationHelper instrumentationHelper;
  @Mock private OpaConnectorService opaConnectorService;
  @Mock private NGFeatureFlagHelperService ngFeatureFlagHelperService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    connectorService = spy(new ConnectorServiceImpl(defaultConnectorService, secretManagerConnectorService,
        connectorActivityService, connectorHeartbeatService, connectorRepository, eventProducer, executorService,
        connectorErrorMessagesHelper, harnessManagedConnectorHelper, ngErrorHelper, gitSyncSdkService,
        instrumentationHelper, opaConnectorService, ngFeatureFlagHelperService));
  }

  private ConnectorDTO getRequestDTO_vaultAppRole() {
    SecretRefData secretRefData = new SecretRefData(randomAlphabetic(10));
    secretRefData.setDecryptedValue(randomAlphabetic(5).toCharArray());
    ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder().build();
    connectorInfo.setConnectorType(ConnectorType.VAULT);
    connectorInfo.setConnectorConfig(VaultConnectorDTO.builder()
                                         .vaultUrl("https://vaultqa.harness.io")
                                         .secretEngineVersion(1)
                                         .appRoleId(randomAlphabetic(10))
                                         .secretId(secretRefData)
                                         .renewalIntervalMinutes(10)
                                         .build());
    connectorInfo.setName("name");
    connectorInfo.setIdentifier("identifier");
    connectorInfo.setOrgIdentifier("orgIdentifier");
    connectorInfo.setProjectIdentifier("projectIdentifier");
    return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void createConnector_appRoleWithFFEnabled() {
    ConnectorDTO connectorDTO = getRequestDTO_vaultAppRole();
    String accountIdentifier = randomAlphabetic(10);
    when(ngFeatureFlagHelperService.isEnabled(any(), any())).thenReturn(true);
    when(harnessManagedConnectorHelper.isHarnessManagedSecretManager(any())).thenReturn(true);
    when(gitSyncSdkService.isDefaultBranch(any(), any(), any())).thenReturn(false);
    ArgumentCaptor<ConnectorDTO> argumentCaptor = ArgumentCaptor.forClass(ConnectorDTO.class);
    when(secretManagerConnectorService.create(argumentCaptor.capture(), any(), any()))
        .thenReturn(ConnectorResponseDTO.builder().build());
    when(instrumentationHelper.sendConnectorCreateEvent(any(), any())).thenReturn(null);

    connectorService.create(connectorDTO, accountIdentifier, ChangeType.ADD);
    VaultConnectorDTO vaultConnectorDTO =
        (VaultConnectorDTO) argumentCaptor.getValue().getConnectorInfo().getConnectorConfig();
    assertThat(vaultConnectorDTO.isDoNotRenewAppRoleToken()).isEqualTo(true);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void createConnector_appRoleWithFFDisabled() {
    ConnectorDTO connectorDTO = getRequestDTO_vaultAppRole();
    String accountIdentifier = randomAlphabetic(10);
    when(ngFeatureFlagHelperService.isEnabled(any(), any())).thenReturn(false);
    when(harnessManagedConnectorHelper.isHarnessManagedSecretManager(any())).thenReturn(true);
    when(gitSyncSdkService.isDefaultBranch(any(), any(), any())).thenReturn(false);
    ArgumentCaptor<ConnectorDTO> argumentCaptor = ArgumentCaptor.forClass(ConnectorDTO.class);
    when(secretManagerConnectorService.create(argumentCaptor.capture(), any(), any()))
        .thenReturn(ConnectorResponseDTO.builder().build());
    when(instrumentationHelper.sendConnectorCreateEvent(any(), any())).thenReturn(null);

    connectorService.create(connectorDTO, accountIdentifier, ChangeType.ADD);
    VaultConnectorDTO vaultConnectorDTO =
        (VaultConnectorDTO) argumentCaptor.getValue().getConnectorInfo().getConnectorConfig();
    assertThat(vaultConnectorDTO.isDoNotRenewAppRoleToken()).isEqualTo(false);
  }
}
