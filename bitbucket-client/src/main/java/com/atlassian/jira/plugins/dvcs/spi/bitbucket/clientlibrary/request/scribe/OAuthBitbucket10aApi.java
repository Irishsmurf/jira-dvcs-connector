package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

/**
 * OAuthBitbucket10aApi
 * 
 * <br />
 * <br />
 * Created on 14.6.2012, 13:40:17 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
public class OAuthBitbucket10aApi extends DefaultApi10a
{

    private final String apiUrl;
    private final boolean isTwoLegged;

    /**
     * The Constructor.
     * 
     * @param apiUrl
     *            the api url
     * @param isTwoLegged
     *            <code>true</code> if we should use 2LO mechanism,
     *            <code>false</code> for 3LO
     */
    public OAuthBitbucket10aApi(String apiUrl, boolean isTwoLegged)
    {
        this.apiUrl = apiUrl;
        this.isTwoLegged = isTwoLegged;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestTokenEndpoint()
    {
        return apiUrl + "/oauth/request_token/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccessTokenEndpoint()
    {
        return apiUrl + "/oauth/access_token/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthorizationUrl(Token requestToken)
    {
        return String.format(apiUrl + "/oauth/authenticate/?oauth_token=%s", requestToken.getToken());
    }

    @Override
    public OAuthService createService(OAuthConfig config)
    {
        if (isTwoLegged)
        {
            return new TwoLoOAuth10aServiceImpl(this, config);
        } else
        {
            return super.createService(config);
        }

    }

}