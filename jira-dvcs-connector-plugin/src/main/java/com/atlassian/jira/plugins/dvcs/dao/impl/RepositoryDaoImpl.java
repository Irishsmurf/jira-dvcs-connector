package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.sal.api.transaction.TransactionCallback;

public class RepositoryDaoImpl implements RepositoryDao
{

    private static final Logger log = LoggerFactory.getLogger(RepositoryDaoImpl.class);

    private final ActiveObjects activeObjects;
    private final Synchronizer synchronizer;

    public RepositoryDaoImpl(ActiveObjects activeObjects, Synchronizer synchronizer)
    {
        this.activeObjects = activeObjects;
        this.synchronizer = synchronizer;
    }

    protected Repository transform(RepositoryMapping repositoryMapping)
    {
        if (repositoryMapping == null)
        {
            return null;
        }

        OrganizationMapping organizationMapping = activeObjects.get(OrganizationMapping.class, repositoryMapping.getOrganizationId());
        log.debug("Repository transformation: [{}] ", repositoryMapping);

        Repository repository = new Repository(repositoryMapping.getID(), repositoryMapping.getOrganizationId(), null,
                repositoryMapping.getSlug(), repositoryMapping.getName(), repositoryMapping.getLastCommitDate(),
                repositoryMapping.isLinked(), repositoryMapping.isDeleted(), null);
        repository.setSmartcommitsEnabled(repositoryMapping.isSmartcommitsEnabled());
        repository.setActivityLastSync(repositoryMapping.getActivityLastSync());

        Date lastDate = repositoryMapping.getLastCommitDate();

        if (lastDate == null || (repositoryMapping.getActivityLastSync() != null && repositoryMapping.getActivityLastSync().after(lastDate)))
        {
            lastDate = repositoryMapping.getActivityLastSync();
        }
        repository.setLastActivityDate(lastDate);
        repository.setLogo(repositoryMapping.getLogo());
        // set sync progress
        repository.setSync((DefaultProgress) synchronizer.getProgress(repository.getId()));

        if (organizationMapping != null)
        {
            Credential credential = new Credential(organizationMapping.getOauthKey(), organizationMapping.getOauthSecret(),
                    organizationMapping.getAccessToken(), organizationMapping.getAdminUsername(), organizationMapping.getAdminPassword());
            repository.setCredential(credential);
            repository.setDvcsType(organizationMapping.getDvcsType());
            repository.setOrgHostUrl(organizationMapping.getHostUrl());
            repository.setOrgName(organizationMapping.getName());
            repository.setRepositoryUrl(createRepositoryUrl(repositoryMapping, organizationMapping));
        } else
        {
            repository.setOrgHostUrl(null);
            repository.setOrgName(null);
            repository.setRepositoryUrl(null);
        }

        return repository;
    }

