package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.SyncProgress;

/**
 * Returning type {@link Repository} is enriched with synchronization status by default.
 *
 *
 * @see SyncProgress
 *
 */
public interface RepositoryService
{

    /**
     * returns all repositories for given organization
     * @param organizationId organizationId
     * @return repositories
     */
    List<Repository> getAllByOrganization(int organizationId);
    
    /**
     * Gets the all active repositories with synchronization status.
     *
     * @param organizationId the organization id
     * @return the all active repositories
     */
    List<Repository> getAllActiveRepositories();

    /**
     * returns repository by ID
     * @param repositoryId repositoryId
     * @return repository
     */
    Repository get(int repositoryId);

    /**
     * save Repository to storage. If it's new object (without ID) after this operation it will have it assigned.
     * @param repository Repository
     * @return Repository
     */
    Repository save(Repository repository);

    /**
     * synchronization of repository list in given organization
     * @param organizationId organizationId
     */
    void syncRepositoryList(int organizationId);

    /**
     * synchronization of changesets in given repository
     * @param repositoryId repositoryId
     */
    void sync(int repositoryId);

}
