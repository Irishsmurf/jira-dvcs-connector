package it.restart.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.plugins.dvcs.pageobjects.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

import java.util.List;
import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Martin Skurla
 */
public class GreenHopperBoardPage implements Page
{
    @Inject
    WebDriver driver;

    @ElementBy(cssSelector="a[data-link-id='com.pyxis.greenhopper.jira:project-sidebar-plan-scrum'], #plan-toggle")
    PageElement boardPlanToggleViewButton;

    @ElementBy(tagName="body")
    PageElement bodyElement;


    @Override
    public String getUrl()
    {
        return "/secure/RapidBoard.jspa?rapidView=1&useStoredSettings=true";
    }

    public void goToQABoardPlan()
    {
        Poller.waitUntil(boardPlanToggleViewButton.timed().isVisible(), is(true), by(30, SECONDS));
        driver.findElement(By.tagName("body")).sendKeys(Keys.NUMPAD1);
        Poller.waitUntilTrue(bodyElement.find(By.id("ghx-plan")).timed().isVisible());
    }

    public void assertCommitsAppearOnIssue(String issueKey, int expectedNumberOfAssociatedCommits)
    {
        PageElement backlogContainerDiv = bodyElement.find(By.className("ghx-backlog-container"));

        PageElement qa1Div  = PageElementUtils.findTagWithAttributeValue(backlogContainerDiv, "div", "data-issue-key", issueKey);
        PageElement qa1Link = PageElementUtils.findTagWithAttributeValue(qa1Div,              "a",   "title",          issueKey);

        qa1Link.click();

        PageElement openIssueTabsMenu = bodyElement.find(By.className("ghx-detail-nav-menu"));
        Poller.waitUntil(openIssueTabsMenu.timed().isVisible(), is(true), by(15000));
        PageElement commitsTabLink = PageElementUtils.findTagWithAttributeValue(openIssueTabsMenu, "a", "title", "Commits");

        commitsTabLink.click();

        PageElement issueCommitsIntegrationDiv = bodyElement.find(By.id("ghx-tab-com-atlassian-jira-plugins-jira-bitbucket-connector-plugin-dvcs-commits-greenhopper-tab"));
        List<PageElement> commits = issueCommitsIntegrationDiv.findAll(By.className("CommitContainer"));

        assertThat(commits).hasSize(expectedNumberOfAssociatedCommits);
    }
}
