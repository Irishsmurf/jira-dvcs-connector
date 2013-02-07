package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubRepositoryDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * AO implementation of the {@link GitHubRepositoryDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubRepositoryDAOImpl implements GitHubRepositoryDAO
{

    /**
     * @see #GitHubRepositoryDAOImpl(ActiveObjects)
     */
    private final ActiveObjects activeObjects;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     */
    public GitHubRepositoryDAOImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final GitHubRepository gitHubRepository)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                if (gitHubRepository.getId() == 0)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    map(params, gitHubRepository);
                    GitHubRepositoryMapping created = activeObjects.create(GitHubRepositoryMapping.class, params);
                    map(gitHubRepository, created);

                } else
                {
                    GitHubRepositoryMapping toSave = activeObjects.get(GitHubRepositoryMapping.class, gitHubRepository.getId());
                    map(toSave, gitHubRepository);
                    toSave.save();
                    map(gitHubRepository, toSave);

                }

                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final GitHubRepository gitHubRepository)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubRepositoryMapping toDelete = activeObjects.get(GitHubRepositoryMapping.class, gitHubRepository.getId());
                activeObjects.delete(toDelete);
                gitHubRepository.setId(0);

                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubRepository getByGitHubId(long gitHubId)
    {
        Query query = Query.select().where(GitHubRepositoryMapping.COLUMN_GIT_HUB_ID + " = ? ", gitHubId);
        GitHubRepositoryMapping[] founded = activeObjects.find(GitHubRepositoryMapping.class, query);

        if (founded.length == 0)
        {
            return null;

        } else if (founded.length == 1)
        {
            GitHubRepository result = new GitHubRepository();
            map(result, founded[0]);
            return result;

        } else
        {
            throw new IllegalStateException("GitHub ID conflict on repositories! GitHub ID: " + gitHubId + " Founded: " + founded.length
                    + " records.");

        }
    }

    /**
     * Re-maps the model value into the AO creation map.
     * 
     * @param target
     *            AO creation map
     * @param source
     *            model value
     */
    private void map(Map<String, Object> target, GitHubRepository source)
    {
        target.put(GitHubRepositoryMapping.COLUMN_GIT_HUB_ID, source.getGitHubId());
        target.put(GitHubRepositoryMapping.COLUMN_NAME, source.getName());
    }

    /**
     * Re-maps the model value into the AO value.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void map(GitHubRepositoryMapping target, GitHubRepository source)
    {
        target.setGitHubId(source.getGitHubId());
        target.setName(source.getName());
    }

    /**
     * Re-maps the AO value into the model value.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    static void map(GitHubRepository target, GitHubRepositoryMapping source)
    {
        target.setId(source.getID());
        target.setGitHubId(source.getGitHubId());
        target.setName(source.getName());
    }

}
