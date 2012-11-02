package com.atlassian.jira.plugins.dvcs.upgrade;

import java.util.Collection;

import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.google.common.collect.Lists;

/**
 * For Bitbucket.
 * 
 * Disabled - As {@link To_02_ProjectBasedRepositoryLinksUpgradeTask} will do the job.
 * Will be deleted later.
 */
@Deprecated
public class RepositoryLinksUpgradeTask implements PluginUpgradeTask
{

	public RepositoryLinksUpgradeTask()
	{
		super();
	}

	// -------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------
	// Upgrade
	// -------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------

	@Override
	public Collection<Message> doUpgrade() throws Exception
	{
		return Lists.newLinkedList();
	}


	// -------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------

	@Override
	public int getBuildNumber()
	{
		return 1;
	}

	@Override
	public String getShortDescription()
	{
		return "[DISABLED] Upgrades the repository links at Bitbucket with custom handlers (regexp).";
	}

	@Override
	public String getPluginKey()
	{
		return "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin";
	}

}
