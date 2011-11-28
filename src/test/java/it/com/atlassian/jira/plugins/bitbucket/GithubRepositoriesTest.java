package it.com.atlassian.jira.plugins.bitbucket;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import com.atlassian.jira.plugins.bitbucket.pageobjects.page.GithubConfigureRepositoriesPage;

/**
 * Test to verify behaviour when syncing  github repository.
 */
public class GithubRepositoriesTest extends BitBucketBaseTest
{
    private static final String TEST_REPO_URL = "https://github.com/jirabitbucketconnector/test-project";
    private static final String TEST_PRIVATE_REPO_URL = "https://github.com/dusanhornik/my-private-github-repo";
    private static final String TEST_NOT_EXISTING_REPO_URL = "https://github.com/jirabitbucketconnector/repo-does-not-exist";

    @SuppressWarnings("rawtypes")
    @Override
    protected Class getPageClass()
    {
        return GithubConfigureRepositoriesPage.class;
    }

    @Test
    public void addRepoAppearsOnList()
    {
        configureRepos.deleteAllRepositories();
        configureRepos.addPublicRepoToProjectSuccessfully("QA", TEST_REPO_URL);
        assertThat(configureRepos.getRepositories().size(), equalTo(1));
    }

//    @Test
//    public void addRepoCommitsAppearOnIssues()
//    {
//        ensureRepositoryPresent("QA", TEST_REPO_URL);
//
//        assertThat(getCommitsForIssue("QA-2"),
//                hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
//        assertThat(getCommitsForIssue("QA-3"),
//                hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
//    }

    @Test
    public void addRepoThatDoesNotExist()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addRepoToProjectFailing("QA", TEST_NOT_EXISTING_REPO_URL);

        String errorMessage = configureRepos.getErrorStatusMessage();
        assertThat(errorMessage, containsString("Error!The repository url [" + TEST_NOT_EXISTING_REPO_URL + "] is incorrect or the repository is not responding."));
    }

    @Test
    public void addPrivateRepoAsPublic()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addPublicRepoToProjectSuccessfully("QA", TEST_PRIVATE_REPO_URL);

        String syncStatusMessage = configureRepos.getSyncStatusMessage();

        // TODO: remove following line and uncomment next line after GUI fix of showing sync_message div during synchronisation
        assertThat(syncStatusMessage, equalTo(""));
        //assertThat(syncStatusMessage, containsString("Sync Failed"));
    }

    @Test
    public void addPrivateRepo()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addPrivateRepoToProjectSuccessfully("QA", TEST_PRIVATE_REPO_URL);

        String syncStatusMessage = configureRepos.getSyncStatusMessage();

        // TODO: remove following line and uncomment next 2 lines after GUI fix of showing sync_message div during synchronisation
        assertThat(syncStatusMessage, equalTo(""));
        //assertThat(syncStatusMessage, containsString("Sync Finished"));
        // assertThat(syncStatusMessage, not(containsString("Sync Failed")));
    }
//
//    @Test
//    public void testPostCommitHookAdded() throws Exception
//    {
//        String servicesConfig;
//        String baseUrl = jira.getProductInstance().getBaseUrl();
//
//        configureRepos.deleteAllRepositories();
//        // add repository
//        String repoId = configureRepos.addPublicRepoToProjectAndInstallService("QA",
//                "https://bitbucket.org/jirabitbucketconnector/public-hg-repo", "jirabitbucketconnector",
//                "jirabitbucketconnector");
//        // check that it created postcommit hook
//        String syncUrl = baseUrl + "/rest/bitbucket/1.0/repository/" + repoId + "/sync";
//        String bitbucketServiceConfigUrl = "https://api.bitbucket.org/1.0/repositories/jirabitbucketconnector/public-hg-repo/services";
//        servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, "jirabitbucketconnector",
//                "jirabitbucketconnector");
//        assertThat(servicesConfig, containsString(syncUrl));
//        // delete repository
//        configureRepos.deleteAllRepositories();
//        // check that postcommit hook is removed
//        servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, "jirabitbucketconnector",
//                "jirabitbucketconnector");
//        assertThat(servicesConfig, not(containsString(syncUrl)));
//    }
//
//    private String getBitbucketServices(String url, String username, String password) throws Exception
//    {
//        HttpClient httpClient = new HttpClient();
//        HttpMethod method = new GetMethod(url);
//
//        AuthScope authScope = new AuthScope(method.getURI().getHost(), AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME);
//        httpClient.getParams().setAuthenticationPreemptive(true);
//        httpClient.getState().setCredentials(authScope, new UsernamePasswordCredentials(username, password));
//
//        httpClient.executeMethod(method);
//        return method.getResponseBodyAsString();
//    }
//
//    public void testSyncFromPostCommit()
//    {
//        // TODO
//    }
}
