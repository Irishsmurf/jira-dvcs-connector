package it.com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.pageobjects.page.GithubConfigureRepositoriesPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.GithubOAuthConfigPage;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import static com.atlassian.jira.plugins.bitbucket.pageobjects.CommitMessageMatcher.withMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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

    @Test
    public void addRepoCommitsAppearOnIssues()
    {
        ensureRepositoryPresent("QA", TEST_REPO_URL);

        assertThat(getCommitsForIssue("QA-2"),
                hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
        assertThat(getCommitsForIssue("QA-3"),
                hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
    }

    @Test
    public void addRepoThatDoesNotExist()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addRepoToProjectFailingStep1("QA", TEST_NOT_EXISTING_REPO_URL);

        String errorMessage = configureRepos.getErrorStatusMessage();
        assertThat(errorMessage, containsString("Error!The repository url [" + TEST_NOT_EXISTING_REPO_URL + "] is incorrect or the repository is not responding."));
    }

    @Test
    public void addPrivateRepoWithInvalidOAuth()
    {
        configureRepos.deleteAllRepositories();

        goToGithubOAuthConfigPage().setCredentials("xxx", "yyy");

        goToRepositoriesConfigPage();

        configureRepos.addRepoToProjectFailingStep2("QA", TEST_PRIVATE_REPO_URL);

        //String errorStatusMessage = configureRepos.getErrorStatusMessage();

        //assertThat(errorStatusMessage, containsString("Error!"));
    }

    @Test
    public void addPrivateRepoWithValidOAuth()
    {
        configureRepos.deleteAllRepositories();

        goToGithubOAuthConfigPage().setCredentials(GithubOAuthConfigPage.VALID_CLIENT_ID, GithubOAuthConfigPage.VALID_CLIENT_SECRET);

        goToRepositoriesConfigPage();

        configureRepos.addPrivateRepoToProjectSuccessfully("QA", TEST_PRIVATE_REPO_URL);

//        String syncStatusMessage = configureRepos.getSyncStatusMessage();
//
//        assertThat(syncStatusMessage, containsString("Sync Finished"));
//        assertThat(syncStatusMessage, not(containsString("Sync Failed")));
    }


    @Test
    public void testPostCommitHookAdded() throws Exception
    {
        String servicesConfig;
        String baseUrl = jira.getProductInstance().getBaseUrl();

        configureRepos.deleteAllRepositories();
        // add repository
        String repoId = configureRepos.addPublicRepoToProjectAndInstallService("QA",
                TEST_REPO_URL, "jirabitbucketconnector",
                "jirabitbucketconnector1");
        // check that it created postcommit hook
        String syncUrl = baseUrl + "/rest/bitbucket/1.0/repository/" + repoId + "/sync";
        String githubServiceConfigUrl = "hhttps://api.github.com/1.0/repositories/jirabitbucketconnector/test=project/services";
        servicesConfig = getGithubServices(githubServiceConfigUrl, "jirabitbucketconnector",
                "jirabitbucketconnector1");
        assertThat(servicesConfig, containsString(syncUrl));
        // delete repository
        configureRepos.deleteAllRepositories();
        // check that postcommit hook is removed
        servicesConfig = getGithubServices(githubServiceConfigUrl, "jirabitbucketconnector",
                "jirabitbucketconnector1");
        assertThat(servicesConfig, not(containsString(syncUrl)));
    }

    private String getGithubServices(String url, String username, String password) throws Exception
    {
        HttpClient httpClient = new HttpClient();
        HttpMethod method = new GetMethod(url);

        AuthScope authScope = new AuthScope(method.getURI().getHost(), AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME);
        httpClient.getParams().setAuthenticationPreemptive(true);
        httpClient.getState().setCredentials(authScope, new UsernamePasswordCredentials(username, password));

        httpClient.executeMethod(method);
        return method.getResponseBodyAsString();
    }

    public void testSyncFromPostCommit()
    {
        // TODO
    }

}
