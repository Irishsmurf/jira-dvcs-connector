package it.com.atlassian.jira.plugins.dvcs;

import static com.atlassian.jira.plugins.dvcs.pageobjects.BitBucketCommitEntriesAssert.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

import it.com.atlassian.jira.plugins.dvcs.BitBucketBaseOrgTest.AnotherLoginPage;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubEnterpriseConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubEnterpriseOAuthConfigPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubOAuthConfigPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubRegisterOAuthAppPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubRegisteredOAuthAppsPage;
import com.atlassian.jira.plugins.dvcs.util.HttpSenderUtils;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.pageobjects.elements.PageElement;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test to verify behaviour when syncing  github repository.
 */
public class GithubEnterpriseOrganizationsTest extends BitBucketBaseOrgTest<GithubEnterpriseConfigureOrganizationsPage>
{
    public static final String GITHUB_ENTERPRISE_URL = "http://192.168.2.47";
    
    private static final String TEST_ORGANIZATION = "jirabitbucketconnector";
//    private static final String TEST_NOT_EXISTING_URL = "mynotexistingaccount124";
    private static final String REPO_ADMIN_LOGIN = "jirabitbucketconnector";
    private static final String REPO_ADMIN_PASSWORD = PasswordUtil.getPassword("jirabitbucketconnector");

    private static String clientID;
    private static String clientSecret;
    private static String oauthAppLink;

    @BeforeClass
    public static void registerAppToGithub()
    {
        jira.getTester().gotoUrl(GITHUB_ENTERPRISE_URL + GithubLoginPage.PAGE_PATH);
        GithubLoginPage ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogin();

        // find out secrets
        jira.getTester().gotoUrl(GITHUB_ENTERPRISE_URL + GithubRegisterOAuthAppPage.PAGE_PATH);
        GithubRegisterOAuthAppPage registerAppPage = jira.getPageBinder().bind(GithubRegisterOAuthAppPage.class);
        String oauthAppName = "testApp" + System.currentTimeMillis();
        String baseUrl = jira.getProductInstance().getBaseUrl();
        registerAppPage.registerApp(oauthAppName, baseUrl, baseUrl);
        clientID = registerAppPage.getClientId().getText();
        clientSecret = registerAppPage.getClientSecret().getText();

        // find out app URL
        jira.getTester().gotoUrl( GITHUB_ENTERPRISE_URL + GithubRegisteredOAuthAppsPage.PAGE_PATH);
        GithubRegisteredOAuthAppsPage registeredOAuthAppsPage = jira.getPageBinder().bind(GithubRegisteredOAuthAppsPage.class);
        registeredOAuthAppsPage.parseClientIdAndSecret(oauthAppName);
        oauthAppLink = registeredOAuthAppsPage.getOauthAppUrl();
        jira.getTester().gotoUrl(GITHUB_ENTERPRISE_URL + GithubLoginPage.PAGE_PATH);
        ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogout();

        jira.getPageBinder().navigateToAndBind(AnotherLoginPage.class).loginAsSysAdmin(GithubEnterpriseOAuthConfigPage.class);
        GithubOAuthConfigPage oauthConfigPage = jira.getPageBinder().navigateToAndBind(GithubEnterpriseOAuthConfigPage.class);
        oauthConfigPage.setCredentials(clientID, clientSecret);
        
        // logout jira
        jira.getTester().getDriver().manage().deleteAllCookies();
    }

    @AfterClass
    public static void deregisterAppToGithub()
    {
       /* jira.getTester().gotoUrl(GithubLoginPage.LOGOUT_ACTION_URL);

        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        GithubLoginPage ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogin();
*/

        jira.getTester().gotoUrl(oauthAppLink);

        GithubRegisterOAuthAppPage registerAppPage = jira.getPageBinder().bind(GithubRegisterOAuthAppPage.class);
        registerAppPage.deleteOAuthApp();

        jira.getTester().gotoUrl(GITHUB_ENTERPRISE_URL + GithubLoginPage.PAGE_PATH);
        GithubLoginPage ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogout();
    }

    @BeforeMethod
    public void removeExistingPostCommitHooks()
    {
        String[] githubRepositories = { "repo1", "test-project" };
        for (String githubRepositoryId : githubRepositories)
        {
            Set<String> extractedGithubHookIds = extractGithubHookIdsForRepositoryToRemove(githubRepositoryId);
            for (String extractedGithubHookId : extractedGithubHookIds)
            {
                removePostCommitHook(githubRepositoryId, extractedGithubHookId);
            }
        }
    }

    @AfterMethod
    public void deleteRepositoriesAfterTest()
    {
        goToConfigPage();
        configureOrganizations.deleteAllOrganizations();
    }

    @Override
    protected Class<GithubEnterpriseConfigureOrganizationsPage> getConfigureOrganizationsPageClass()
    {
        return GithubEnterpriseConfigureOrganizationsPage.class;
    }

    @Test
    public void addOrganization()
    {
        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, false);
        assertThat(configureOrganizations.getOrganizations()).hasSize(1);
    }

    @Test
    public void shouldBeAbleToSeePrivateRepositoriesFromTeamAccount()
    {
        // we should see 'private-dvcs-connector-test' repo
        configureOrganizations.addOrganizationSuccessfully("atlassian", false);

        assertThat(configureOrganizations.containsRepositoryWithName("private-dvcs-connector-test")).isTrue();
    }

