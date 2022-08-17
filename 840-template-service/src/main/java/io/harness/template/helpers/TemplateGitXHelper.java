package io.harness.template.helpers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.context.GlobalContext;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.manage.GlobalContextManager;

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
    String parentEntityRepoUrl = getRepoUrl(scope);
    if (!GitAwareContextHelper.isNullOrDefault(parentEntityRepoUrl) && !parentEntityRepoUrl.equals(entityRepoURL)) {
      branchName = "";
    }
    return branchName;
  }

  public String getRepoUrl(Scope scope) {
    GitEntityInfo gitEntityInfo = GitAwareContextHelper.getGitRequestParamsInfo();
    if (!GitAwareContextHelper.isNullOrDefault(gitEntityInfo.getParentEntityRepoUrl())) {
      return gitEntityInfo.getParentEntityRepoUrl();
    }
    String parentEntityRepoUrl;
    if (GitAwareContextHelper.isNullOrDefault(gitEntityInfo.getParentEntityRepoName())
        && GitAwareContextHelper.isNullOrDefault(gitEntityInfo.getParentEntityConnectorRef())) {
      parentEntityRepoUrl = "";
    } else {
      parentEntityRepoUrl = scmGitSyncHelper
                                .getRepoUrl(scope, gitEntityInfo.getParentEntityRepoName(),
                                    gitEntityInfo.getParentEntityConnectorRef(), Collections.emptyMap())
                                .getRepoUrl();

      gitEntityInfo.setParentEntityRepoUrl(parentEntityRepoUrl);
      setupGitContext(gitEntityInfo);
    }
    return parentEntityRepoUrl;
  }

  private void setupGitContext(GitEntityInfo branchInfo) {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(GitSyncBranchContext.builder().gitBranchInfo(branchInfo).build());
  }
}
