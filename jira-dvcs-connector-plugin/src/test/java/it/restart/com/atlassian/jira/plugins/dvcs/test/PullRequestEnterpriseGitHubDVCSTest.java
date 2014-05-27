package it.restart.com.atlassian.jira.plugins.dvcs.test;

import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount.AccountType;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.testng.annotations.Test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseClientProvider;
import com.atlassian.pageobjects.TestedProductFactory;

/**
 * Pull request GitHub related tests.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Test
public class PullRequestEnterpriseGitHubDVCSTest extends BasePullRequestGitHubDVCSTest
{

    /**
     * Base URL of GitHub server.
     */
    private static final String GIT_HUB_BASE_URL = GithubEnterpriseTests.GITHUB_ENTERPRISE_URL;

    /**
     * @see JiraTestedProduct
     */
    private JiraTestedProduct jiraTestedProduct = TestedProductFactory.create(JiraTestedProduct.class);

    /**
     * @see GitHubTestResource
     */
    private GitHubTestResource gitHubResource = new GitHubTestResource(this, new MagicVisitor(jiraTestedProduct));

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addOrganizations()
    {
        OAuth oAuth = gitHubResource.addOAuth(GIT_HUB_BASE_URL, jiraTestedProduct.getProductInstance().getBaseUrl(),
                GitHubTestResource.Lifetime.DURING_CLASS);

        new MagicVisitor(jiraTestedProduct).visit(GithubLoginPage.class, GIT_HUB_BASE_URL).doLogin();
        OAuthCredentials oAuthCredentials = new OAuthCredentials(oAuth.key, oAuth.secret);
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.getPage().deleteAllOrganizations();

        RepositoriesPageController.AccountType accountType = RepositoriesPageController.AccountType.getGHEAccountType(GIT_HUB_BASE_URL);
        repositoriesPageController.addOrganization(accountType, GitHubTestResource.USER, oAuthCredentials, false);
        repositoriesPageController.addOrganization(accountType, GitHubTestResource.ORGANIZATION, oAuthCredentials, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setupGitHubResource(GitHubTestResource gitHubTestResource)
    {
        GitHubClient gitHubClient = GithubEnterpriseClientProvider.createClient(GIT_HUB_BASE_URL, "jira-dvcs-plugin-test");
        gitHubClient.setCredentials(GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD);
        gitHubTestResource.addOwner(GitHubTestResource.USER, gitHubClient);
        gitHubTestResource.addOwner(GitHubTestResource.ORGANIZATION, gitHubClient);

        GitHubClient gitHubClient2 = GithubEnterpriseClientProvider.createClient(GIT_HUB_BASE_URL, "jira-dvcs-plugin-test");
        gitHubClient2.setCredentials(GitHubTestResource.OTHER_USER, GitHubTestResource.OTHER_USER_PASSWORD);
        gitHubTestResource.addOwner(GitHubTestResource.OTHER_USER, gitHubClient2);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected AccountType getAccountType()
    {
        return AccountType.GIT_HUB_ENTERPRISE;
    }

    @Override
    protected void logoutFromGitHub()
    {
        new MagicVisitor(jiraTestedProduct).visit(GithubLoginPage.class, GIT_HUB_BASE_URL).doLogout();
    }

}
