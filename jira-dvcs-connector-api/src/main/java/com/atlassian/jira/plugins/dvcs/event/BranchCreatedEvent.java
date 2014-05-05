package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.dvcs.model.Branch;

import java.util.Set;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link com.atlassian.jira.plugins.dvcs.model.Branch} was created.
 */
@SuppressWarnings ("UnusedDeclaration")
@EventName("jira.dvcsconnector.sync.branch.created")
public final class BranchCreatedEvent
{
    @Nonnull
    private final Branch branch;

    @Nonnull
    private final Set<String> issueKeys;

    public BranchCreatedEvent(final @Nonnull Branch branch, final @Nonnull Set<String> issueKeys)
    {
        this.issueKeys = issueKeys;
        this.branch = checkNotNull(branch, "branch");
    }

    @Nonnull
    public Branch getBranch()
    {
        return branch;
    }

    @Nonnull
    public Set<String> getIssueKeys()
    {
        return issueKeys;
    }
}
