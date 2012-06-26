package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("RepositoryMapping")
public interface RepositoryMapping extends Entity
{
    public static final String ORGANIZATION_ID = "ORGANIZATION_ID";
    public static final String SLUG = "SLUG";
    public static final String NAME = "NAME";
    public static final String LAST_COMMIT_DATE = "LAST_COMMIT_DATE";
    public static final String LINKED = "LINKED";
    public static final String DELETED = "DELETED";
    public static final String SMARTCOMMITS_ENABLED = "SMARTCOMMITS_ENABLED";

    int getOrganizationId();
    String getSlug();
    String getName();
    Date getLastCommitDate();
    boolean isLinked();
    boolean isDeleted();
    boolean isSmartcommitsEnabled();

    void setOrganizationId(int organizationId);
    void setSlug(String slug);
    void setName(String name);
    void setLastCommitDate(Date lastCommitDate);
    void setLinked(boolean linked);
    void setDeleted(boolean deleted);
    void setSmartcommitsEnabled(boolean enabled);
}
