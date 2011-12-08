package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

import java.util.Date;

@Table("IssueMappingV2")
public interface IssueMapping extends Entity {
    int getRepositoryId();

    String getNode();

    String getIssueId();

    String getRawAuthor();

    String getAuthor();

    Date getTimestamp();

    String getRawNode();

    String getBranch();

    String getMessage();

    String getFilesData();


    void setRepositoryId(int repositoryId);

    void setNode(String node);

    void setIssueId(String issueId);

    void setRawAuthor(String rawAuthor);

    void setAuthor(String author);

    void setTimestamp(Date timestamp);

    void setRawNode(String rawNode);

    void setBranch(String branch);

    void setMessage(String message);

    void setFilesData(String files);
}
