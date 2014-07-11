package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.google.common.collect.Iterables;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.EventService;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Resource;

/**
 * Message consumer for {@link com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage}
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public class GitHubPullRequestPageMessageConsumer implements MessageConsumer<GitHubPullRequestPageMessage>
{
    public static final String QUEUE = GitHubPullRequestPageMessageConsumer.class.getCanonicalName();
    public static final String ADDRESS = GitHubPullRequestPageMessageConsumer.class.getCanonicalName();

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.service.message.MessagingService} dependency.
     */
    @Resource
    private MessagingService messagingService;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider} dependency.
     */
    @Resource(name = "githubClientProvider")
    private GithubClientProvider gitHubClientProvider;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.sync.GitHubPullRequestProcessor} dependency.
     */
    @Resource
    private GitHubPullRequestProcessor gitHubPullRequestProcessor;

    /**
     * Injected {@link GitHubEventService} dependency.
     */
    @Resource
    private GitHubEventService gitHubEventService;

    @Override
    public void onReceive(final Message<GitHubPullRequestPageMessage> message, final GitHubPullRequestPageMessage payload)
    {
        Repository repository = payload.getRepository();
        int page = payload.getPage();
        int pagelen = payload.getPagelen();

        CustomPullRequestService pullRequestService = gitHubClientProvider.getPullRequestService(repository);
        EventService eventService = gitHubClientProvider.getEventService(repository);

        RepositoryId repositoryId = RepositoryId.createFromUrl(repository.getRepositoryUrl());

        if (page == 1)
        {
            // saving the first event as save point
            // GitHub doesn't support per_page parameter for events, therefore 30 events will be downloaded and saved
            // leaving the page size set to 1 in case that this will change in the future to request only the first event
            PageIterator<Event> eventsPages = eventService.pageEvents(repositoryId, 1);
            for (Event event : Iterables.getFirst(eventsPages, Collections.<Event>emptyList()))
            {
                gitHubEventService.saveEvent(repository, event, true);
            }
        }

        PageIterator<PullRequest> pullRequestsPages = pullRequestService.pagePullRequests(repositoryId, CustomPullRequestService.STATE_ALL, CustomPullRequestService.SORT_CREATED, CustomPullRequestService.DIRECTION_ASC, page, pagelen);
        Collection<PullRequest> pullRequests = Iterables.getFirst(pullRequestsPages, null);
        if (pullRequests != null)
        {
            for (PullRequest pullRequest : pullRequests)
            {
                gitHubPullRequestProcessor.processPullRequest(repository, pullRequest);
            }
        }

        if (pullRequestsPages.hasNext())
        {
            fireNextPage(message, payload, pullRequestsPages.getNextPage());
        }
    }

    private void fireNextPage(Message<GitHubPullRequestPageMessage> message, GitHubPullRequestPageMessage payload, int nextPage)
    {
        GitHubPullRequestPageMessage nextMessage = new GitHubPullRequestPageMessage(payload.getProgress(), payload.getSyncAuditId(), payload.isSoftSync(), payload.getRepository(), nextPage, payload.getPagelen());
        messagingService.publish(getAddress(), nextMessage, message.getTags());
    }

    @Override
    public String getQueue()
    {
        return QUEUE;
    }

    @Override
    public MessageAddress<GitHubPullRequestPageMessage> getAddress()
    {
        return messagingService.get(GitHubPullRequestPageMessage.class, ADDRESS);
    }

    @Override
    public int getParallelThreads()
    {
        return MessageConsumer.THREADS_PER_CONSUMER;
    }
}
