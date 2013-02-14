package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Preload;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Preload
@Table("PR_COMMENT")
public interface RepositoryActivityPullRequestCommentMapping extends RepositoryActivityPullRequestMapping
{
    String MESSAGE = "MESSAGE";
    String PARENT_ID = "PARENT_ID";
    
    //
    // getters
    //
    @StringLength(StringLength.UNLIMITED)
    String getMessage();
    int getParentId();
    
    //
    // setters
    //
    @StringLength(StringLength.UNLIMITED)
    void setMessage(String message);
    void setParentId(int parentId);
}

