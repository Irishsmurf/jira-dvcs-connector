package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFileAction;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultBitbucketChangesetFile;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class BitbucketChangesetFileFactory
{
    /**
     * Parse the json object as a {@link ChangesetFile file} within a changeset.
     * @param json the json object describing the file
     * @return the parsed {@link ChangesetFile}
     */
    public static ChangesetFile parse(JSONObject json)
    {
        try
        {
            return new DefaultBitbucketChangesetFile(
                    ChangesetFileAction.valueOf(json.getString("type").toUpperCase()),
                    // TODO: additions, deletions
                    json.getString("file"), 0, 0);
        }
        catch (JSONException e)
        {
            throw new SourceControlException("invalid json object", e);
        }
    }

    private BitbucketChangesetFileFactory()
    {
    }
}
