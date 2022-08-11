/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.service;

import static io.harness.cdng.creator.plan.manifest.ManifestsPlanCreator.SERVICE_ENTITY_DEFINITION_TYPE_KEY;
import static io.harness.cdng.manifest.ManifestType.SERVICE_OVERRIDE_SUPPORTED_MANIFEST_TYPES;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.emptyList;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.azure.webapp.ApplicationSettingsParameters;
import io.harness.cdng.azure.webapp.ConnectionStringsParameters;
import io.harness.cdng.azure.webapp.StartupCommandParameters;
import io.harness.cdng.configfile.ConfigFileWrapper;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.service.beans.AzureWebAppServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.cdng.utilities.ArtifactsUtility;
import io.harness.cdng.utilities.AzureConfigsUtility;
import io.harness.cdng.utilities.ConfigFileUtility;
import io.harness.cdng.utilities.ManifestsUtility;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.environment.yaml.NGEnvironmentConfig;
import io.harness.ng.core.service.yaml.NGServiceV2InfoConfig;
import io.harness.ng.core.serviceoverride.yaml.NGServiceOverrideConfig;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.YamlUpdates;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse.PlanCreationResponseBuilder;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Contains method useful for serviceDefinition plan creator
 */
@UtilityClass
public class ServiceDefinitionPlanCreatorHelper {
  private final String SERVICE_OVERRIDES = "service overrides";
  private final String ENVIRONMENT_GLOBAL_OVERRIDES = "environment global overrides";

