package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.remote.BranchTip;
import com.atlassian.jira.plugins.dvcs.service.remote.BranchedChangesetIterator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.JsonParsingException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranch;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranchesAndTags;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceField;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.DetailedChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.GroupTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.RepositoryTransformer;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.jira.plugins.dvcs.util.Retryer;
import com.atlassian.plugin.PluginAccessor;

/**
 * The Class BitbucketCommunicator.
 *
 */
public class BitbucketCommunicator implements DvcsCommunicator
{
    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(BitbucketCommunicator.class);

    /** The Constant BITBUCKET. */
    private static final String BITBUCKET = "bitbucket";

    private final BitbucketLinker bitbucketLinker;
    private final String pluginVersion;
    private final BitbucketClientBuilder bitbucketClientBuilder;

    private final ChangesetCache changesetCache;

    /**
     * The Constructor.
     *
     * @param bitbucketLinker
     * @param pluginAccessor
     * @param oauth
     * @param bitbucketAuthProviderFactory
     */
    public BitbucketCommunicator(@Qualifier("defferedBitbucketLinker") BitbucketLinker bitbucketLinker,
            PluginAccessor pluginAccessor, BitbucketClientBuilder bitbucketClientBuilder,
            ChangesetCache changesetCache)
   {
        this.bitbucketLinker = bitbucketLinker;
        this.bitbucketClientBuilder = bitbucketClientBuilder;
        this.changesetCache = changesetCache;
        this.pluginVersion = DvcsConstants.getPluginVersion(pluginAccessor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDvcsType()
    {
        return BITBUCKET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilder.noAuthClient(hostUrl).build();

            // just to call the rest
            remoteClient.getAccountRest().getUser(accountName);
            return new AccountInfo(BitbucketCommunicator.BITBUCKET);
        } catch (BitbucketRequestException e)
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Repository> getRepositories(Organization organization)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilder.forOrganization(organization).cached().build();
            List<BitbucketRepository> repositories = remoteClient.getRepositoriesRest().getAllRepositories(
                    organization.getName());
            return RepositoryTransformer.fromBitbucketRepositories(repositories);
        } catch (BitbucketRequestException.Unauthorized_401 e)
        {
            log.debug("Invalid credentials", e);
            throw new SourceControlException.UnauthorisedException("Invalid credentials", e);
        } catch ( BitbucketRequestException.BadRequest_400 e)
        {
            // We received bad request status code and we assume that an invalid OAuth is the cause
            throw new SourceControlException.UnauthorisedException("Invalid credentials");
        } catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException(e.getMessage(), e);
        } catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            if (organization.isIntegratedAccount())
            {
                throw new SourceControlException.UnauthorisedException("Unexpected response was returned back from server side. Check that all provided information of account '"
                        + organization.getName() + "' is valid. Basically it means: unexisting account or invalid key/secret combination.", e);
            }
            throw new SourceControlException.InvalidResponseException("The response could not be parsed. This is most likely caused by invalid credentials.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Changeset getChangeset(Repository repository, String node)
    {
        try
        {
            // get the changeset
            BitbucketRemoteClient remoteClient = bitbucketClientBuilder.forRepository(repository).build();
            BitbucketChangeset bitbucketChangeset = remoteClient.getChangesetsRest().getChangeset(repository.getOrgName(),
                            repository.getSlug(), node);

            Changeset fromBitbucketChangeset = ChangesetTransformer.fromBitbucketChangeset(repository.getId(), bitbucketChangeset);
            return fromBitbucketChangeset;
        } catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException("Could not get changeset [" + node + "] from " + repository.getRepositoryUrl(), e);
        } catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException.InvalidResponseException("Could not get changeset [" + node + "] from " + repository.getRepositoryUrl(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Changeset getDetailChangeset(Repository repository, Changeset changeset)
    {
        try
        {
            // get the commit statistics for changeset
            BitbucketRemoteClient remoteClient = bitbucketClientBuilder.forRepository(repository).build();
            List<BitbucketChangesetWithDiffstat> changesetDiffStat = remoteClient.getChangesetsRest().getChangesetDiffStat(repository.getOrgName(),
                    repository.getSlug(), changeset.getNode(), Changeset.MAX_VISIBLE_FILES);
            // merge it all
            return DetailedChangesetTransformer.fromChangesetAndBitbucketDiffstats(changeset, changesetDiffStat);
        } catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException("Could not get detailed changeset [" + changeset.getNode() + "] from " + repository.getRepositoryUrl(), e);
        } catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException.InvalidResponseException("Could not get changeset [" + changeset.getNode() + "] from " + repository.getRepositoryUrl(), e);
        }
    }

    @Override
    public Iterable<Changeset> getChangesets(final Repository repository)
    {
        return new Iterable<Changeset>()
        {
            @Override
            public Iterator<Changeset> iterator()
            {
                List<BranchTip> branches = getBranches(repository);
                return new BranchedChangesetIterator(changesetCache, BitbucketCommunicator.this, repository, branches);
            }

        };
    }

    private List<BranchTip> getBranches(Repository repository)
    {
        List<BranchTip> branchTips = new ArrayList<BranchTip>();
        BitbucketBranchesAndTags branchesAndTags = retrieveBranchesAndTags(repository);

            List<BitbucketBranch> bitbucketBranches = branchesAndTags.getBranches();
            for (BitbucketBranch bitbucketBranch : bitbucketBranches)
            {
                List<String> heads = bitbucketBranch.getHeads();
                for (String head : heads)
                {
                    // make sure "master" branch is first in the list
                    if ("master".equals(bitbucketBranch.getName()))
                    {
                        branchTips.add(0, new BranchTip(bitbucketBranch.getName(), head));
                    } else
                    {
                        branchTips.add(new BranchTip(bitbucketBranch.getName(), head));
                    }
                }
            }

        // Bitbucket returns raw_nodes for each branch, but changesetiterator works
        // with nodes. We need to use only first 12 characters from the node
        for (BranchTip branchTip : branchTips)
        {
            String rawNode = branchTip.getNode();
            if (StringUtils.length(rawNode)>12)
            {
                String node = rawNode.substring(0, 12);
                branchTip.setNode(node);
            }
        }
        return branchTips;
    }

    private BitbucketBranchesAndTags retrieveBranchesAndTags(final Repository repository)
    {
        return new Retryer<BitbucketBranchesAndTags>().retry(new Callable<BitbucketBranchesAndTags>()
        {
            @Override
            public BitbucketBranchesAndTags call() throws Exception
            {
                return getBranchesAndTags(repository);
            }
        });
    }

    private BitbucketBranchesAndTags getBranchesAndTags(Repository repository)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilder.forRepository(repository).cached().build();
            // Using undocumented https://api.bitbucket.org/1.0/repositories/atlassian/jira-bitbucket-connector/branches-tags
            return remoteClient.getBranchesAndTagsRemoteRestpoint().getBranchesAndTags(repository.getOrgName(),repository.getSlug());
        } catch (BitbucketRequestException e)
        {
            log.debug("Could not retrieve list of branches", e);
            throw new SourceControlException("Could not retrieve list of branches", e);
        } catch (JsonParsingException e)
        {
            log.debug("The response could not be parsed", e);
            throw new SourceControlException.InvalidResponseException("Could not retrieve list of branches", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupPostcommitHook(Repository repository, String postCommitUrl)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilder.forRepository(repository).cached().build();
            
            if (!hookDoesExist(repository, postCommitUrl, remoteClient)) {
	            remoteClient.getServicesRest().addPOSTService(repository.getOrgName(), // owner
	                    repository.getSlug(), postCommitUrl);
            }

        } catch (BitbucketRequestException e)
        {
            throw new SourceControlException.PostCommitHookRegistrationException("Could not add postcommit hook", e);
        }
    }

