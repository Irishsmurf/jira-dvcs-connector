package com.atlassian.jira.plugins.bitbucket.api;

import java.util.Date;
import java.util.List;


/**
 * Details on a changeset
 */
public interface Changeset {

    public int getRepositoryId();

    public String getNode();

    public String getRawAuthor();

    public String getAuthor();

    public Date getTimestamp();

    public String getRawNode();

    public String getBranch();

    public String getMessage();

    public List<String> getParents();

    public List<ChangesetFile> getFiles();

    public String getCommitURL(SourceControlRepository repository);
}
