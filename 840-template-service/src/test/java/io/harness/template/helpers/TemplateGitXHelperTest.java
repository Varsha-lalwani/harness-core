package io.harness.template.helpers;

import static io.harness.rule.OwnerRule.ADITHYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;

import io.harness.beans.Scope;
import io.harness.category.element.UnitTests;
import io.harness.context.GlobalContext;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.manage.GlobalContextManager;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.io.IOException;


@OwnedBy(HarnessTeam.PL)
public class TemplateGitXHelperTest {

    @InjectMocks TemplateGitXHelper templateGitXHelper;

    @Mock SCMGitSyncHelper scmGitSyncHelper;

    private static final String BranchName = "branch";
    private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
    private static final String ORG_IDENTIFIER = "orgIdentifier";
    private static final String PROJECT_IDENTIFIER = "projectIdentifier";
    private static final String ENTITY_REPO_URL = "https://github.com/adivishy1/testRepo";



    @Before
    public void setUp() throws IOException {

//        on(templateGitXHelper).set("scmGitSyncHelper", scmGitSyncHelper);
    }

    @Test
    @Owner(developers = ADITHYA)
    @Category(UnitTests.class)
    public void testGetWorkingBranch() {

        GitEntityInfo branchInfo = GitEntityInfo.builder().branch(BranchName).build();
        setupGitContext(branchInfo);
        Scope scope = Scope.of(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
        assertThat(templateGitXHelper.getWorkingBranch(scope, ENTITY_REPO_URL)).isEqualTo(BranchName);
        branchInfo = GitEntityInfo.builder().branch(BranchName).parentEntityRepoURL(ENTITY_REPO_URL).build();
        setupGitContext(branchInfo);
        assertThat(templateGitXHelper.getWorkingBranch(scope, "random repo url")).isEqualTo("");
        assertThat(templateGitXHelper.getWorkingBranch(scope, ENTITY_REPO_URL)).isEqualTo(BranchName);
    }

    private void setupGitContext(GitEntityInfo branchInfo) {
        if (!GlobalContextManager.isAvailable()) {
            GlobalContextManager.set(new GlobalContext());
        }
        GlobalContextManager.upsertGlobalContextRecord(GitSyncBranchContext.builder().gitBranchInfo(branchInfo).build());
    }
}
