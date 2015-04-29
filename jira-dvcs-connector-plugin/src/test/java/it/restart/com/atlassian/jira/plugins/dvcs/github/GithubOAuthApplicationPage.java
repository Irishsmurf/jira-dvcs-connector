package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;

/**
 *
 */
public class GithubOAuthApplicationPage implements Page
{
    @Inject
    private PageElementFinder pageElementFinder;

    @Inject
    private PageBinder pageBinder;

    private final String hostUrl;

    public GithubOAuthApplicationPage()
    {
        this("https://github.com");
    }

    public GithubOAuthApplicationPage(String hostUrl)
    {
        this.hostUrl = hostUrl;
    }

    @Override
    public String getUrl()
    {
        return hostUrl + "/settings/applications";
    }

    public void removeConsumer(final OAuth oAuth)
    {
        this.removeConsumer(StringUtils.removeStart(oAuth.applicationId, hostUrl));
    }

    public void removeConsumer(final String appUri)
    {
        pageElementFinder.find(By.xpath("//a[@href='" + appUri + "']")).click();
        pageBinder.bind(GithubOAuthPage.class).removeConsumer();
    }

    public void removeConsumerForAppName(final String appName)
    {
        PageElement element = pageElementFinder.find(By.linkText(appName));
        element.click();
        pageBinder.bind(GithubOAuthPage.class).removeConsumer();

        // wait for the page refresh
        waitUntilFalse("Deleted consumer " + appName + " should not be present", element.timed().isPresent());
    }
}