//TODO Commenting out the test as for github:enterprise the account validation is disabled
//    @Test
//    public void addUrlThatDoesNotExist()
//    {
//        configureOrganizations.addOrganizationFailingStep1(TEST_NOT_EXISTING_URL);
//
//        String errorMessage = configureOrganizations.getErrorStatusMessage();
//        assertThat(errorMessage).contains("Invalid user/team account.");
//        configureOrganizations.clearForm();
//    }

    @Test
    public void testPostCommitHookAdded() throws Exception
    {
        //TODO delete rest call doesn't work for http
        if ( !GITHUB_ENTERPRISE_URL.startsWith("http://") )
        {
            String baseUrl = jira.getProductInstance().getBaseUrl();
    
            // add repository
            configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, true);
    
            // check that it created postcommit hook
            String githubServiceConfigUrlPath = baseUrl + "/rest/bitbucket/1.0/repository/";
            String hooksURL = GITHUB_ENTERPRISE_URL + "/jirabitbucketconnector/test-project/admin/hooks";
            String hooksPage = getGithubServices(hooksURL, REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);
            assertThat(hooksPage).contains(githubServiceConfigUrlPath);
            goToConfigPage();
            // delete repository
            configureOrganizations.deleteAllOrganizations();
            // check that postcommit hook is removed
            hooksPage = getGithubServices(hooksURL, REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);
            assertThat(hooksPage).doesNotContain(githubServiceConfigUrlPath);
        }
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

    @Test
    public void addRepoCommitsAppearOnIssues()
    {
        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, true);

        assertThat(getCommitsForIssue("QA-2",6)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        assertThat(getCommitsForIssue("QA-3",1)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
    }

    @Test
    public void testCommitStatistics()
    {
        configureOrganizations.deleteAllOrganizations();
        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, true);

        // QA-2
        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-3",1);
        assertThat(commitMessages).hasSize(1);
//        Assert.assertEquals("Expected 1 commit", 1, commitMessages.size());
        BitBucketCommitEntry commitMessage = commitMessages.get(0);
        List<PageElement> statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.getAdditions(statistics.get(0))).isEqualTo("+1");
        assertThat(commitMessage.getDeletions(statistics.get(0))).isEqualTo("-");

//        Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
//        Assert.assertEquals("Expected Additions: 1", commitMessage.getAdditions(statistics.get(0)), "+1");
//        Assert.assertEquals("Expected Deletions: -", commitMessage.getDeletions(statistics.get(0)), "-");

        // QA-4
        commitMessages = getCommitsForIssue("QA-4",1);
        assertThat(commitMessages).hasSize(1);
//        Assert.assertEquals("Expected 1 commit", 1, commitMessages.size());
        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();
//        Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
//        Assert.assertTrue("Expected commit resource Added: 1", commitMessage.isAdded(statistics.get(0)));
    }


    @Test
    public void addPrivateRepoWithInvalidOAuth()
    {
        goToGithubOAuthConfigPage().setCredentials("xxx", "yyy");

        goToConfigPage();

        configureOrganizations.addRepoToProjectFailingStep2();

        goToGithubOAuthConfigPage().setCredentials(clientID, clientSecret);
    }

    @Test
    public void addPrivateRepositoryWithValidOAuth()
    {
        GithubConfigureOrganizationsPage githubConfigPage = (GithubEnterpriseConfigureOrganizationsPage) goToConfigPage();

        GithubConfigureOrganizationsPage githubConfigureOrganizationsPage = githubConfigPage
                .addRepoToProjectForOrganization("jirabitbucketconnectorprivate");

        assertThat(githubConfigureOrganizationsPage.getNumberOfVisibleRepositories()).isEqualTo(2);
    }

    @Override
    protected GithubOAuthConfigPage goToGithubOAuthConfigPage()
    {
        return jira.visit(GithubEnterpriseOAuthConfigPage.class);
    }
    
    private static Set<String> extractGithubHookIdsForRepositoryToRemove(String repositoryId)
    {
        String listHooksResponseString;
        String finalGithubUrl = String.format(GITHUB_ENTERPRISE_URL + "/api/v3/repos/jirabitbucketconnector/%s/hooks",
                                              repositoryId);

        try
        {
            listHooksResponseString = HttpSenderUtils.sendGetHttpRequest(finalGithubUrl,
                                                                         REPO_ADMIN_LOGIN,
                                                                         REPO_ADMIN_PASSWORD);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot extract BitBucket service IDs !", e);
        }

        Set<String> extractedHookIds = new LinkedHashSet<String>();

        // parsing following JSON:
        //[
        //  {
        //    "url": "https://api.github.com/repos/octocat/Hello-World/hooks/1",
        //    "updated_at": "2011-09-06T20:39:23Z",
        //    "created_at": "2011-09-06T17:26:27Z",
        //    "name": "web",
        //    "events": [
        //      "push"
        //    ],
        //    "active": true,
        //    "config": {
        //      "url": "http://example.com",
        //      "content_type": "json"
        //    },
        //    "id": 1
        //  }
        //]

        try {
            JSONArray jsonArray = new JSONArray(listHooksResponseString);

            for (int i = 0; i < jsonArray.length(); i++)
            {
                JSONObject data = (JSONObject) jsonArray.get(i);

                String githubHookId = data.getString("id");
                JSONObject configObject = data.getJSONObject("config");
                String configURL = configObject.getString("url");

                if (configURL.contains(jira.getProductInstance().getBaseUrl())) {
                    extractedHookIds.add(githubHookId);
                }
            }
        }
        catch (JSONException e)
        {
            throw new IllegalStateException("Cannot parse JSON !", e);
        }

        return extractedHookIds;
    }

    private static void removePostCommitHook(String repositoryId, String serviceId) {
        String finalGithubUrl = String.format(
                GITHUB_ENTERPRISE_URL + "/api/v3/repos/jirabitbucketconnector/%s/hooks/%s",
                repositoryId,
                serviceId);
        try
        {
            HttpSenderUtils.sendDeleteHttpRequest(finalGithubUrl, REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot send DELETE Http request !", e);
        }
    }
}