package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.AccountRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ChangesetRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.GroupRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.SSHRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ServiceRemoteRestpoint;

/**
 * 
 * <h3>Example of use</h3>
 * <pre>
 * RepositoryRemoteRestpoint repositoriesRest = 
 * 				new BitbucketRemoteClient( new TwoLeggedOauthProvider("https://www.bitbucket.org", "coolkey9b9...", "coolsecret040oerre....") ).getRepositoriesRest();
 *
 * List&lt;BitbucketRepository&gt; repositories = repositoriesRest.getAllRepositories("teamname");
 *		
 * <pre>
 *
 * 
 * <br /><br />
 * Created on 12.7.2012, 17:04:43
 * <br /><br />
 * 
 * @see AuthProvider
 * 
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketRemoteClient
{
    public static final String BITBUCKET_URL = "https://bitbucket.org";
    
    private final AccountRemoteRestpoint accountRemoteRestpoint;
    private final ChangesetRemoteRestpoint changesetRemoteRestpoint;
    private final GroupRemoteRestpoint groupRemoteRestpoint;
    private final RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint;
    private final RepositoryRemoteRestpoint repositoryRemoteRestpoint;
    private final SSHRemoteRestpoint sshRemoteRestpoint;
    private final ServiceRemoteRestpoint serviceRemoteRestpoint;

    private final RemoteRequestor requestor;
    
	
	public BitbucketRemoteClient(AuthProvider provider)
	{
        requestor = provider.provideRequestor();

        this.accountRemoteRestpoint = new AccountRemoteRestpoint(requestor);
        this.changesetRemoteRestpoint = new ChangesetRemoteRestpoint(requestor);
        this.groupRemoteRestpoint = new GroupRemoteRestpoint(requestor);
        this.repositoryLinkRemoteRestpoint = new RepositoryLinkRemoteRestpoint(requestor);
        this.repositoryRemoteRestpoint = new RepositoryRemoteRestpoint(requestor);
        this.sshRemoteRestpoint = new SSHRemoteRestpoint(requestor);
        this.serviceRemoteRestpoint = new ServiceRemoteRestpoint(requestor);
	}
	
    public AccountRemoteRestpoint getAccountRest()
    {
        return accountRemoteRestpoint;
    }
    
	public ChangesetRemoteRestpoint getChangesetsRest()
    {
		return changesetRemoteRestpoint;
	}
    
    public GroupRemoteRestpoint getGroupsRest()
    {
		return groupRemoteRestpoint;
	}
    
	public RepositoryLinkRemoteRestpoint getRepositoryLinksRest()
    {
		return repositoryLinkRemoteRestpoint;
	}
	
	public RepositoryRemoteRestpoint getRepositoriesRest()
    {
		return repositoryRemoteRestpoint;
	}
	
    public SSHRemoteRestpoint getSSHRest()
    {
        return sshRemoteRestpoint;
    }
    
	public ServiceRemoteRestpoint getServicesRest()
    {
		return serviceRemoteRestpoint;
	}

    public RemoteRequestor getRequestor()
    {
        return requestor;
    }
}
