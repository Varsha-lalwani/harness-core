/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.utils;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.ws.rs.core.UriInfo;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
@Slf4j
public class CustomWebhookTriggerResponseUtils {
  public String buildApiExecutionUrl(UriInfo uriInfo, String uuid, String accountIdentifier) {
    return format("%swebhook/triggerExecutionDetails/%s?accountIdentifier=%s", uriInfo.getBaseUri().toString(), uuid,
        accountIdentifier);
  }

  public String buildUiUrl(String baseUiUrl, String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier) {
    return format("%s#/account/%s/cd/orgs/%s/projects/%s/deployments?pipelineIdentifier=%s&page=1", baseUiUrl,
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier);
  }

  public String buildUiSetupUrl(String baseUiUrl, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier) {
    return format("%s#/account/%s/cd/orgs/%s/projects/%s/pipelines/%s/pipeline-studio/", baseUiUrl, accountIdentifier,
        orgIdentifier, projectIdentifier, pipelineIdentifier);
  }
}
