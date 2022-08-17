package io.harness.template.helpers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.SCMGitSyncHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class TemplateGitXHelper {
  @Inject SCMGitSyncHelper scmGitSyncHelper;

  public String getWorkingBranch(Scope scope, String entityRepoURL) {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    String branchName = gitEntityInfo.getBranch();
    String parentEntityRepoUrl;
    if (GitAwareContextHelper.isNullOrDefault(gitEntityInfo.getParentEntityRepoName()) && GitAwareContextHelper.isNullOrDefault(gitEntityInfo.getParentEntityConnectorRef())) {
      parentEntityRepoUrl = "";
    } else {
      parentEntityRepoUrl = scmGitSyncHelper
                                .getRepoUrl(scope, gitEntityInfo.getParentEntityRepoName(),
                                    gitEntityInfo.getParentEntityConnectorRef(), Collections.emptyMap())
                                .getRepoUrl();
    }
    if (!GitAwareContextHelper.isNullOrDefault(parentEntityRepoUrl) && !parentEntityRepoUrl.equals(entityRepoURL)) {
      branchName = "";
    }
    return branchName;
  }
}
