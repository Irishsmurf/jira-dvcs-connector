package it.restart.com.atlassian.jira.plugins.dvcs.bitbucket;

import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class BitbucketGrantAccessPage implements Page
{    
    @ElementBy(tagName= "body")
    private PageElement bodyElement;
    
    @Override
    public String getUrl()
    {
        throw new UnsupportedOperationException();
    }
    
    public void grantAccess()
    {
        PageElement buttonsDiv = bodyElement.find(By.className("buttons"));
        PageElement grantAccessButton = PageElementUtils.findTagWithAttributeValue(buttonsDiv, "button", "type", "submit");
        grantAccessButton.click();
    }
}
