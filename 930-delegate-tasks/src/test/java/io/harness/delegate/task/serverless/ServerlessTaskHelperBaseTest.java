/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.rule.OwnerRule.ALLU_VAMSI;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifactory.ArtifactoryNgService;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.service.git.NGGitServiceImpl;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthCredentialsDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.instancesync.info.ServerlessAwsLambdaServerInstanceInfo;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaManifestSchema;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.artifactory.ArtifactoryRequestMapper;
import io.harness.delegate.task.aws.AwsNgConfigMapper;
import io.harness.delegate.task.git.ScmFetchFilesHelperNG;
import io.harness.delegate.task.aws.AwsNgConfigMapper;
import io.harness.encryption.SecretRefData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.HintException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.filesystem.FileIo;
import io.harness.git.GitClientV2;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.serverless.model.AwsLambdaFunctionDetails;
import io.harness.serverless.model.ServerlessDelegateTaskParams;
import io.harness.shell.SshSessionConfig;

import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegateNG;

import java.io.IOException;
import java.nio.file.Path;
import software.wings.service.impl.AwsApiHelperService;

import com.amazonaws.services.s3.model.S3Object;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDP)
public class ServerlessTaskHelperBaseTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ArtifactoryNgService artifactoryNgService;
  @Mock private ArtifactoryRequestMapper artifactoryRequestMapper;
  @Mock private SecretDecryptionService secretDecryptionService;
  @Mock private ServerlessInfraConfigHelper serverlessInfraConfigHelper;
  @Mock private AwsNgConfigMapper awsNgConfigMapper;
  @Mock private AwsLambdaHelperServiceDelegateNG awsLambdaHelperServiceDelegateNG;
  @Mock private AwsInternalConfig awsInternalConfig;
  @Mock private AwsNgConfigMapper awsNgConfigMapper;
  @Mock protected AwsApiHelperService awsApiHelperService;
  @InjectMocks @Spy private ServerlessTaskHelperBase serverlessTaskHelperBase;

  private static final String ARTIFACTORY_PATH = "asdffasd.zip";
  private static final String BUCKET_NAME = "bucket";
  private static final String FILE_PATH = "artifact/aws.zip";
  private String ARTIFACT_DIRECTORY;
  private static final String ARTIFACTORY_ARTIFACT_PATH = "artifactPath";
  private static final String ARTIFACTORY_ARTIFACT_NAME = "artifactName";

  String repositoryName = "dfsgvgasd";

  ArtifactoryConfigRequest artifactoryConfigRequest = ArtifactoryConfigRequest.builder().build();
  SecretRefData password = SecretRefData.builder().build();
  ArtifactoryAuthCredentialsDTO artifactoryAuthCredentialsDTO =
      ArtifactoryUsernamePasswordAuthDTO.builder().passwordRef(password).build();
  ArtifactoryAuthenticationDTO artifactoryAuthenticationDTO = ArtifactoryAuthenticationDTO.builder()
                                                                  .credentials(artifactoryAuthCredentialsDTO)
                                                                  .authType(ArtifactoryAuthType.USER_PASSWORD)
                                                                  .build();
  ArtifactoryConnectorDTO connectorConfigDTO =
      ArtifactoryConnectorDTO.builder().auth(artifactoryAuthenticationDTO).build();
  ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().connectorConfig(connectorConfigDTO).build();
  List<EncryptedDataDetail> encryptedDataDetailsList =
      Arrays.asList(EncryptedDataDetail.builder().fieldName("afsd").build());

  @Mock LogCallback logCallback;

  @Before
  public void setUp() {
    ARTIFACT_DIRECTORY =
        "./repository/serverless/" + RandomStringUtils.random(5, 0, 0, true, true, null, new SecureRandom());
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void fetchArtifactoryArtifactTest() throws Exception {
    ServerlessArtifactConfig serverlessArtifactConfig = ServerlessArtifactoryArtifactConfig.builder()
                                                            .artifactPath(ARTIFACTORY_PATH)
                                                            .connectorDTO(connectorInfoDTO)
                                                            .encryptedDataDetails(encryptedDataDetailsList)
                                                            .repositoryName(repositoryName)
                                                            .build();
  ServerlessArtifactConfig serverlessArtifactConfig = ServerlessArtifactoryArtifactConfig.builder()
                                                          .artifactPath(ARTIFACTORY_PATH)
                                                          .connectorDTO(connectorInfoDTO)
                                                          .encryptedDataDetails(encryptedDataDetailsList)
                                                          .repositoryName(repositoryName)
                                                          .build();

  private ServerlessDelegateTaskParams serverlessDelegateTaskParams =
      ServerlessDelegateTaskParams.builder().workingDirectory("/dir/").serverlessClientPath("/scPath").build();
  private GitConfigDTO gitConfigDTO = GitConfigDTO.builder().url("url").build();
  @Mock LogCallback logCallback;
  @Mock ServerlessGitFetchTaskHelper serverlessGitFetchTaskHelper;
  @Mock ScmFetchFilesHelperNG scmFetchFilesHelper;
  @Mock GitDecryptionHelper gitDecryptionHelper;
  @Mock NGGitServiceImpl ngGitService;
  @Mock SshSessionConfig sshSessionConfig;
  @Mock GitClientV2 gitClientV2;

  @Test
  @Owner(developers = ALLU_VAMSI)
  @Category(UnitTests.class)
  public void fetchManifestFilesAndWriteToDirectoryTest() throws IOException {
    GitStoreDelegateConfig gitStoreDelegateConfig = GitStoreDelegateConfig.builder()
                                                        .branch("branch")
                                                        .commitId("commitId")
                                                        .connectorName("connector")
                                                        .manifestId("manifest")
                                                        .gitConfigDTO(gitConfigDTO)
                                                        .fetchType(FetchType.BRANCH)
                                                        .paths(new ArrayList<String>(Arrays.asList("path1", "path2")))
                                                        .optimizedFilesFetch(true)
                                                        .build();
    ServerlessAwsLambdaManifestConfig serverlessManifestConfig = ServerlessAwsLambdaManifestConfig.builder()
                                                                     .manifestPath("manifestPath")
                                                                     .configOverridePath("path")
                                                                     .gitStoreDelegateConfig(gitStoreDelegateConfig)
                                                                     .build();

    doNothing().when(serverlessGitFetchTaskHelper).decryptGitStoreConfig(gitStoreDelegateConfig);
    doNothing().when(scmFetchFilesHelper).downloadFilesUsingScm("/dir/", gitStoreDelegateConfig, logCallback);
    doReturn("fileLog")
        .when(serverlessTaskHelperBase)
        .getManifestFileNamesInLogFormat(serverlessDelegateTaskParams.getWorkingDirectory());

    serverlessTaskHelperBase.fetchManifestFilesAndWriteToDirectory(
        serverlessManifestConfig, "accountId", logCallback, serverlessDelegateTaskParams);

    verify(logCallback).saveExecutionLog(color("Successfully fetched following files:", White, Bold));
    verify(logCallback)
        .saveExecutionLog(color(format("Fetching %s files with identifier: %s",
                                    gitStoreDelegateConfig.getManifestType(), gitStoreDelegateConfig.getManifestId()),
            White, Bold));
    verify(logCallback).saveExecutionLog("Using optimized file fetch");
  }

  @Test
  @Owner(developers = ALLU_VAMSI)
  @Category(UnitTests.class)
  public void downloadFilesFromGitNotOptimizedFilesFetchTest() throws IOException {
    GitStoreDelegateConfig gitStoreDelegateConfig = GitStoreDelegateConfig.builder()
                                                        .branch("branch")
                                                        .commitId("commitId")
                                                        .connectorName("connector")
                                                        .manifestId("manifest")
                                                        .gitConfigDTO(gitConfigDTO)
                                                        .fetchType(FetchType.BRANCH)
                                                        .paths(new ArrayList<String>(Arrays.asList("path1", "path2")))
                                                        .optimizedFilesFetch(false)
                                                        .build();
    ServerlessAwsLambdaManifestConfig serverlessManifestConfig = ServerlessAwsLambdaManifestConfig.builder()
                                                                     .manifestPath("manifestPath")
                                                                     .configOverridePath("path")
                                                                     .gitStoreDelegateConfig(gitStoreDelegateConfig)
                                                                     .build();

    doNothing().when(serverlessGitFetchTaskHelper).decryptGitStoreConfig(gitStoreDelegateConfig);
    doNothing().when(scmFetchFilesHelper).downloadFilesUsingScm("/dir/", gitStoreDelegateConfig, logCallback);
    doReturn("fileLog")
        .when(serverlessTaskHelperBase)
        .getManifestFileNamesInLogFormat(serverlessDelegateTaskParams.getWorkingDirectory());
    doNothing()
        .when(gitDecryptionHelper)
        .decryptGitConfig(gitConfigDTO, gitStoreDelegateConfig.getEncryptedDataDetails());
    doNothing()
        .when(ngGitService)
        .downloadFiles(gitStoreDelegateConfig, "/dir/", "acoountId", sshSessionConfig, gitConfigDTO);
    serverlessTaskHelperBase.fetchManifestFilesAndWriteToDirectory(
        serverlessManifestConfig, "accountId", logCallback, serverlessDelegateTaskParams);
  }

  @Test(expected = HintException.class)
  @Owner(developers = ALLU_VAMSI)
  @Category(UnitTests.class)
  public void downloadFilesFromGitExceptionTest() throws Exception {
    GitStoreDelegateConfig gitStoreDelegateConfig = GitStoreDelegateConfig.builder()
                                                        .branch("branch")
                                                        .commitId("commitId")
                                                        .connectorName("connector")
                                                        .manifestId("manifest")
                                                        .gitConfigDTO(gitConfigDTO)
                                                        .fetchType(FetchType.BRANCH)
                                                        .paths(new ArrayList<String>(Arrays.asList("path1", "path2")))
                                                        .optimizedFilesFetch(false)
                                                        .build();
    ServerlessAwsLambdaManifestConfig serverlessManifestConfig = ServerlessAwsLambdaManifestConfig.builder()
                                                                     .manifestPath("manifestPath")
                                                                     .configOverridePath("path")
                                                                     .gitStoreDelegateConfig(gitStoreDelegateConfig)
                                                                     .build();

    doNothing().when(serverlessGitFetchTaskHelper).decryptGitStoreConfig(gitStoreDelegateConfig);
    doNothing().when(scmFetchFilesHelper).downloadFilesUsingScm("/dir/", gitStoreDelegateConfig, logCallback);
    doReturn("fileLog")
        .when(serverlessTaskHelperBase)
        .getManifestFileNamesInLogFormat(serverlessDelegateTaskParams.getWorkingDirectory());
    doNothing()
        .when(gitDecryptionHelper)
        .decryptGitConfig(gitConfigDTO, gitStoreDelegateConfig.getEncryptedDataDetails());
    doReturn(sshSessionConfig)
        .when(gitDecryptionHelper)
        .getSSHSessionConfig(
            gitStoreDelegateConfig.getSshKeySpecDTO(), gitStoreDelegateConfig.getEncryptedDataDetails());

    doThrow(IOException.class)
        .when(ngGitService)
        .downloadFiles(gitStoreDelegateConfig, "/dir/", "accountId", sshSessionConfig, gitConfigDTO);

    serverlessTaskHelperBase.fetchManifestFilesAndWriteToDirectory(
        serverlessManifestConfig, "accountId", logCallback, serverlessDelegateTaskParams);
  }

  @Test(expected = HintException.class)
  @Owner(developers = ALLU_VAMSI)
  @Category(UnitTests.class)
  public void fetchArtifactInputStreamNullTest() throws Exception {
    doReturn(artifactoryConfigRequest).when(artifactoryRequestMapper).toArtifactoryRequest(connectorConfigDTO);
    ServerlessInfraConfig serverlessInfraConfig = ServerlessAwsLambdaInfraConfig.builder().region("us-east-1").build();
    Map<String, String> artifactMetadata = new HashMap<>();
    artifactMetadata.put(ARTIFACTORY_ARTIFACT_PATH, repositoryName + "/" + ARTIFACTORY_PATH);
    artifactMetadata.put(ARTIFACTORY_ARTIFACT_NAME, repositoryName + "/" + ARTIFACTORY_PATH);

    doReturn(null)
        .when(artifactoryNgService)
        .downloadArtifacts(artifactoryConfigRequest, repositoryName, artifactMetadata, ARTIFACTORY_ARTIFACT_PATH,
            ARTIFACTORY_ARTIFACT_NAME);
    serverlessTaskHelperBase.fetchArtifact(
        serverlessArtifactConfig, logCallback, ARTIFACT_DIRECTORY, serverlessInfraConfig);
    verify(logCallback)
        .saveExecutionLog("Failed to download artifact from artifactory.ø", ERROR, CommandExecutionStatus.FAILURE);
    Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(new Exception("HintException"));
    verify(logCallback)
        .saveExecutionLog(
            "Failed to download artifact from artifactory. " + ExceptionUtils.getMessage(sanitizedException), ERROR,
            CommandExecutionStatus.FAILURE);
    FileIo.deleteDirectoryAndItsContentIfExists(Paths.get(ARTIFACT_DIRECTORY).toAbsolutePath().toString());
  }

  @Test(expected = HintException.class)
  @Owner(developers = ALLU_VAMSI)
  @Category(UnitTests.class)
  public void fetchArtifactEmptyArtifactPathTest() throws Exception {
    ServerlessArtifactConfig serverlessArtifactConfig =
        ServerlessArtifactoryArtifactConfig.builder().repositoryName(repositoryName).build();
    serverlessTaskHelperBase.fetchArtifact(serverlessArtifactConfig, logCallback, ARTIFACT_DIRECTORY);
  }

  @Test
  @Owner(developers = ALLU_VAMSI)
  @Category(UnitTests.class)
  public void getManifestFileNamesInLogFormatTest() throws IOException {
    String workingDir =
        Paths.get("workingDir" + convertBase64UuidToCanonicalForm(generateUuid())).toAbsolutePath().toString();
    FileIo.createDirectoryIfDoesNotExist(workingDir);
    String filePath1 = Paths.get(workingDir, "file1").toString();
    String filePath2 = Paths.get(workingDir, "file2").toString();
    FileIo.writeUtf8StringToFile(filePath1, "fileContent1");
    FileIo.writeUtf8StringToFile(filePath2, "fileContent2");
    serverlessTaskHelperBase.getManifestFileNamesInLogFormat(workingDir);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void fetchS3ArtifactTest() throws Exception {
    ServerlessArtifactConfig serverlessArtifactConfig =
        ServerlessS3ArtifactConfig.builder().bucketName(BUCKET_NAME).filePath(FILE_PATH).build();
    ServerlessAwsLambdaInfraConfig serverlessInfraConfig =
        ServerlessAwsLambdaInfraConfig.builder().region("us-east-1").build();

    String input = "asfd";
    InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    String artifactPath = Paths
                              .get(((ServerlessS3ArtifactConfig) serverlessArtifactConfig).getBucketName(),
                                  ((ServerlessS3ArtifactConfig) serverlessArtifactConfig).getFilePath())
                              .toString();

    S3Object s3Object = new S3Object();
    s3Object.setObjectContent(inputStream);

    doReturn(awsInternalConfig)
        .when(awsNgConfigMapper)
        .createAwsInternalConfig(serverlessInfraConfig.getAwsConnectorDTO());
    doReturn(s3Object)
        .when(awsApiHelperService)
        .getObjectFromS3(awsInternalConfig, serverlessInfraConfig.getRegion(), BUCKET_NAME, FILE_PATH);
    serverlessTaskHelperBase.fetchArtifact(
        serverlessArtifactConfig, logCallback, ARTIFACT_DIRECTORY, serverlessInfraConfig);
    verify(logCallback)
        .saveExecutionLog(color(
            format("Downloading %s artifact with identifier: %s", serverlessArtifactConfig.getServerlessArtifactType(),
                ((ServerlessS3ArtifactConfig) serverlessArtifactConfig).getIdentifier()),
            White, Bold));
    verify(logCallback).saveExecutionLog("S3 Object Path: " + artifactPath);
    verify(logCallback).saveExecutionLog(color("Successfully downloaded artifact..", White, Bold));
  }

  @After
  public void cleanUp() throws IOException {
    File file = new File(ARTIFACT_DIRECTORY);
    FileUtils.deleteDirectory(file);
    file.delete();
  }
}

  @Test
  @Owner(developers = ALLU_VAMSI)
  @Category(UnitTests.class)
  public void generateTruncatedFileListForLoggingTest() {
    Path path1 = Paths.get("path1");
    Path path2 = Paths.get("path2");
    Stream<Path> paths = Stream.of(path1, path2);
    serverlessTaskHelperBase.generateTruncatedFileListForLogging(path1, paths);
  }

  @Test
  @Owner(developers = ALLU_VAMSI)
  @Category(UnitTests.class)
  public void getRelativePathTest() {
    assertThat(serverlessTaskHelperBase.getRelativePath("dir/path1", "dir")).isEqualTo("path1");
  }

  @Test
  @Owner(developers = ALLU_VAMSI)
  @Category(UnitTests.class)
  public void replaceManifestWithRenderedContentTest() throws IOException {
    String workingDir = Paths.get("workingDir", convertBase64UuidToCanonicalForm(generateUuid()))
                            .normalize()
                            .toAbsolutePath()
                            .toString();
    FileIo.createDirectoryIfDoesNotExist(workingDir);
    ServerlessDelegateTaskParams serverlessDelegateTaskParams =
        ServerlessDelegateTaskParams.builder().workingDirectory(workingDir).serverlessClientPath("scPath").build();
    ServerlessAwsLambdaManifestSchema serverlessManifestSchema =
        ServerlessAwsLambdaManifestSchema.builder()
            .plugins(new ArrayList<String>(Arrays.asList("plugin@manifest")))
            .build();
    GitStoreDelegateConfig gitStoreDelegateConfig =
        GitStoreDelegateConfig.builder().fetchType(FetchType.BRANCH).optimizedFilesFetch(true).build();
    ServerlessAwsLambdaManifestConfig serverlessManifestConfig = ServerlessAwsLambdaManifestConfig.builder()
                                                                     .manifestPath("manifestFile")
                                                                     .configOverridePath("path")
                                                                     .gitStoreDelegateConfig(gitStoreDelegateConfig)
                                                                     .build();

    serverlessTaskHelperBase.replaceManifestWithRenderedContent(
        serverlessDelegateTaskParams, serverlessManifestConfig, "manifestContent", serverlessManifestSchema);
    FileIo.deleteFileIfExists(workingDir + "manifestFile");
  }

  @Test
  @Owner(developers = ALLU_VAMSI)
  @Category(UnitTests.class)
  public void getServerlessAwsLambdaServerInstanceInfosTest() {
    List<String> functions = Arrays.asList("fun");
    AwsManualConfigSpecDTO config = AwsManualConfigSpecDTO.builder().accessKey("accessKey").build();
    AwsCredentialDTO awsCredentialDTO =
        AwsCredentialDTO.builder().awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS).config(config).build();
    AwsConnectorDTO awsConnectorDTO = AwsConnectorDTO.builder().credential(awsCredentialDTO).build();
    ServerlessAwsLambdaInfraConfig serverlessAwsLambdaInfraConfig = ServerlessAwsLambdaInfraConfig.builder()
                                                                        .awsConnectorDTO(awsConnectorDTO)
                                                                        .infraStructureKey("infraKey")
                                                                        .stage("stage")
                                                                        .build();
    ServerlessAwsLambdaDeploymentReleaseData deploymentReleaseData =
        ServerlessAwsLambdaDeploymentReleaseData.builder()
            .functions(functions)
            .serviceName("service")
            .region("us-east-1")
            .serverlessInfraConfig(serverlessAwsLambdaInfraConfig)
            .build();
    doNothing().when(serverlessInfraConfigHelper).decryptServerlessInfraConfig(serverlessAwsLambdaInfraConfig);
    AwsInternalConfig awsInternalConfig =
        AwsInternalConfig.builder().useIRSA(false).accessKey(new char[] {'a', 'c', 'c', 'e', 's', 's'}).build();
    doReturn(awsInternalConfig)
        .when(awsNgConfigMapper)
        .createAwsInternalConfig(serverlessAwsLambdaInfraConfig.getAwsConnectorDTO());
    AwsLambdaFunctionDetails awsLambdaFunctionDetails = AwsLambdaFunctionDetails.builder().functionName("fun").build();
    doReturn(awsLambdaFunctionDetails)
        .when(awsLambdaHelperServiceDelegateNG)
        .getAwsLambdaFunctionDetails(awsInternalConfig, "fun", deploymentReleaseData.getRegion());
    ServerlessAwsLambdaServerInstanceInfo serverInstanceInfo =
        (ServerlessAwsLambdaServerInstanceInfo) serverlessTaskHelperBase
            .getServerlessAwsLambdaServerInstanceInfos(deploymentReleaseData)
            .get(0);

    assertThat(serverInstanceInfo.getServerlessServiceName()).isEqualTo("service");
    assertThat(serverInstanceInfo.getServerlessStage()).isEqualTo("stage");
    assertThat(serverInstanceInfo.getFunctionName()).isEqualTo("fun");
    assertThat(serverInstanceInfo.getRegion()).isEqualTo("us-east-1");
    assertThat(serverInstanceInfo.getInfraStructureKey()).isEqualTo("infraKey");
  }
}