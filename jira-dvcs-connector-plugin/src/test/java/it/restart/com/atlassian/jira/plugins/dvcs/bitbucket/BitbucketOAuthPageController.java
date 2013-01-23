package it.restart.com.atlassian.jira.plugins.dvcs.bitbucket;

import it.restart.com.atlassian.jira.plugins.dvcs.JiraBitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.common.PageController;

import com.atlassian.jira.pageobjects.JiraTestedProduct;

public class BitbucketOAuthPageController implements PageController<BitbucketOAuthPage>
{
    private final JiraTestedProduct jira;
    private BitbucketOAuthPage page;
    private OAuth oAuth;

    public BitbucketOAuthPageController(JiraTestedProduct jira)
    {
        this.jira = jira;
        this.page = new MagicVisitor(jira).visit(BitbucketOAuthPage.class);
    }

    public BitbucketOAuthPageController setupOAuth()
    {
        oAuth = page.addConsumer();
        JiraBitbucketOAuthPage jiraBitbucketOAuthPage = jira.visit(JiraBitbucketOAuthPage.class);
        jiraBitbucketOAuthPage.setCredentials(oAuth.key, oAuth.secret);
        return this;
    }

    public void removeOAuth()
    {
        page = new MagicVisitor(jira).visit(BitbucketOAuthPage.class);
        page.removeConsumer(oAuth.applicationId);
    }

    @Override
    public BitbucketOAuthPage getPage()
    {
        return page;
    }

}