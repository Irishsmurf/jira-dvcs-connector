package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketAuthentication;
import com.atlassian.sal.api.net.Request;
import org.apache.commons.lang.StringUtils;

/**
 * Basic authentication
 */
public class BasicAuthentication extends BitbucketAuthentication
{
    private final String username;
    private final String password;

    public BasicAuthentication(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    public void addAuthentication(Request<?, ?> request)
    {
        // add basic authentication
        if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password))
            request.addBasicAuthentication(username, password);
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicAuthentication that = (BasicAuthentication) o;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }
}
