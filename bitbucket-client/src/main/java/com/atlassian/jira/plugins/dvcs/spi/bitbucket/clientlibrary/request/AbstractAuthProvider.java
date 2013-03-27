package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

/**
 * 
 * AbstractAuthProvider
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 17:00:59
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public abstract class AbstractAuthProvider implements AuthProvider
{
	private final String hostUrl;
	private int apiVersion = 1;
    private String userAgent;

    public AbstractAuthProvider(String hostUrl)
	{
		this.hostUrl = hostUrl;
	}

    @Override
	public String getApiUrl()
	{
		return hostUrl.replaceAll("/$", "") + "/api/"  + apiVersion + ".0";
	}

    @Override
    public void setApiVersion(int apiVersion)
    {
        this.apiVersion = apiVersion;
    }

    @Override
    public String getHostUrl()
    {
        return hostUrl;
    }

    @Override
    public void setUserAgent(String userAgent)
    {
        this.userAgent = userAgent;
    }

    @Override
    public String getUserAgent()
    {
        return userAgent;
    }
}
