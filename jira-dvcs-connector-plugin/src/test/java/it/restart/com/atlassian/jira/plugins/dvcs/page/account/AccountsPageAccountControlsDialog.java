package it.restart.com.atlassian.jira.plugins.dvcs.page.account;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * Controls dialog of {@link AccountsPageAccount}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class AccountsPageAccountControlsDialog extends WebDriverElement
{

    /**
     * Reference to "Refresh List" link.
     */
    @ElementBy(linkText = "Refresh list")
    private PageElement refreshLink;

    /**
     * Reference "Re-generate OAuth Access Token" link.
     */
    @ElementBy(linkText = "Re-generate OAuth Access Token")
    private PageElement regenerateLink;

    /**
     * Constructor.
     * 
     * @param locator
     * @param parent
     * @param timeoutType
     */
    public AccountsPageAccountControlsDialog(By locator, WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(locator, parent, timeoutType);
    }

    /**
     * Refreshes repositories list of account.
     */
    public void refresh()
    {
        refreshLink.click();
    }

    /**
     * Regenerates account OAuth.
     */
    public void regenerate()
    {
        regenerateLink.click();
    }

}