  public Map<String, ByteString> prepareMetadata(
      String planNodeId, ServiceConfig actualServiceConfig, KryoSerializer kryoSerializer) {
    Map<String, ByteString> metadataDependency = new HashMap<>();
    metadataDependency.put(YamlTypes.UUID, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(planNodeId)));
    // TODO: Find an efficient way to not pass whole service config
    metadataDependency.put(
        YamlTypes.SERVICE_CONFIG, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(actualServiceConfig)));
    return metadataDependency;
  }

  public Map<String, ByteString> prepareMetadataManifestV2(String planNodeId,
      List<ManifestConfigWrapper> finalManifests, ServiceDefinitionType serviceDefinitionType,
      KryoSerializer kryoSerializer) {
    Map<String, ByteString> metadataDependency = new HashMap<>();
    metadataDependency.put(YamlTypes.UUID, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(planNodeId)));
    metadataDependency.put(
        YamlTypes.MANIFEST_LIST_CONFIG, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(finalManifests)));
    metadataDependency.put(
        SERVICE_ENTITY_DEFINITION_TYPE_KEY, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(serviceDefinitionType)));
    return metadataDependency;
  }

  public Map<String, ByteString> prepareMetadataV2(
      String planNodeId, NGServiceV2InfoConfig serviceV2Config, KryoSerializer kryoSerializer) {
    Map<String, ByteString> metadataDependency = new HashMap<>();
    metadataDependency.put(YamlTypes.UUID, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(planNodeId)));
    // TODO: Find an efficient way to not pass whole service entity
    metadataDependency.put(
        YamlTypes.SERVICE_ENTITY, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(serviceV2Config)));

    return metadataDependency;
  }

  boolean validateCreatePlanNodeForArtifacts(ServiceConfig actualServiceConfig) {
    ArtifactListConfig artifactListConfig = actualServiceConfig.getServiceDefinition().getServiceSpec().getArtifacts();

    // Contains either primary artifacts or side-car artifacts
    if (artifactListConfig != null) {
      if (artifactListConfig.getPrimary() != null || isNotEmpty(artifactListConfig.getSidecars())) {
        return true;
      }
    }

    if (actualServiceConfig.getStageOverrides() != null
        && actualServiceConfig.getStageOverrides().getArtifacts() != null) {
      return actualServiceConfig.getStageOverrides().getArtifacts().getPrimary() != null
          || isNotEmpty(actualServiceConfig.getStageOverrides().getArtifacts().getSidecars());
    }

    return false;
  }

  boolean validateCreatePlanNodeForArtifactsV2(NGServiceV2InfoConfig serviceConfig) {
    ArtifactListConfig artifactListConfig = serviceConfig.getServiceDefinition().getServiceSpec().getArtifacts();

    // Contains either primary artifacts or side-car artifacts
    if (artifactListConfig != null) {
      return artifactListConfig.getPrimary() != null || isNotEmpty(artifactListConfig.getSidecars());
    }
    return false;
  }

  String addDependenciesForArtifacts(YamlNode serviceConfigNode,
      Map<String, PlanCreationResponse> planCreationResponseMap, ServiceConfig actualServiceConfig,
      KryoSerializer kryoSerializer) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    boolean isUseFromStage = actualServiceConfig.getUseFromStage() != null;
    YamlField artifactYamlField =
        ArtifactsUtility.fetchArtifactYamlFieldAndSetYamlUpdates(serviceConfigNode, isUseFromStage, yamlUpdates);
    String artifactsPlanNodeId = UUIDGenerator.generateUuid();

    Map<String, ByteString> metadataDependency =
        prepareMetadata(artifactsPlanNodeId, actualServiceConfig, kryoSerializer);

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(artifactsPlanNodeId, artifactYamlField);
    PlanCreationResponseBuilder artifactPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(
                artifactsPlanNodeId, Dependency.newBuilder().putAllMetadata(metadataDependency).build())
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      artifactPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(artifactsPlanNodeId, artifactPlanCreationResponse.build());
    return artifactsPlanNodeId;
  }

  String addDependenciesForArtifactsV2(YamlNode serviceV2Node,
      Map<String, PlanCreationResponse> planCreationResponseMap, NGServiceV2InfoConfig serviceV2Config,
      KryoSerializer kryoSerializer) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    YamlField artifactYamlField =
        ArtifactsUtility.fetchArtifactYamlFieldAndSetYamlUpdates(serviceV2Node, false, yamlUpdates);
    String artifactsPlanNodeId = UUIDGenerator.generateUuid();

    Map<String, ByteString> metadataDependency =
        prepareMetadataV2(artifactsPlanNodeId, serviceV2Config, kryoSerializer);

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(artifactsPlanNodeId, artifactYamlField);
    PlanCreationResponseBuilder artifactPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(
                artifactsPlanNodeId, Dependency.newBuilder().putAllMetadata(metadataDependency).build())
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      artifactPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(artifactsPlanNodeId, artifactPlanCreationResponse.build());
    return artifactsPlanNodeId;
  }

  String addDependenciesForManifests(YamlNode serviceConfigNode,
      Map<String, PlanCreationResponse> planCreationResponseMap, ServiceConfig actualServiceConfig,
      KryoSerializer kryoSerializer) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    boolean isUseFromStage = actualServiceConfig.getUseFromStage() != null;
    YamlField manifestsYamlField =
        ManifestsUtility.fetchManifestsYamlFieldAndSetYamlUpdates(serviceConfigNode, isUseFromStage, yamlUpdates);
    String manifestsPlanNodeId = "manifests-" + UUIDGenerator.generateUuid();

    Map<String, ByteString> metadataDependency =
        prepareMetadata(manifestsPlanNodeId, actualServiceConfig, kryoSerializer);

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(manifestsPlanNodeId, manifestsYamlField);
    PlanCreationResponseBuilder manifestsPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(
                manifestsPlanNodeId, Dependency.newBuilder().putAllMetadata(metadataDependency).build())
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      manifestsPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(manifestsPlanNodeId, manifestsPlanCreationResponse.build());
    return manifestsPlanNodeId;
  }

  String addDependenciesForManifestsV2(YamlNode serviceV2Node,
      Map<String, PlanCreationResponse> planCreationResponseMap, NGServiceV2InfoConfig serviceV2Config,
      NGServiceOverrideConfig serviceOverrideConfig, NGEnvironmentConfig ngEnvironmentConfig,
      KryoSerializer kryoSerializer) throws IOException {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();

    List<ManifestConfigWrapper> finalManifests =
        prepareFinalManifests(serviceV2Config, serviceOverrideConfig, ngEnvironmentConfig);

    // in case no manifest is present no node should be created
    if (isEmpty(finalManifests)) {
      return StringUtils.EMPTY;
    }

    YamlField manifestsYamlField = prepareFinalUuidInjectedManifestYamlField(serviceV2Node, finalManifests);
    PlanCreatorUtils.setYamlUpdate(manifestsYamlField, yamlUpdates);
    String manifestsPlanNodeId = "manifests-" + UUIDGenerator.generateUuid();

    Map<String, ByteString> metadataDependency = prepareMetadataManifestV2(
        manifestsPlanNodeId, finalManifests, serviceV2Config.getServiceDefinition().getType(), kryoSerializer);

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(manifestsPlanNodeId, manifestsYamlField);
    PlanCreationResponseBuilder manifestsPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(
                manifestsPlanNodeId, Dependency.newBuilder().putAllMetadata(metadataDependency).build())
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      manifestsPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(manifestsPlanNodeId, manifestsPlanCreationResponse.build());
    return manifestsPlanNodeId;
  }

  @NotNull
  private YamlField prepareFinalUuidInjectedManifestYamlField(
      YamlNode serviceV2Node, List<ManifestConfigWrapper> finalManifests) throws IOException {
    YamlField manifestsYamlField = YamlUtils.injectUuidInYamlField(YamlUtils.write(finalManifests));
    manifestsYamlField = new YamlField(YamlTypes.MANIFEST_LIST_CONFIG,
        new YamlNode(YamlTypes.MANIFEST_LIST_CONFIG, manifestsYamlField.getNode().getCurrJsonNode(),
            serviceV2Node.getField(YamlTypes.SERVICE_DEFINITION).getNode().getField(YamlTypes.SPEC).getNode()));
    return manifestsYamlField;
  }

  @VisibleForTesting
  @NotNull
  List<ManifestConfigWrapper> prepareFinalManifests(NGServiceV2InfoConfig serviceV2Config,
      NGServiceOverrideConfig serviceOverrideConfig, NGEnvironmentConfig ngEnvironmentConfig) {
    final List<ManifestConfigWrapper> svcManifests = getManifests(serviceV2Config);
    final List<ManifestConfigWrapper> envGlobalManifests =
        getAndValidateEnvGlobalManifests(serviceV2Config, ngEnvironmentConfig, svcManifests);
    final List<ManifestConfigWrapper> svcOverrideManifests =
        getAndValidateSvcOverrideManifests(serviceV2Config, serviceOverrideConfig, ngEnvironmentConfig, svcManifests);
    checkCrossLocationDuplicateIdentifiers(svcOverrideManifests, envGlobalManifests, serviceV2Config.getIdentifier(),
        ngEnvironmentConfig.getNgEnvironmentInfoConfig().getIdentifier(), SERVICE_OVERRIDES);

    final List<ManifestConfigWrapper> finalManifests = new ArrayList<>();

    finalManifests.addAll(svcManifests);
    finalManifests.addAll(envGlobalManifests);
    finalManifests.addAll(svcOverrideManifests);
    return finalManifests;
  }

  @NotNull
  private List<ManifestConfigWrapper> getAndValidateEnvGlobalManifests(NGServiceV2InfoConfig serviceV2Config,
      NGEnvironmentConfig ngEnvironmentConfig, List<ManifestConfigWrapper> svcManifests) {
    final List<ManifestConfigWrapper> envGlobalManifests = getEnvGlobalManifests(ngEnvironmentConfig);

    checkCrossLocationDuplicateIdentifiers(svcManifests, envGlobalManifests, serviceV2Config.getIdentifier(),
        ngEnvironmentConfig.getNgEnvironmentInfoConfig().getIdentifier(),
        ServiceDefinitionPlanCreatorHelper.ENVIRONMENT_GLOBAL_OVERRIDES);
    validateAllowedManifestTypesInOverrides(
        envGlobalManifests, ServiceDefinitionPlanCreatorHelper.ENVIRONMENT_GLOBAL_OVERRIDES);

    return envGlobalManifests;
  }

  @NotNull
  private List<ManifestConfigWrapper> getAndValidateSvcOverrideManifests(NGServiceV2InfoConfig serviceV2Config,
      NGServiceOverrideConfig serviceOverrideConfig, NGEnvironmentConfig ngEnvironmentConfig,
      List<ManifestConfigWrapper> svcManifests) {
    if (serviceOverrideConfig == null) {
      return EMPTY_LIST;
    }
    List<ManifestConfigWrapper> svcOverrideManifests = getSvcOverrideManifests(serviceOverrideConfig);

    checkCrossLocationDuplicateIdentifiers(svcManifests, svcOverrideManifests, serviceV2Config.getIdentifier(),
        ngEnvironmentConfig.getNgEnvironmentInfoConfig().getIdentifier(), SERVICE_OVERRIDES);
    validateAllowedManifestTypesInOverrides(svcOverrideManifests, SERVICE_OVERRIDES);

    return svcOverrideManifests;
  }

  @NonNull
  private static List<ManifestConfigWrapper> getEnvGlobalManifests(NGEnvironmentConfig ngEnvironmentConfig) {
    if (checkManifestAvailability(ngEnvironmentConfig)) {
      return EMPTY_LIST;
    }
    return ngEnvironmentConfig.getNgEnvironmentInfoConfig().getNgEnvironmentGlobalOverride().getManifests();
  }

  private static boolean checkManifestAvailability(NGEnvironmentConfig ngEnvironmentConfig) {
    return ngEnvironmentConfig == null || ngEnvironmentConfig.getNgEnvironmentInfoConfig() == null
        || ngEnvironmentConfig.getNgEnvironmentInfoConfig().getNgEnvironmentGlobalOverride() == null
        || ngEnvironmentConfig.getNgEnvironmentInfoConfig().getNgEnvironmentGlobalOverride().getManifests() == null;
  }

  private static void validateAllowedManifestTypesInOverrides(
      List<ManifestConfigWrapper> svcOverrideManifests, String overrideLocation) {
    if (isEmpty(svcOverrideManifests)) {
      return;
    }
    Set<String> unsupportedManifestTypesUsed =
        svcOverrideManifests.stream()
            .map(ManifestConfigWrapper::getManifest)
            .filter(Objects::nonNull)
            .map(ManifestConfig::getType)
            .map(ManifestConfigType::getDisplayName)
            .filter(type -> !SERVICE_OVERRIDE_SUPPORTED_MANIFEST_TYPES.contains(type))
            .collect(Collectors.toSet());
    if (isNotEmpty(unsupportedManifestTypesUsed)) {
      throw new InvalidRequestException(format("Unsupported Manifest Types: [%s] found for %s",
          unsupportedManifestTypesUsed.stream().map(Object::toString).collect(Collectors.joining(",")),
          overrideLocation));
    }
  }

  private static void checkCrossLocationDuplicateIdentifiers(List<ManifestConfigWrapper> manifestsA,
      List<ManifestConfigWrapper> manifestsB, String svcIdentifier, String envIdentifier, String overrideLocation) {
    if (isEmpty(manifestsA) || isEmpty(manifestsB)) {
      return;
    }
    Set<String> overridesIdentifiers = manifestsB.stream()
                                           .map(ManifestConfigWrapper::getManifest)
                                           .map(ManifestConfig::getIdentifier)
                                           .collect(Collectors.toSet());
    List<String> duplicateManifestIds = manifestsA.stream()
                                            .map(ManifestConfigWrapper::getManifest)
                                            .map(ManifestConfig::getIdentifier)
                                            .filter(overridesIdentifiers::contains)
                                            .collect(Collectors.toList());
    if (isNotEmpty(duplicateManifestIds)) {
      throw new InvalidRequestException(
          format("Found duplicate manifest identifiers [%s] in %s for service [%s] and environment [%s]",
              duplicateManifestIds.stream().map(Object::toString).collect(Collectors.joining(",")), overrideLocation,
              svcIdentifier, envIdentifier));
    }
  }

  private List<ManifestConfigWrapper> getManifests(NGServiceV2InfoConfig serviceV2Config) {
    return emptyIfNull(serviceV2Config.getServiceDefinition().getServiceSpec().getManifests());
  }

  @NonNull
  private List<ManifestConfigWrapper> getSvcOverrideManifests(@NonNull NGServiceOverrideConfig serviceOverrideConfigs) {
    if (serviceOverrideConfigs.getServiceOverrideInfoConfig() == null) {
      return emptyList();
    }
    return emptyIfNull(serviceOverrideConfigs.getServiceOverrideInfoConfig().getManifests());
  }

  String addDependenciesForConfigFilesV2(YamlNode serviceV2Node,
      Map<String, PlanCreationResponse> planCreationResponseMap, NGServiceV2InfoConfig serviceV2Config,
      KryoSerializer kryoSerializer) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    YamlField configFilesYamlField =
        ConfigFileUtility.fetchConfigFilesYamlFieldAndSetYamlUpdates(serviceV2Node, false, yamlUpdates);
    String configFilesPlanNodeId = "configFiles-" + UUIDGenerator.generateUuid();

    Map<String, ByteString> metadataDependency =
        prepareMetadataV2(configFilesPlanNodeId, serviceV2Config, kryoSerializer);

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(configFilesPlanNodeId, configFilesYamlField);

    PlanCreationResponseBuilder configFilesPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        getDependencies(configFilesPlanNodeId, metadataDependency, dependenciesMap));
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      configFilesPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(configFilesPlanNodeId, configFilesPlanCreationResponse.build());

    return configFilesPlanNodeId;
  }

  private Dependencies getDependencies(
      String planNodeId, Map<String, ByteString> metadataDependency, Map<String, YamlField> dependenciesMap) {
    return DependenciesUtils.toDependenciesProto(dependenciesMap)
        .toBuilder()
        .putDependencyMetadata(planNodeId, Dependency.newBuilder().putAllMetadata(metadataDependency).build())
        .build();
  }

  String addDependenciesForStartupCommand(YamlNode serviceConfigNode,
      Map<String, PlanCreationResponse> planCreationResponseMap, ServiceConfig serviceConfig,
      KryoSerializer kryoSerializer) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    boolean isUseFromStage = serviceConfig.getUseFromStage() != null;
    YamlField startupCommandYamlField = AzureConfigsUtility.fetchAzureConfigYamlFieldAndSetYamlUpdates(
        serviceConfigNode, isUseFromStage, yamlUpdates, YamlTypes.STARTUP_COMMAND);
    String startupCommandPlanNodeId = "startupCommnad-" + UUIDGenerator.generateUuid();

    StoreConfigWrapper startupCommand =
        ((AzureWebAppServiceSpec) serviceConfig.getServiceDefinition().getServiceSpec()).getStartupCommand();
    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(startupCommandPlanNodeId, startupCommandYamlField);
    PlanCreationResponseBuilder startupCommandPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(startupCommandPlanNodeId,
                AzureConfigsUtility.getDependencyMetadata(startupCommandPlanNodeId,
                    StartupCommandParameters.builder().startupCommand(startupCommand).build(), kryoSerializer,
                    PlanCreatorConstants.STARTUP_COMMAND_STEP_PARAMETER))
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      startupCommandPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(startupCommandPlanNodeId, startupCommandPlanCreationResponse.build());
    return startupCommandPlanNodeId;
  }

  String addDependenciesForApplicationSettings(YamlNode serviceConfigNode,
      Map<String, PlanCreationResponse> planCreationResponseMap, ServiceConfig serviceConfig,
      KryoSerializer kryoSerializer) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    boolean isUseFromStage = serviceConfig.getUseFromStage() != null;
    YamlField applicationSettingsYamlField = AzureConfigsUtility.fetchAzureConfigYamlFieldAndSetYamlUpdates(
        serviceConfigNode, isUseFromStage, yamlUpdates, YamlTypes.APPLICATION_SETTINGS);
    String applicationSettingsYamlFieldPlanNodeId = "applicationSettings-" + UUIDGenerator.generateUuid();

    StoreConfigWrapper applicationSettings =
        ((AzureWebAppServiceSpec) serviceConfig.getServiceDefinition().getServiceSpec()).getApplicationSettings();
    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(applicationSettingsYamlFieldPlanNodeId, applicationSettingsYamlField);
    PlanCreationResponseBuilder applicationSettingsPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(applicationSettingsYamlFieldPlanNodeId,
                AzureConfigsUtility.getDependencyMetadata(applicationSettingsYamlFieldPlanNodeId,
                    ApplicationSettingsParameters.builder().applicationSettings(applicationSettings).build(),
                    kryoSerializer, PlanCreatorConstants.APPLICATION_SETTINGS_STEP_PARAMETER))
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      applicationSettingsPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(
        applicationSettingsYamlFieldPlanNodeId, applicationSettingsPlanCreationResponse.build());
    return applicationSettingsYamlFieldPlanNodeId;
  }

  String addDependenciesForConnectionStrings(YamlNode serviceConfigNode,
      Map<String, PlanCreationResponse> planCreationResponseMap, ServiceConfig serviceConfig,
      KryoSerializer kryoSerializer) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    boolean isUseFromStage = serviceConfig.getUseFromStage() != null;
    YamlField connectionStringsYamlField = AzureConfigsUtility.fetchAzureConfigYamlFieldAndSetYamlUpdates(
        serviceConfigNode, isUseFromStage, yamlUpdates, YamlTypes.CONNECTION_STRINGS);
    String connectionStringsYamlFieldPlanNodeId = "connectionStrings-" + UUIDGenerator.generateUuid();

    StoreConfigWrapper connectionStrings =
        ((AzureWebAppServiceSpec) serviceConfig.getServiceDefinition().getServiceSpec()).getConnectionStrings();
    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(connectionStringsYamlFieldPlanNodeId, connectionStringsYamlField);
    PlanCreationResponseBuilder connectionStringsPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(connectionStringsYamlFieldPlanNodeId,
                AzureConfigsUtility.getDependencyMetadata(connectionStringsYamlFieldPlanNodeId,
                    ConnectionStringsParameters.builder().connectionStrings(connectionStrings).build(), kryoSerializer,
                    PlanCreatorConstants.CONNECTION_STRINGS_STEP_PARAMETER))
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      connectionStringsPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(connectionStringsYamlFieldPlanNodeId, connectionStringsPlanCreationResponse.build());
    return connectionStringsYamlFieldPlanNodeId;
  }

  String addDependenciesForStartupCommandV2(YamlNode serviceV2Node,
      Map<String, PlanCreationResponse> planCreationResponseMap, NGServiceV2InfoConfig serviceV2Config,
      KryoSerializer kryoSerializer) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    YamlField startupCommandYamlField = AzureConfigsUtility.fetchAzureConfigYamlFieldAndSetYamlUpdates(
        serviceV2Node, false, yamlUpdates, YamlTypes.STARTUP_COMMAND);
    String startupCommandPlanNodeId = "startupCommand-" + UUIDGenerator.generateUuid();

    StoreConfigWrapper startupCommand =
        ((AzureWebAppServiceSpec) serviceV2Config.getServiceDefinition().getServiceSpec()).getStartupCommand();

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(startupCommandPlanNodeId, startupCommandYamlField);
    PlanCreationResponseBuilder startupCommandPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(startupCommandPlanNodeId,
                AzureConfigsUtility.getDependencyMetadata(startupCommandPlanNodeId,
                    StartupCommandParameters.builder().startupCommand(startupCommand).build(), kryoSerializer,
                    PlanCreatorConstants.STARTUP_COMMAND_STEP_PARAMETER))
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      startupCommandPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(startupCommandPlanNodeId, startupCommandPlanCreationResponse.build());
    return startupCommandPlanNodeId;
  }

  String addDependenciesForApplicationSettingsV2(YamlNode serviceV2Node,
      Map<String, PlanCreationResponse> planCreationResponseMap, NGServiceV2InfoConfig serviceV2Config,
      KryoSerializer kryoSerializer) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    YamlField applicationSettingsYamlField = AzureConfigsUtility.fetchAzureConfigYamlFieldAndSetYamlUpdates(
        serviceV2Node, false, yamlUpdates, YamlTypes.APPLICATION_SETTINGS);
    String applicationSettingsPlanNodeId = "applicationSettings-" + UUIDGenerator.generateUuid();

    StoreConfigWrapper applicationSettings =
        ((AzureWebAppServiceSpec) serviceV2Config.getServiceDefinition().getServiceSpec()).getApplicationSettings();

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(applicationSettingsPlanNodeId, applicationSettingsYamlField);
    PlanCreationResponseBuilder applicationSettingsPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(applicationSettingsPlanNodeId,
                AzureConfigsUtility.getDependencyMetadata(applicationSettingsPlanNodeId,
                    ApplicationSettingsParameters.builder().applicationSettings(applicationSettings).build(),
                    kryoSerializer, PlanCreatorConstants.APPLICATION_SETTINGS_STEP_PARAMETER))
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      applicationSettingsPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(applicationSettingsPlanNodeId, applicationSettingsPlanCreationResponse.build());
    return applicationSettingsPlanNodeId;
  }

  String addDependenciesForConnectionStringsV2(YamlNode serviceV2Node,
      Map<String, PlanCreationResponse> planCreationResponseMap, NGServiceV2InfoConfig serviceV2Config,
      KryoSerializer kryoSerializer) {
    YamlUpdates.Builder yamlUpdates = YamlUpdates.newBuilder();
    YamlField connectionStringsYamlField = AzureConfigsUtility.fetchAzureConfigYamlFieldAndSetYamlUpdates(
        serviceV2Node, false, yamlUpdates, YamlTypes.CONNECTION_STRINGS);
    String connectionStringsPlanNodeId = "connectionStrings-" + UUIDGenerator.generateUuid();

    StoreConfigWrapper connectionStrings =
        ((AzureWebAppServiceSpec) serviceV2Config.getServiceDefinition().getServiceSpec()).getConnectionStrings();

    Map<String, YamlField> dependenciesMap = new HashMap<>();
    dependenciesMap.put(connectionStringsPlanNodeId, connectionStringsYamlField);
    PlanCreationResponseBuilder connectionStringsPlanCreationResponse = PlanCreationResponse.builder().dependencies(
        DependenciesUtils.toDependenciesProto(dependenciesMap)
            .toBuilder()
            .putDependencyMetadata(connectionStringsPlanNodeId,
                AzureConfigsUtility.getDependencyMetadata(connectionStringsPlanNodeId,
                    ConnectionStringsParameters.builder().connectionStrings(connectionStrings).build(), kryoSerializer,
                    PlanCreatorConstants.CONNECTION_STRINGS_STEP_PARAMETER))
            .build());
    if (yamlUpdates.getFqnToYamlCount() > 0) {
      connectionStringsPlanCreationResponse.yamlUpdates(yamlUpdates.build());
    }
    planCreationResponseMap.put(connectionStringsPlanNodeId, connectionStringsPlanCreationResponse.build());
    return connectionStringsPlanNodeId;
  }

  boolean shouldCreatePlanNodeForManifests(ServiceConfig actualServiceConfig) {
    List<ManifestConfigWrapper> manifests = actualServiceConfig.getServiceDefinition().getServiceSpec().getManifests();

    // Contains either manifests or overrides or nothing.
    if (isNotEmpty(manifests)) {
      return true;
    }

    return actualServiceConfig.getStageOverrides() != null
        && actualServiceConfig.getStageOverrides().getManifests() != null
        && isNotEmpty(actualServiceConfig.getStageOverrides().getManifests());
  }

  boolean shouldCreatePlanNodeForManifestsV2(NGServiceV2InfoConfig serviceV2InfoConfig) {
    List<ManifestConfigWrapper> manifests = serviceV2InfoConfig.getServiceDefinition().getServiceSpec().getManifests();

    // Contains either manifests or not.
    return isNotEmpty(manifests);
  }

  boolean shouldCreatePlanNodeForConfigFilesV2(NGServiceV2InfoConfig serviceV2InfoConfig) {
    List<ConfigFileWrapper> configFiles = serviceV2InfoConfig.getServiceDefinition().getServiceSpec().getConfigFiles();

    return isNotEmpty(configFiles);
  }
}
