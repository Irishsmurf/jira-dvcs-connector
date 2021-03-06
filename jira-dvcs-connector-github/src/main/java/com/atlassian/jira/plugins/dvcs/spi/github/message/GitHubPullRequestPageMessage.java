package com.atlassian.jira.plugins.dvcs.spi.github.message;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;

import java.util.Set;

/**
 * Message for a particular GitHub pull request page
 */
public class GitHubPullRequestPageMessage extends BaseProgressEnabledMessage
{
    private final int page;
    private final int pagelen;
    private final Set<Long> processedPullRequests;

    public GitHubPullRequestPageMessage(final Progress progress, final int syncAuditId, final boolean softSync, final Repository repository, final int page, final int pagelen, final Set<Long> processedPullRequests, boolean webHookSync)
    {
        super(progress, syncAuditId, softSync, repository, webHookSync);
        this.page = page;
        this.pagelen = pagelen;
        this.processedPullRequests = processedPullRequests;
    }

    public int getPage()
    {
        return page;
    }

    public int getPagelen()
    {
        return pagelen;
    }

    public Set<Long> getProcessedPullRequests()
    {
        return processedPullRequests;
    }
}
