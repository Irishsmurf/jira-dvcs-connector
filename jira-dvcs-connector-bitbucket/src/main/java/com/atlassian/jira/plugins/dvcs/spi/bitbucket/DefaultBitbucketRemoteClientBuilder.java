package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;

public class DefaultBitbucketRemoteClientBuilder implements BitbucketClientBuilder
{
    private final AuthProvider authProvider;
    private int apiVersion = 1;
    private boolean cached;
    private int timeout = -1;



    public DefaultBitbucketRemoteClientBuilder(AuthProvider authProvider)
    {
        this.authProvider = authProvider;
    }

    @Override
    public BitbucketClientBuilder cached()
    {
        this.cached = true;
        return this;
    }

    @Override
    public BitbucketClientBuilder apiVersion(int apiVersion)
    {
        this.apiVersion = apiVersion;
        return this;
    }

    @Override
    public BitbucketClientBuilder timeout(int timeout)
    {
        this.timeout = timeout;
        return this;
    }

    @Override
    public BitbucketRemoteClient build()
    {
        authProvider.setApiVersion(apiVersion);

        authProvider.setCached(cached);

        authProvider.setTimeout(timeout);

        return new BitbucketRemoteClient(authProvider);
    }
}
