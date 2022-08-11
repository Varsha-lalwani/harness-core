/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.CDC)
public class EnvironmentValidationHelper {
  @Inject private EnvironmentService environmentService;

  public boolean checkThatEnvExists(@NotEmpty String accountIdentifier, @NotEmpty String orgIdentifier,
      @NotEmpty String projectIdentifier, @NotEmpty String envIdentifier) {
    Optional<Environment> environment =
        environmentService.get(accountIdentifier, orgIdentifier, projectIdentifier, envIdentifier, false);
    if (!environment.isPresent()) {
      throw new NotFoundException(String.format("environment [%s] not found.", envIdentifier));
    }
    return true;
  }

  public static void checkDuplicateManifestIdentifiersWithIn(List<ManifestConfigWrapper> manifests) {
    if (isEmpty(manifests)) {
      return;
    }
    Set<String> uniqueIds = new HashSet<>();
    Set<String> duplicateIds = new HashSet<>();
    manifests.stream().map(ManifestConfigWrapper::getManifest).map(ManifestConfig::getIdentifier).forEach(id -> {
      if (!uniqueIds.add(id)) {
        duplicateIds.add(id);
      }
    });
    if (isNotEmpty(duplicateIds)) {
      throw new InvalidRequestException(format("Found duplicate manifest identifiers [%s]",
          duplicateIds.stream().map(Object::toString).collect(Collectors.joining(","))));
    }
  }
}
