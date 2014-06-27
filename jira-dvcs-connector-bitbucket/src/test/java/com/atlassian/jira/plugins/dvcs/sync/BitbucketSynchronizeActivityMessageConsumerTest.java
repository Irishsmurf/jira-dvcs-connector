package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilder;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranch;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLinks;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommitAuthor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestHead;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestParticipant;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketUser;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.google.common.collect.Lists;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Test
 */
public class BitbucketSynchronizeActivityMessageConsumerTest
{
    @Mock
    private MessagingService messagingService;
    @Mock
    private BitbucketClientBuilderFactory bitbucketClientBuilderFactory;
    @Mock
    private RepositoryPullRequestDao repositoryPullRequestDao;
    @Mock
    private PullRequestService pullRequestService;
    @Mock
    private RepositoryDao repositoryDao;

    @InjectMocks
    private BitbucketSynchronizeActivityMessageConsumer testedClass;

    @Mock
    private Progress progress;

    @Mock
    private Repository repository;

    @Mock
    private BitbucketRemoteClient bitbucketRemoteClient;

    @Mock
    private BitbucketRemoteClient cachedBitbucketRemoteClient;

    @Mock
    private RemoteRequestor requestor;

    @Mock
    private RemoteRequestor cachedRequestor;

    @Mock
    private BitbucketPullRequest bitbucketPullRequest;

    @Mock
    private BitbucketSynchronizeActivityMessage payload;

    @Mock
    private Message<BitbucketSynchronizeActivityMessage> message;

    @Captor
    private ArgumentCaptor<Map> savePullRequestCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Participant>> participantsIndexCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> saveCommitCaptor;

    @Mock
    private BitbucketPullRequestUpdateActivity activity;

    private class BuilderAnswer implements Answer<Object>
    {
        private boolean cached;

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable
        {
            Object builderMock = invocation.getMock();
            if (invocation.getMethod().getReturnType().isInstance(builderMock))
            {
                if (invocation.getMethod().getName().equals("cached"))
                {
                    cached = true;
                }
                return builderMock;
            } else
            {
                return cached ? cachedBitbucketRemoteClient : bitbucketRemoteClient;
            }
        }
    }

    @BeforeMethod
    private void init()
    {
        testedClass = null;

        MockitoAnnotations.initMocks(this);

        when(repository.getOrgName()).thenReturn("org");
        when(repository.getSlug()).thenReturn("repo");
        when(bitbucketPullRequest.getId()).thenReturn(1L);
        when(bitbucketPullRequest.getUpdatedOn()).thenReturn(new Date());

        when(bitbucketRemoteClient.getPullRequestAndCommentsRemoteRestpoint()).thenReturn(new PullRequestRemoteRestpoint(requestor));
        when(cachedBitbucketRemoteClient.getPullRequestAndCommentsRemoteRestpoint()).thenReturn(new PullRequestRemoteRestpoint(cachedRequestor));

        BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> activityPage = Mockito.mock(BitbucketPullRequestPage.class);

        String activityUrl = String.format("/repositories/%s/%s/pullrequests/activity?pagelen=%s&page=", repository.getOrgName(), repository.getSlug(), PullRequestRemoteRestpoint.REPO_ACTIVITY_PAGESIZE);
        String pullRequestDetailUrl = String.format("/repositories/%s/%s/pullrequests/%s", repository.getOrgName(), repository.getSlug(), bitbucketPullRequest.getId());

        when(requestor.get(Mockito.startsWith(activityUrl), anyMap(), any(ResponseCallback.class))).thenReturn(activityPage);
        when(requestor.get(eq(pullRequestDetailUrl), anyMap(), any(ResponseCallback.class))).thenReturn(bitbucketPullRequest);
        when(cachedRequestor.get(Mockito.startsWith(activityUrl), anyMap(), any(ResponseCallback.class))).thenReturn(activityPage);
        when(cachedRequestor.get(eq(pullRequestDetailUrl), anyMap(), any(ResponseCallback.class))).thenReturn(bitbucketPullRequest);

        BitbucketPullRequestActivityInfo activityInfo = Mockito.mock(BitbucketPullRequestActivityInfo.class);

        when(activityPage.getValues()).thenReturn(Lists.newArrayList(activityInfo));

        when(activityInfo.getActivity()).thenReturn(activity);
        when(activity.getDate()).thenReturn(new Date());
        when(activity.getUpdatedOn()).thenReturn(new Date());
        BitbucketPullRequestHead source = mock(BitbucketPullRequestHead.class);
        BitbucketPullRequestRepository sourceRepository = mock(BitbucketPullRequestRepository.class);
        when(source.getRepository()).thenReturn(sourceRepository);
        when(activity.getSource()).thenReturn(source);

        when(activityInfo.getPullRequest()).thenReturn(bitbucketPullRequest);
        when(bitbucketClientBuilderFactory.forRepository(repository)).then(new Answer<BitbucketClientBuilder>()
        {
            @Override
            public BitbucketClientBuilder answer(final InvocationOnMock invocation) throws Throwable
            {
                BuilderAnswer builderAnswer = new BuilderAnswer();
                BitbucketClientBuilder bitbucketClientBuilder = mock(BitbucketClientBuilder.class, builderAnswer);
                return bitbucketClientBuilder;
            }
        });

        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);
        when(payload.getPageNum()).thenReturn(1);

