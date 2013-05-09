package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("IssueToChangeset")
public interface IssueToChangesetMapping extends Entity
{

    public static final String TABLE_NAME = "AO_E8B6CC_ISSUE_TO_CHANGESET";
    public static final String CHANGESET_ID = "CHANGESET_ID";
    public static final String ISSUE_KEY = "ISSUE_KEY";
    public static final String PROJECT_KEY = "PROJECT_KEY";


    ChangesetMapping getChangeset();
    String getIssueKey();
    String getProjectKey();

    void setChangeset(ChangesetMapping changeset);
    void setIssueKey(String issueKey);
    void setProjectKey(String projectKey);

}
