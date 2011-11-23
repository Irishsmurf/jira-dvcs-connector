package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.CommunicatorHelper;
import com.atlassian.jira.plugins.bitbucket.spi.CommunicatorHelper.ExtendedResponse;
import com.atlassian.jira.plugins.bitbucket.spi.CommunicatorHelper.ExtendedResponseHandler;
import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.UrlInfo;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubUserFactory;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;

public class GithubCommunicator implements Communicator
{
    private final Logger logger = LoggerFactory.getLogger(GithubCommunicator.class);

    private final AuthenticationFactory authenticationFactory;
    private final CommunicatorHelper communicatorHelper;

    public GithubCommunicator(RequestFactory<?> requestFactory, AuthenticationFactory authenticationFactory)
    {
        this.authenticationFactory = authenticationFactory;
        this.communicatorHelper = new CommunicatorHelper(requestFactory);
    }

    @Override
    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        try
        {
            RepositoryUri uri = repository.getRepositoryUri();
            logger.debug("parse user [ {} ]", username);
            Authentication authentication = authenticationFactory.getAuthentication(repository);
            String responseString = communicatorHelper.get(authentication, "/user/show/" + CustomStringUtils.encode(username), null, uri.getApiUrl());
            return GithubUserFactory.parse(new JSONObject(responseString).getJSONObject("user"));
        } catch (ResponseException e)
        {
            logger.debug("could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        } catch (JSONException e)
        {
            logger.debug("could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        }
    }

    @Override
    public Changeset getChangeset(SourceControlRepository repository, String id)
    {
        try
        {
            RepositoryUri uri = repository.getRepositoryUri();
            String owner = uri.getOwner();
            String slug = uri.getSlug();
            Authentication authentication = authenticationFactory.getAuthentication(repository);

            logger.debug("parse gihchangeset [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, id });
            String responseString = communicatorHelper.get(authentication, "/commits/show/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/" + CustomStringUtils.encode(id), null,
                    uri.getApiUrl());

            // TODO; branch
            return GithubChangesetFactory.parse(repository.getId(), "master", new JSONObject(responseString).getJSONObject("commit"));
        } catch (ResponseException e)
        {
            throw new SourceControlException("could not get result", e);
        } catch (JSONException e)
        {
            throw new SourceControlException("could not parse json result", e);
        }
    }

    public List<Changeset> getChangesets(SourceControlRepository repository, String branch, int pageNumber)
    {
        RepositoryUri uri = repository.getRepositoryUri();
        String owner = uri.getOwner();
        String slug = uri.getSlug();
        Authentication authentication = authenticationFactory.getAuthentication(repository);

        logger.debug("parse github changesets [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, String.valueOf(pageNumber) });

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("page", String.valueOf(pageNumber));

        List<Changeset> changesets = new ArrayList<Changeset>();

        ExtendedResponseHandler responseHandler = new ExtendedResponseHandler();
        try
        {
            communicatorHelper.get(authentication, "/commits/list/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/" + branch, params, uri.getApiUrl(), responseHandler);
            
            ExtendedResponse extendedResponse = responseHandler.getExtendedResponse();

            if (extendedResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new SourceControlException("Incorrect credentials");
            } else if (extendedResponse.getStatusCode() == HttpStatus.SC_NOT_FOUND)
            {
                // no more changesets
                return Collections.emptyList();
            }
            
            String responseString = extendedResponse.getResponseString();
            JSONArray list = new JSONObject(responseString).getJSONArray("commits");
            for (int i = 0; i < list.length(); i++)
            {
                changesets.add(GithubChangesetFactory.parse(repository.getId(), branch, list.getJSONObject(i)));
            }
        } catch (ResponseException e)
        {
            logger.debug("Could not get changesets from page: {}", pageNumber, e);
            throw new SourceControlException("Error requesting changesets. Page: " + pageNumber + ". [" + e.getMessage() + "]", e);
        } catch (JSONException e)
        {
            logger.debug("Could not parse repositories from page: {}", pageNumber);
            return Collections.emptyList();
        }

        return changesets;
    }

    @Override
    public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Iterable<Changeset> getChangesets(final SourceControlRepository repository)
    {
        return new Iterable<Changeset>()
        {
            @Override
            public Iterator<Changeset> iterator()
            {
                List<String> branches = getBranches(repository);
                return new GithubChangesetIterator(GithubCommunicator.this, repository, branches);
            }
        };
    }

    @Override
    public UrlInfo getUrlInfo(final RepositoryUri repositoryUri)
    {
        logger.debug("get repository info in bitbucket [ {} ]", repositoryUri.getRepositoryUrl());
        Boolean repositoryPrivate = communicatorHelper.isRepositoryPrivate1(repositoryUri);
        if (repositoryPrivate == null) return null;
        return new UrlInfo(GithubRepositoryManager.GITHUB, repositoryPrivate.booleanValue());
    }

    private List<String> getBranches(SourceControlRepository repository)
    {
        List<String> branches = new ArrayList<String>();
        RepositoryUri repositoryUri = repository.getRepositoryUri();
        String owner = repositoryUri.getOwner();
        String slug = repositoryUri.getSlug();

        logger.debug("get list of branches in github repository [ {} ]", slug);

        try
        {
            String responseString = communicatorHelper.get(Authentication.ANONYMOUS, "/repos/show/" +
                    CustomStringUtils.encode(owner) + "/" + CustomStringUtils.encode(slug) + "/branches", null, repositoryUri.getApiUrl());

            JSONArray list = new JSONObject(responseString).getJSONObject("branches").names();
            for (int i = 0; i < list.length(); i++)
            {
                branches.add(list.getString(i));
            }
        } catch (Exception e)
        {
            logger.info("Can not obtain branches list from repository [ {} ]", slug);
            // we have to use at least master branch
            return Arrays.asList("master");
        }

        return branches;

    }
}
