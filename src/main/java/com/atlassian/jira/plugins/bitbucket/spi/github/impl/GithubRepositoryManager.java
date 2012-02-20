package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugins.bitbucket.IssueLinker;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.UrlInfo;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubOAuth;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.net.MalformedURLException;
import java.net.URL;

public class GithubRepositoryManager extends DvcsRepositoryManager
{
    private static final Logger LOG = LoggerFactory.getLogger(GithubRepositoryManager.class);

    public static final String GITHUB = "github";

    private final GithubOAuth githubOAuth;

    public GithubRepositoryManager(RepositoryPersister repositoryPersister, @Qualifier("githubCommunicator") Communicator communicator,
                                   Encryptor encryptor, ApplicationProperties applicationProperties, IssueLinker issueLinker,
                                   TemplateRenderer templateRenderer, IssueManager issueManager, GithubOAuth githubOAuth)
    {
        super(communicator, repositoryPersister, encryptor, applicationProperties, issueLinker, templateRenderer, issueManager);
        this.githubOAuth = githubOAuth;
    }


    @Override
    public Changeset reloadChangeset(IssueMapping issueMapping)
    {
        Changeset reloadedChangeset = super.reloadChangeset(issueMapping);
        if (StringUtils.isNotBlank(issueMapping.getBranch()))
        {
            reloadedChangeset.setBranch(issueMapping.getBranch());
        }

        return reloadedChangeset;
    }


    @Override
    public String getRepositoryType()
    {
        return GITHUB;
    }
    
    public UrlInfo validateUrlInfo(UrlInfo urlInfo)
    {
        urlInfo = super.validateUrlInfo(urlInfo);
        if (StringUtils.isBlank(githubOAuth.getClientId()) || StringUtils.isBlank(githubOAuth.getClientSecret()))
        {
            String baseUrl = getApplicationProperties().getBaseUrl();
            urlInfo.addValidationError("<a href='"+baseUrl+"/secure/admin/ConfigureGithubOAuth!default.jspa'>GitHub OAuth Settings</a> have to be configured before adding GitHub repository");
        }
        return urlInfo;
    }

    @Override
    public RepositoryUri getRepositoryUri(String urlString)
    {
        try
        {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            String hostname = url.getHost();
            String path = url.getPath();
            String[] split = StringUtils.split(path, "/");
            if (split.length < 2)
            {
                throw new SourceControlException("Expected url is https://domainname.com/username/repository");
            }
            String owner = split[0];
            String slug = split[1];
            return new GithubRepositoryUri(protocol, hostname, owner, slug);
        } catch (MalformedURLException e)
        {
            throw new SourceControlException("Invalid url [" + urlString + "]");
        }
    }

    public boolean wasChangesetAlreadySynchronized(int repositoryId, String node) {
        final IssueMapping issueMapping = repositoryPersister.getIssueMapping(repositoryId, node);
        return issueMapping != null;
    }
}