	private boolean hookDoesExist(Repository repository, String postCommitUrl,
            BitbucketRemoteClient remoteClient)
    {
	    List<BitbucketServiceEnvelope> services = remoteClient.getServicesRest().getAllServices(
	            repository.getOrgName(), // owner
	            repository.getSlug());
	    for (BitbucketServiceEnvelope bitbucketServiceEnvelope : services)
	    {
	        for (BitbucketServiceField serviceField : bitbucketServiceEnvelope.getService().getFields())
	        {
	            boolean fieldNameIsUrl = serviceField.getName().equals("URL");
	            boolean fieldValueIsRequiredPostCommitUrl = serviceField.getValue().equals(postCommitUrl);

	            if (fieldNameIsUrl && fieldValueIsRequiredPostCommitUrl)
	            {
	                return true;
	            }
	        }
	    }
	    return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkRepository(Repository repository, Set<String> withProjectkeys)
    {
        try
        {
            bitbucketLinker.linkRepository(repository, withProjectkeys);
        } catch (Exception e)
        {
            log.warn("Failed to link repository " + repository.getName() + " : " + e.getClass() + " :: "
                    + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkRepositoryIncremental(Repository repository, Set<String> withPossibleNewProjectkeys)
    {
        try
        {
            bitbucketLinker.linkRepositoryIncremental(repository, withPossibleNewProjectkeys);
        } catch (Exception e)
        {
            log.warn("Failed to do incremental repository linking " + repository.getName() + " : " + e.getClass()
                    + " :: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePostcommitHook(Repository repository, String postCommitUrl)
    {
        try
        {
            bitbucketLinker.unlinkRepository(repository);
            BitbucketRemoteClient remoteClient = bitbucketClientBuilder.forRepository(repository).build();
            List<BitbucketServiceEnvelope> services = remoteClient.getServicesRest().getAllServices(
                    repository.getOrgName(), // owner
                    repository.getSlug());

            for (BitbucketServiceEnvelope bitbucketServiceEnvelope : services)
            {
                for (BitbucketServiceField serviceField : bitbucketServiceEnvelope.getService().getFields())
                {
                    boolean fieldNameIsUrl = serviceField.getName().equals("URL");
                    boolean fieldValueIsRequiredPostCommitUrl = serviceField.getValue().equals(postCommitUrl);

                    if (fieldNameIsUrl && fieldValueIsRequiredPostCommitUrl)
                    {
                        remoteClient.getServicesRest().deleteService(repository.getOrgName(), // owner
                                repository.getSlug(), bitbucketServiceEnvelope.getId());
                    }
                }
            }
        } catch (BitbucketRequestException e)
        {
            throw new SourceControlException.PostCommitHookRegistrationException("Could not remove postcommit hook", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCommitUrl(Repository repository, Changeset changeset)
    {
        return MessageFormat.format("{0}/{1}/{2}/changeset/{3}?dvcsconnector={4}", repository.getOrgHostUrl(),
                repository.getOrgName(), repository.getSlug(), changeset.getNode(), pluginVersion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index)
    {
        return MessageFormat.format("{0}#chg-{1}", getCommitUrl(repository, changeset), file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DvcsUser getUser(Repository repository, String author)
    {
        BitbucketRemoteClient remoteClient = bitbucketClientBuilder.forRepository(repository).timeout(2000).build();
        BitbucketAccount bitbucketAccount = remoteClient.getAccountRest().getUser(author);
        String username = bitbucketAccount.getUsername();
        String fullName = bitbucketAccount.getFirstName() + " " + bitbucketAccount.getLastName();
        String avatar = bitbucketAccount.getAvatar();
        return new DvcsUser(username, fullName, null, avatar, repository.getOrgHostUrl() + "/" + username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DvcsUser getTokenOwner(Organization organization)
    {
        BitbucketRemoteClient remoteClient = bitbucketClientBuilder.forOrganization(organization).build();
        BitbucketAccount bitbucketAccount = remoteClient.getAccountRest().getCurrentUser();
        String username = bitbucketAccount.getUsername();
        String fullName = bitbucketAccount.getFirstName() + " " + bitbucketAccount.getLastName();
        String avatar = bitbucketAccount.getAvatar();
        return new DvcsUser(username, fullName, null, avatar, organization.getHostUrl() + "/" + username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Group> getGroupsForOrganization(Organization organization)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilder.forOrganization(organization).build();
            List<BitbucketGroup> groups = remoteClient.getGroupsRest().getGroups(organization.getName()); // owner

            return GroupTransformer.fromBitbucketGroups(groups);

        } catch (BitbucketRequestException.Forbidden_403 e)
        {
            log.debug("Could not get groups for organization [" + organization.getName() + "]");
            throw new SourceControlException.Forbidden_403(e);

        } catch (BitbucketRequestException e)
        {
            log.debug("Could not get groups for organization [" + organization.getName() + "]");
            throw new SourceControlException(e);

        } catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException.InvalidResponseException("Could not parse response [" + organization.getName()
                    + "]. This is most likely caused by invalid credentials.", e);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsInvitation(Organization organization)
    {
        return organization.getDvcsType().equalsIgnoreCase(BITBUCKET);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilder.forOrganization(organization).build();
            for (String groupSlug : groupSlugs)
            {
                log.debug("Going invite " + userEmail + " to group " + groupSlug + " of bitbucket organization "
                        + organization.getName());
                remoteClient.getAccountRest().inviteUser(organization.getName(), userEmail, organization.getName(),
                        groupSlug);
            }
        } catch (BitbucketRequestException exception)
        {
            log.warn("Failed to invite user {} to organization {}. Response HTTP code {}", new Object[] { userEmail,
                    organization.getName(), exception.getClass().getName() });
        }
    }

    public static String getApiUrl(String hostUrl)
    {
        return hostUrl + "/!api/1.0";
    }

}