        RepositoryPullRequestMapping pullRequestMapping = Mockito.mock(RepositoryPullRequestMapping.class);
        Date updatedOn = bitbucketPullRequest.getUpdatedOn();
        long remoteId = bitbucketPullRequest.getId();
        when(pullRequestMapping.getUpdatedOn()).thenReturn(updatedOn);
        when(pullRequestMapping.getRemoteId()).thenReturn(remoteId);
        when(repositoryPullRequestDao.savePullRequest(eq(repository), savePullRequestCaptor.capture())).thenReturn(pullRequestMapping);

        BitbucketLinks links = mockLinks();
        when(bitbucketPullRequest.getLinks()).thenReturn(links);
    }

    @Test
    public void testSourceBranchDeleted()
    {
        BitbucketPullRequestHead source = Mockito.mock(BitbucketPullRequestHead.class);
        when(source.getRepository()).thenReturn(Mockito.mock(BitbucketPullRequestRepository.class));
        when(source.getBranch()).thenReturn(null);
        when(bitbucketPullRequest.getSource()).thenReturn(source);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), anyString(), anyString(), anyString(), any(RepositoryPullRequestMapping.Status.class), any(Date.class), anyString(), anyInt());
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), any(Map.class));
    }

    @Test
    public void testSourceRepositoryDeleted()
    {
        when(bitbucketPullRequest.getSource()).thenReturn(null);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), anyString(), anyString(), anyString(), any(RepositoryPullRequestMapping.Status.class), any(Date.class), anyString(), anyInt());
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), any(Map.class));
    }

    @Test(expectedExceptions = BitbucketRequestException.Unauthorized_401.class)
    public void testAccessDenied()
    {
        when(requestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.Unauthorized_401());
        when(cachedRequestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.Unauthorized_401());

        testedClass.onReceive(message, payload);
    }

    @Test(expectedExceptions = BitbucketRequestException.NotFound_404.class)
    public void testNotFound()
    {
        when(requestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.NotFound_404());
        when(cachedRequestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.NotFound_404());

        testedClass.onReceive(message, payload);
    }

    @Test(expectedExceptions = BitbucketRequestException.InternalServerError_500.class)
    public void testInternalServerError()
    {
        when(requestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.InternalServerError_500());
        when(cachedRequestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.InternalServerError_500());

        testedClass.onReceive(message, payload);
    }

    @Test
    public void testNoAuthor()
    {
        BitbucketPullRequestHead source = mockRef("branch");
        BitbucketPullRequestHead destination = mockRef("master");
        when(bitbucketPullRequest.getSource()).thenReturn(source);
        when(bitbucketPullRequest.getDestination()).thenReturn(destination);

        when(bitbucketPullRequest.getAuthor()).thenReturn(null);

        testedClass.onReceive(message, payload);

        assertNull(savePullRequestCaptor.getValue().get(RepositoryPullRequestMapping.AUTHOR));
    }

    @Test
    public void testEmptyTitle()
    {
        BitbucketPullRequestHead source = mockRef("branch");
        BitbucketPullRequestHead destination = mockRef("master");
        when(bitbucketPullRequest.getSource()).thenReturn(source);
        when(bitbucketPullRequest.getDestination()).thenReturn(destination);

        when(bitbucketPullRequest.getTitle()).thenReturn(null);

        testedClass.onReceive(message, payload);

        assertNull(savePullRequestCaptor.getValue().get(RepositoryPullRequestMapping.NAME));
    }

    @Test
    public void testNoParticipants()
    {
        BitbucketPullRequestHead source = mockRef("branch");
        BitbucketPullRequestHead destination = mockRef("master");
        when(bitbucketPullRequest.getSource()).thenReturn(source);
        when(bitbucketPullRequest.getDestination()).thenReturn(destination);

        when(bitbucketPullRequest.getParticipants()).thenReturn(Collections.<BitbucketPullRequestParticipant>emptyList());

        testedClass.onReceive(message, payload);

        verify(pullRequestService).updatePullRequestParticipants(anyInt(), anyInt(), participantsIndexCaptor.capture());
        assertTrue(participantsIndexCaptor.getValue().isEmpty());
    }

    @Test
    public void testCacheOnlyFirstPage()
    {
        BitbucketPullRequestHead source = mockRef("branch");
        BitbucketPullRequestHead destination = mockRef("master");
        when(bitbucketPullRequest.getSource()).thenReturn(source);
        when(bitbucketPullRequest.getDestination()).thenReturn(destination);

        when(payload.getPageNum()).thenReturn(1);
        testedClass.onReceive(message, payload);
        verify(cachedRequestor, times(1)).get(anyString(), anyMap(), any(ResponseCallback.class));
    }

    @Test
    public void testNoCacheSecondPage()
    {
        BitbucketPullRequestHead source = mockRef("branch");
        BitbucketPullRequestHead destination = mockRef("master");
        when(bitbucketPullRequest.getSource()).thenReturn(source);
        when(bitbucketPullRequest.getDestination()).thenReturn(destination);

        when(payload.getPageNum()).thenReturn(2);
        testedClass.onReceive(message, payload);
        verify(cachedRequestor, never()).get(anyString(), anyMap(), any(ResponseCallback.class));
    }

    @Test
    public void testCommit() throws IOException
    {
        BitbucketPullRequestHead source = mockRef("branch");
        BitbucketPullRequestHead destination = mockRef("master");
        when(bitbucketPullRequest.getSource()).thenReturn(source);
        when(bitbucketPullRequest.getDestination()).thenReturn(destination);
        when(activity.getState()).thenReturn(RepositoryPullRequestMapping.Status.OPEN.name());

        final BitbucketPullRequestCommit commit = mock(BitbucketPullRequestCommit.class);
        when(commit.getHash()).thenReturn("aaa");
        BitbucketPullRequestCommitAuthor commitAuthor = mock(BitbucketPullRequestCommitAuthor.class);
        BitbucketUser user = mock(BitbucketUser.class);
        when(commitAuthor.getUser()).thenReturn(user);

        when(commit.getAuthor()).thenReturn(commitAuthor);
        BitbucketPullRequestPage<BitbucketPullRequestCommit> commitsPage = mock(BitbucketPullRequestPage.class);
        when(commitsPage.getValues()).thenReturn(Lists.newArrayList(commit));

        when(requestor.get(startsWith("commitsLink"), anyMap(), any(ResponseCallback.class))).thenReturn(commitsPage);

        testedClass.onReceive(message, payload);
        verify(repositoryPullRequestDao).saveCommit(eq(repository), saveCommitCaptor.capture());

        assertEquals(saveCommitCaptor.getValue().get(RepositoryCommitMapping.NODE), "aaa");
    }

    private BitbucketLinks mockLinks()
    {
        BitbucketLinks bitbucketLinks = new BitbucketLinks();
        bitbucketLinks.setHtml(mockLink("htmlLink"));
        bitbucketLinks.setCommits(mockLink("commitsLink"));

        return bitbucketLinks;
    }

    private BitbucketLink mockLink(final String link)
    {
        BitbucketLink bitbucketLink = mock(BitbucketLink.class);
        when(bitbucketLink.getHref()).thenReturn(link);
        return bitbucketLink;
    }

    private BitbucketPullRequestHead mockRef(String branchName)
    {
        BitbucketPullRequestHead source = Mockito.mock(BitbucketPullRequestHead.class);
        BitbucketBranch bitbucketBranch = new BitbucketBranch();
        bitbucketBranch.setName(branchName);
        when(source.getRepository()).thenReturn(Mockito.mock(BitbucketPullRequestRepository.class));
        when(source.getBranch()).thenReturn(bitbucketBranch);

        return source;
    }
}