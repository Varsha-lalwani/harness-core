/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngtriggers.helpers;

import static java.lang.String.format;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.RestClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class UrlHelper {
  @Inject private AccountClient accountClient;

  public String getBaseUrl(String accountIdentifier) {
    return RestClientUtils.getResponse(accountClient.getBaseUrl(accountIdentifier));
  }

  public String buildApiExecutionUrl(UriInfo uriInfo, String uuid, String accountIdentifier) {
    return format("%swebhook/triggerExecutionDetails/%s?accountIdentifier=%s", uriInfo.getBaseUri().toString(), uuid,
        accountIdentifier);
  }

  public String buildUiUrl(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    return format("%s#/account/%s/cd/orgs/%s/projects/%s/deployments?pipelineIdentifier=%s&page=1",
        getBaseUrl(accountIdentifier), accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier);
  }

  public String buildUiSetupUrl(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    return format("%s#/account/%s/cd/orgs/%s/projects/%s/pipelines/%s/pipeline-studio/", getBaseUrl(accountIdentifier),
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier);
  }
}
