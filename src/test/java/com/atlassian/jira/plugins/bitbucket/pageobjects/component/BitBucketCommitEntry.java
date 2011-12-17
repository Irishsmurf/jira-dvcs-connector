package com.atlassian.jira.plugins.bitbucket.pageobjects.component;

import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

/**
 * Represents a commit entry that is displayed in the <tt>BitBucketIssuePanel</tt>
 */
public class BitBucketCommitEntry
{
    private final PageElement div;

    public BitBucketCommitEntry(PageElement div)
    {
        this.div = div;
    }

    /**
     * The message associated with this commit
     * @return Message
     */
    public String getCommitMessage()
    {
        return div.findAll(By.className("Text")).get(0).getText();
    }
}
