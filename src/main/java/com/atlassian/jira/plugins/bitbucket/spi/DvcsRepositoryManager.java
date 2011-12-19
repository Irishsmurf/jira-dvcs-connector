package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.IssueLinker;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.streams.GlobalFilter;
import com.atlassian.jira.plugins.bitbucket.velocity.VelocityUtils;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DvcsRepositoryManager implements RepositoryManager, RepositoryUriFactory
{
    private static final Logger log = LoggerFactory.getLogger(DvcsRepositoryManager.class);

    public static final int MAX_VISIBLE_FILES = 5;

    private final RepositoryPersister repositoryPersister;
    private final Communicator communicator;
    private final Encryptor encryptor;
    private final ApplicationProperties applicationProperties;
    private final IssueLinker issueLinker;
    private final TemplateRenderer templateRenderer;

    /* Maps ProjectMapping to SourceControlRepository */
    private final Function<ProjectMapping, SourceControlRepository> TO_SOURCE_CONTROL_REPOSITORY = new Function<ProjectMapping, SourceControlRepository>()
    {
        @Override
        public SourceControlRepository apply(ProjectMapping pm)
        {
            String decryptedPassword = encryptor.decrypt(pm.getPassword(), pm.getProjectKey(), pm.getRepositoryUrl());
            String decryptedAdminPassword = encryptor.decrypt(pm.getAdminPassword(), pm.getProjectKey(),
                    pm.getRepositoryUrl());
            return new DefaultSourceControlRepository(pm.getID(), pm.getRepositoryType(), getRepositoryUri(pm.getRepositoryUrl()),
                    pm.getProjectKey(), pm.getUsername(), decryptedPassword,
                    pm.getAdminUsername(), decryptedAdminPassword, pm.getAccessToken());
        }
    };

    private final Function<IssueMapping, Changeset> toChangesetTransformer;

    public DvcsRepositoryManager(Communicator communicator, RepositoryPersister repositoryPersister, Encryptor encryptor,
                                 ApplicationProperties applicationProperties, IssueLinker issueLinker, TemplateRenderer templateRenderer)
    {
        this.communicator = communicator;
        this.repositoryPersister = repositoryPersister;
        this.encryptor = encryptor;
        this.applicationProperties = applicationProperties;
        this.issueLinker = issueLinker;
        this.templateRenderer = templateRenderer;

        toChangesetTransformer = new ToChangesetTransformer(this);
    }

    public void validateRepositoryAccess(String repositoryType, String projectKey, String repositoryUrl, String username,
                                         String password, String adminUsername, String adminPassword, String accessToken) throws SourceControlException
    {
        RepositoryUri repositoryUri = getRepositoryUri(repositoryUrl);
        getCommunicator().validateRepositoryAccess(repositoryType, projectKey, repositoryUri, username, password, adminUsername, adminPassword, accessToken);
    }

    @Override
    public SourceControlRepository addRepository(String repositoryType, String projectKey, String repositoryUrl, String username,
                                                 String password, String adminUsername, String adminPassword, String accessToken)
    {
        // Remove trailing slashes from URL
        if (repositoryUrl.endsWith("/"))
        {
            repositoryUrl = repositoryUrl.substring(0, repositoryUrl.length() - 1);
        }

        // Set all URLs to HTTPS
        if (repositoryUrl.startsWith("http:"))
        {
            repositoryUrl = repositoryUrl.replaceFirst("http:", "https:");
        }
        validateRepositoryAccess(repositoryType, projectKey, repositoryUrl, username, password, adminUsername, adminPassword, accessToken);

        String encryptedPassword = encryptor.encrypt(password, projectKey, repositoryUrl);
        String encryptedAdminPassword = encryptor.encrypt(adminPassword, projectKey, repositoryUrl);
        ProjectMapping pm = repositoryPersister.addRepository(repositoryType, projectKey, repositoryUrl, username,
                encryptedPassword, adminUsername, encryptedAdminPassword, accessToken);
        return TO_SOURCE_CONTROL_REPOSITORY.apply(pm);
    }

    @Override
    public SourceControlRepository getRepository(int repositoryId)
    {
        ProjectMapping repository = repositoryPersister.getRepository(repositoryId);
        return TO_SOURCE_CONTROL_REPOSITORY.apply(repository);
    }

    @Override
    public List<SourceControlRepository> getRepositories(String projectKey)
    {
        List<ProjectMapping> repositories = repositoryPersister.getRepositories(projectKey, getRepositoryType());
        return Lists.transform(repositories, TO_SOURCE_CONTROL_REPOSITORY);
    }

    @Override
    public List<Changeset> getChangesets(String issueKey)
    {
        List<IssueMapping> issueMappings = repositoryPersister.getIssueMappings(issueKey, getRepositoryType());
        return Lists.transform(issueMappings, toChangesetTransformer);
    }

    @Override
    public void removeRepository(int id)
    {
        repositoryPersister.removeRepository(id);
    }

    @Override
    public void addChangeset(SourceControlRepository repository, String issueId, Changeset changeset)
    {
        repositoryPersister.addChangeset(issueId, changeset);
    }

    @Override
    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        return getCommunicator().getUser(repository, username);
    }


    @Override
    public String getHtmlForChangeset(SourceControlRepository repository, Changeset changeset)
    {
        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("velocity_utils", new VelocityUtils());
        templateMap.put("issue_linker", issueLinker);
        templateMap.put("changeset", changeset);
        templateMap.put("repository", repository);

        String documentJpgUrl = getApplicationProperties().getBaseUrl() + "/download/resources/com.atlassian.jira.plugins.jira-bitbucket-connector-plugin/images/document.jpg";
        templateMap.put("document_jpg_url", documentJpgUrl);

        String authorName = changeset.getRawAuthor();
        String login = changeset.getAuthor();
        String commitURL = changeset.getCommitURL(repository);
        SourceControlUser user = getUser(repository, changeset.getAuthor());
        String gravatarUrl = user.getAvatar().replace("s=32", "s=60");
        String commitMessage = changeset.getMessage();  //TODO add functional test for rendering issue numbers as links

        templateMap.put("gravatar_url", gravatarUrl);
        templateMap.put("user_url", repository.getRepositoryUri().getUserUrl(CustomStringUtils.encode(login)));
        templateMap.put("login", login);
        templateMap.put("user_name", authorName);
        templateMap.put("commit_message", commitMessage);
        templateMap.put("commit_url", commitURL);
        templateMap.put("commit_hash", changeset.getNode());


        StringWriter sw = new StringWriter();
        try
        {
            templateRenderer.render("/templates/com/atlassian/jira/plugins/bitbucket/issuetabpanels/commits-view.vm", templateMap, sw);
        } catch (IOException e)
        {
            log.warn(e.getMessage(), e);
        }
        return sw.toString();
    }

    @Override
    public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progressProvider)
    {
        return new DefaultSynchronisationOperation(key, this, getCommunicator(), progressProvider);
    }

    protected boolean hasValidFormat(String url)
    {
        // Valid URL
        Pattern p = Pattern.compile("^(https|http)://[a-zA-Z0-9][-a-zA-Z0-9]*(.[a-zA-Z0-9]+)+/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher m = p.matcher(url);
        return m.matches();
    }

    @Override
    public abstract String getRepositoryType();

    public ApplicationProperties getApplicationProperties()
    {
        return applicationProperties;
    }

    public Communicator getCommunicator()
    {
        return communicator;
    }

    @Override
    public UrlInfo getUrlInfo(String repositoryUrl)
    {
        if (!hasValidFormat(repositoryUrl)) return null;
        return getCommunicator().getUrlInfo(getRepositoryUri(repositoryUrl));
    }

    @Override
    public Changeset reloadChangeset(IssueMapping issueMapping)
    {
        ProjectMapping pm = repositoryPersister.getRepository(issueMapping.getRepositoryId());
        SourceControlRepository repository = TO_SOURCE_CONTROL_REPOSITORY.apply(pm);
        Changeset changeset = getCommunicator().getChangeset(repository, issueMapping.getNode());
        repositoryPersister.addChangeset(issueMapping.getIssueId(), changeset);
        return changeset;
    }

    @Override
    public void setupPostcommitHook(SourceControlRepository repo)
    {
        getCommunicator().setupPostcommitHook(repo, getPostCommitUrl(repo));
    }

    private String getPostCommitUrl(SourceControlRepository repo)
    {
        return getApplicationProperties().getBaseUrl() + "/rest/bitbucket/1.0/repository/" + repo.getId() + "/sync";
    }

    @Override
    public void removePostcommitHook(SourceControlRepository repo)
    {
        getCommunicator().removePostcommitHook(repo, getPostCommitUrl(repo));
    }

    @Override
    public Set<Changeset> getLatestChangesets(int count, GlobalFilter gf)
    {
        List<IssueMapping> latestIssueMappings = repositoryPersister.getLatestIssueMappings(count, gf, getRepositoryType());
        List<Changeset> changesets = Lists.transform(latestIssueMappings, toChangesetTransformer);
        return Sets.newHashSet(changesets);
    }
}