    private String createRepositoryUrl(RepositoryMapping repositoryMapping, OrganizationMapping organizationMapping)
    {
        String hostUrl = organizationMapping.getHostUrl();
        // normalize
        if (hostUrl != null && hostUrl.endsWith("/"))
        {
            hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
        }
        return hostUrl + "/" + organizationMapping.getName() + "/" + repositoryMapping.getSlug();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Repository> getAllByOrganization(final int organizationId, final boolean includeDeleted)
    {
        List<RepositoryMapping> repositoryMappings = activeObjects.executeInTransaction(new TransactionCallback<List<RepositoryMapping>>()
        {
            @Override
            public List<RepositoryMapping> doInTransaction()
            {
                Query query = Query.select();
                if (includeDeleted)
                {
                    query.where(RepositoryMapping.ORGANIZATION_ID + " = ? ", organizationId);
                } else
                {
                    query.where(RepositoryMapping.ORGANIZATION_ID + " = ? AND " + RepositoryMapping.DELETED + " = ? ", organizationId,
                            Boolean.FALSE);
                }
                query.order(RepositoryMapping.NAME);

                final RepositoryMapping[] rms = activeObjects.find(RepositoryMapping.class, query);
                return Arrays.asList(rms);
            }
        });

        return (List<Repository>) CollectionUtils.collect(repositoryMappings, new Transformer()
        {

            @Override
            public Object transform(Object input)
            {
                RepositoryMapping repositoryMapping = (RepositoryMapping) input;

                return RepositoryDaoImpl.this.transform(repositoryMapping);
            }
        });
    }

    @Override
    public List<Repository> getAll(final boolean includeDeleted)
    {

        List<RepositoryMapping> repositoryMappings = activeObjects.executeInTransaction(new TransactionCallback<List<RepositoryMapping>>()
        {
            @Override
            public List<RepositoryMapping> doInTransaction()
            {
                Query select = Query.select();
                if (!includeDeleted)
                {
                    select = select.where(RepositoryMapping.DELETED + " = ? ", Boolean.FALSE);
                }
                select.order(RepositoryMapping.NAME);

                final RepositoryMapping[] repos = activeObjects.find(RepositoryMapping.class, select);
                return Arrays.asList(repos);
            }
        });

        final Collection<Repository> repositories = transformRepositories(repositoryMappings);

        return new ArrayList<Repository>(repositories);

    }

    @Override
    public boolean existsLinkedRepositories(final boolean includeDeleted)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<Boolean>()
        {
            @Override
            public Boolean doInTransaction()
            {
                Query query = Query.select();
                if (includeDeleted)
                {
                    query.where(RepositoryMapping.LINKED + " = ?", Boolean.TRUE);
                } else
                {
                    query.where(RepositoryMapping.LINKED + " = ? AND " + RepositoryMapping.DELETED + " = ? ", Boolean.TRUE, Boolean.FALSE);
                }

                return activeObjects.count(RepositoryMapping.class, query) > 0;
            }
        });
    }

    /**
     * Transform repositories.
     *
     * @param repositoriesToReturn
     *            the repositories to return
     * @return the collection< repository>
     */
    @SuppressWarnings("unchecked")
    private Collection<Repository> transformRepositories(final List<RepositoryMapping> repositoriesToReturn)
    {
        return CollectionUtils.collect(repositoriesToReturn, new Transformer()
        {

            @Override
            public Object transform(Object input)
            {
                RepositoryMapping repositoryMapping = (RepositoryMapping) input;
                return RepositoryDaoImpl.this.transform(repositoryMapping);
            }
        });
    }

    @Override
    public Repository get(final int repositoryId)
    {
        RepositoryMapping repositoryMapping = activeObjects.executeInTransaction(new TransactionCallback<RepositoryMapping>()
        {
            @Override
            public RepositoryMapping doInTransaction()
            {
                return activeObjects.get(RepositoryMapping.class, repositoryId);
            }
        });

        if (repositoryMapping == null)
        {
            log.warn("Repository with id {} was not found.", repositoryId);
            return null;
        } else
        {
            return transform(repositoryMapping);

        }
    }

    @Override
    public Repository save(final Repository repository)
    {
        final RepositoryMapping repositoryMapping = activeObjects.executeInTransaction(new TransactionCallback<RepositoryMapping>()
        {

            @Override
            public RepositoryMapping doInTransaction()
            {
                RepositoryMapping rm;
                if (repository.getId() == 0)
                {
                    // we need to remove null characters '\u0000' because PostgreSQL cannot store String values
                    // with such characters
                    final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
                    map.put(RepositoryMapping.ORGANIZATION_ID, repository.getOrganizationId());
                    map.put(RepositoryMapping.SLUG, repository.getSlug());
                    map.put(RepositoryMapping.NAME, repository.getName());
                    map.put(RepositoryMapping.LAST_COMMIT_DATE, repository.getLastCommitDate());
                    map.put(RepositoryMapping.LINKED, repository.isLinked());
                    map.put(RepositoryMapping.DELETED, repository.isDeleted());
                    map.put(RepositoryMapping.SMARTCOMMITS_ENABLED, repository.isSmartcommitsEnabled());
                    map.put(RepositoryMapping.ACTIVITY_LAST_SYNC, repository.getActivityLastSync());
                    map.put(RepositoryMapping.LOGO, repository.getLogo());

                    rm = activeObjects.create(RepositoryMapping.class, map);
                    rm = activeObjects.find(RepositoryMapping.class, "ID = ?", rm.getID())[0];
                } else
                {
                    rm = activeObjects.get(RepositoryMapping.class, repository.getId());

                    rm.setSlug(repository.getSlug());
                    rm.setName(repository.getName());
                    rm.setLastCommitDate(repository.getLastCommitDate());
                    rm.setLinked(repository.isLinked());
                    rm.setDeleted(repository.isDeleted());
                    rm.setSmartcommitsEnabled(repository.isSmartcommitsEnabled());
                    rm.setActivityLastSync(repository.getActivityLastSync());
                    rm.setLogo(repository.getLogo());

                    rm.save();
                }
                return rm;
            }
        });

        return transform(repositoryMapping);
    }

    @Override
    public void remove(int repositoryId)
    {
        activeObjects.delete(activeObjects.get(RepositoryMapping.class, repositoryId));
    }

    private OrganizationMapping getOrganizationMapping(final int organizationId)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<OrganizationMapping>()
        {
            @Override
            public OrganizationMapping doInTransaction()
            {
                return activeObjects.get(OrganizationMapping.class, organizationId);
            }
        });
    }

    @Override
    public void setLastActivitySyncDate(final Integer repositoryId, final Date date)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            public Void doInTransaction()
            {
                RepositoryMapping repo = activeObjects.get(RepositoryMapping.class, repositoryId);
                repo.setActivityLastSync(date);
                repo.save();
                return null;
            }
        });
    }
}
