package com.atlassian.jira.plugins.bitbucket.bitbucket;

import org.junit.Test;

import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link RepositoryUri}
 */
public class TestRepositoryUri
{
    @Test
    public void testParseRepositoryFullUrlWithBranch() {
        RepositoryUri repositoryUri = RepositoryUri.parse("http://bitbucket.org/owner/slug/default");
        assertEquals("owner", repositoryUri.getOwner());
        assertEquals("slug", repositoryUri.getSlug());
    }

    @Test
    public void testParseRepositoryFullUrl() {
        RepositoryUri repositoryUri = RepositoryUri.parse("http://bitbucket.org/owner/slug");
        assertEquals("owner", repositoryUri.getOwner());
        assertEquals("slug", repositoryUri.getSlug());
    }

}
