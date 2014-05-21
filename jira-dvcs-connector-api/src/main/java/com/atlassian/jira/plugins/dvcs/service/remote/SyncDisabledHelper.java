package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.config.FeatureManager;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Helper class to have all synchronization features in one place
 */
@Component
public class SyncDisabledHelper
{
    @VisibleForTesting
    public static final String DISABLE_SYNCHRONIZATION_FEATURE = "dvcs.connector.synchronization.disabled";
    @VisibleForTesting
    public static final String DISABLE_BITBUCKET_SYNCHRONIZATION_FEATURE = "dvcs.connector.synchronization.disabled.bitbucket";
    @VisibleForTesting
    public static final String DISABLE_GITHUB_SYNCHRONIZATION_FEATURE = "dvcs.connector.synchronization.disabled.github";
    @VisibleForTesting
    public static final String DISABLE_GITHUB_ENTERPRISE_SYNCHRONIZATION_FEATURE = "dvcs.connector.synchronization.disabled.githube";
    private final String DISABLE_FULL_SYNCHRONIZATION_FEATURE = "dvcs.connector.full-synchronization.disabled";
    private final String DISABLE_PR_SYNCHRONIZATION_FEATURE = "dvcs.connector.pr-synchronization.disabled";

    @Resource
    private FeatureManager featureManager;

    public boolean isBitbucketSyncDisabled()
    {
        return featureManager.isEnabled(DISABLE_SYNCHRONIZATION_FEATURE) || featureManager.isEnabled(DISABLE_BITBUCKET_SYNCHRONIZATION_FEATURE);
    }

    public boolean isGithubSyncDisabled()
    {
        return featureManager.isEnabled(DISABLE_SYNCHRONIZATION_FEATURE) || featureManager.isEnabled(DISABLE_GITHUB_SYNCHRONIZATION_FEATURE);
    }

    public boolean isGithubEnterpriseSyncDisabled()
    {
        return featureManager.isEnabled(DISABLE_SYNCHRONIZATION_FEATURE) || featureManager.isEnabled(DISABLE_GITHUB_ENTERPRISE_SYNCHRONIZATION_FEATURE);
    }

    public boolean isFullSychronizationDisabled()
    {
        return featureManager.isEnabled(DISABLE_FULL_SYNCHRONIZATION_FEATURE);
    }

    public boolean isPullRequestSynchronizationDisabled()
    {
        return featureManager.isEnabled(DISABLE_PR_SYNCHRONIZATION_FEATURE);
    }
}
