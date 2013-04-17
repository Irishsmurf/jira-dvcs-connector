package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.ActiveObjectsUtils;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.Table;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChangesetDaoImpl implements ChangesetDao
{
    private static final Logger log = LoggerFactory.getLogger(ChangesetDaoImpl.class);

    private final ActiveObjects activeObjects;
    private final ChangesetTransformer transformer = new ChangesetTransformer();

    public ChangesetDaoImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    protected List<Changeset> transform(ChangesetMapping changesetMapping)
    {
        return transformer.transform(changesetMapping);
    }

    @SuppressWarnings("unchecked")
    protected List<Changeset> transform(List<ChangesetMapping> changesetMappings)
    {
        List<Changeset> changesets = new ArrayList<Changeset>();

        for (ChangesetMapping changesetMapping : changesetMappings)
        {
            changesets.addAll(transform(changesetMapping));
        }

        return changesets;
    }

    @Override
    public void removeAllInRepository(final int repositoryId)
    {

        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {
                // todo: transaction: plugin use SalTransactionManager and there is empty implementation of TransactionSynchronisationManager.
                // todo: Therefore there are only entityCache transactions. No DB transactions.

                // delete association repo - changesets
                Query query = Query.select().where(RepositoryToChangesetMapping.REPOSITORY_ID + " = ?", repositoryId);
                log.debug("deleting repo - changesets associations from RepoToChangeset with id = [ {} ]", new String[]{String.valueOf(repositoryId)});
                ActiveObjectsUtils.delete(activeObjects, RepositoryToChangesetMapping.class, query);

                // delete association issues - changeset
                query = Query.select().where(
                        RepositoryToChangesetMapping.CHANGESET_ID + " not in  " +
                                "(select \"" + RepositoryToChangesetMapping.CHANGESET_ID + "\" from \"" + RepositoryToChangesetMapping.TABLE_NAME + "\")");
                log.debug("deleting orphaned issue-changeset associations");
                ActiveObjectsUtils.delete(activeObjects, IssueToChangesetMapping.class, query);


                // delete orphaned changesets
                query = Query.select().where(
                        "ID not in  " +
                                "(select \"" + RepositoryToChangesetMapping.CHANGESET_ID + "\" from \"" + RepositoryToChangesetMapping.TABLE_NAME + "\")");
                log.debug("deleting orphaned changesets");
                ActiveObjectsUtils.delete(activeObjects, ChangesetMapping.class, query);

                return null;
            }
        });
    }

    @Override
    public Changeset save(final Changeset changeset, final Set<String> extractedIssues)
    {
        ChangesetMapping changesetMapping = activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {

            @Override
            public ChangesetMapping doInTransaction()
            {
                // delete existing
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, 
                        ChangesetMapping.NODE + " = ? ", changeset.getNode());

                // add new
                ChangesetMapping chm = null;

                if (ArrayUtils.isEmpty(mappings))
                {
                    chm = activeObjects.create(ChangesetMapping.class);
                } else
                {
                    if (mappings.length > 1)
                    {
                        log.warn("More changesets with same Node. Same changesets count: {}, Node: {}, Repository: {}",
                                new Object[]{mappings.length, changeset.getNode(), changeset.getRepositoryId()});
                    }
                    chm = mappings[0];
                }


                // we need to remove null characters '\u0000' because PostgreSQL cannot store String values with such
                // characters
                // todo: remove NULL Chars before call setters
                chm.setNode(changeset.getNode());
                chm.setRawAuthor(changeset.getRawAuthor());
                chm.setAuthor(changeset.getAuthor());
                chm.setDate(changeset.getDate());
                chm.setRawNode(changeset.getRawNode());
                chm.setBranch(changeset.getBranch());
                chm.setMessage(changeset.getMessage());
                chm.setAuthorEmail(changeset.getAuthorEmail());
                chm.setSmartcommitAvailable(changeset.isSmartcommitAvaliable());

                JSONArray parentsJson = new JSONArray();
                for (String parent : changeset.getParents())
                {
                    parentsJson.put(parent);
                }
                
                String parentsData = parentsJson.toString();
                if (parentsData.length() > 255)
                {
                    parentsData = ChangesetMapping.TOO_MANY_PARENTS;
                }
                chm.setParentsData(parentsData);

                JSONObject filesDataJson = new JSONObject();
                JSONArray filesJson = new JSONArray();
                try
                {
                    List<ChangesetFile> files = changeset.getFiles();
                    int count = changeset.getAllFileCount();
                    filesDataJson.put("count", count);
                    for (int i = 0; i < Math.min(count, Changeset.MAX_VISIBLE_FILES); i++)
                    {
                        ChangesetFile changesetFile = files.get(i);
                        JSONObject fileJson = new JSONObject();
                        fileJson.put("filename", changesetFile.getFile());
                        fileJson.put("status", changesetFile.getFileAction().getAction());
                        fileJson.put("additions", changesetFile.getAdditions());
                        fileJson.put("deletions", changesetFile.getDeletions());

                        filesJson.put(fileJson);
                    }
                
                    filesDataJson.put("files", filesJson);
                    chm.setFilesData(filesDataJson.toString());

                } catch (JSONException e)
                {
                    log.error("Creating files JSON failed!", e);
                }

                chm.setVersion(ChangesetMapping.LATEST_VERSION);
                chm.save();

                associateRepositoryToChangeset(chm, changeset.getRepositoryId());
                if (extractedIssues != null)
                {
                    associateIssuesToChangeset(chm, extractedIssues);
                }

                return chm;
            }
        });

        changeset.setId(changesetMapping.getID());

        return changeset;
    }

    private Changeset filterByRepository(List<Changeset> changesets, int repositoryId)
    {
        for (Changeset changeset : changesets)
        {
            if (changeset.getRepositoryId() == repositoryId)
            {
                return changeset;
            }
        }

        return null;
    }

    private void associateIssuesToChangeset(ChangesetMapping changesetMapping, Set<String> extractedIssues)
    {
        // remove all assoc issues-changeset
        Query query = Query.select().where(IssueToChangesetMapping.CHANGESET_ID + " = ? ", changesetMapping);
        ActiveObjectsUtils.delete(activeObjects, IssueToChangesetMapping.class, query);

        // insert all
        for (String extractedIssue : extractedIssues)
        {
            final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
            map.put(IssueToChangesetMapping.ISSUE_KEY, extractedIssue);
            map.put(IssueToChangesetMapping.PROJECT_KEY, parseProjectKey(extractedIssue));
            map.put(IssueToChangesetMapping.CHANGESET_ID, changesetMapping.getID());
            activeObjects.create(IssueToChangesetMapping.class, map);
        }


    }

    private void associateRepositoryToChangeset(ChangesetMapping changesetMapping, int repositoryId)
    {

        RepositoryToChangesetMapping[] mappings = activeObjects.find(RepositoryToChangesetMapping.class,
                RepositoryToChangesetMapping.REPOSITORY_ID + " = ? and " +
                        RepositoryToChangesetMapping.CHANGESET_ID + " = ? ",
                repositoryId,
                changesetMapping);

        if (ArrayUtils.isEmpty(mappings))
        {
            final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();

            map.put(RepositoryToChangesetMapping.REPOSITORY_ID, repositoryId);
            map.put(RepositoryToChangesetMapping.CHANGESET_ID, changesetMapping);

            activeObjects.create(RepositoryToChangesetMapping.class, map);
        }
    }

    public static String parseProjectKey(String issueKey)
    {
        return issueKey.substring(0, issueKey.indexOf("-"));
    }

    @Override
    public Changeset getByNode(final int repositoryId, final String changesetNode)
    {
        final ChangesetMapping changesetMapping = activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping doInTransaction()
            {
                Query query = Query.select()
                        .alias(ChangesetMapping.class, "chm")
                        .alias(RepositoryToChangesetMapping.class, "rtchm")
                        .join(RepositoryToChangesetMapping.class, "chm.ID = rtchm." + RepositoryToChangesetMapping.CHANGESET_ID)
                        .where("chm." + ChangesetMapping.NODE + " = ? AND rtchm." + RepositoryToChangesetMapping.REPOSITORY_ID + " = ? ", changesetNode, repositoryId);



                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, query);
                return mappings.length != 0 ? mappings[0] : null;
            }
        });

        final List<Changeset> changesets = transform(changesetMapping);
        return changesets != null ? filterByRepository(changesets, repositoryId) : null;
    }

    @Override
    public List<Changeset> getByIssueKey(final String issueKey)
    {
        final List<ChangesetMapping> changesetMappings = activeObjects.executeInTransaction(new TransactionCallback<List<ChangesetMapping>>()
        {
            @Override
            public List<ChangesetMapping> doInTransaction()
            {
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class,
                        Query.select()
                                .alias(ChangesetMapping.class, "chm")
                                .alias(IssueToChangesetMapping.class, "itchm")
                                .join(IssueToChangesetMapping.class, "chm.ID = itchm." + IssueToChangesetMapping.CHANGESET_ID)
                                .where(IssueToChangesetMapping.ISSUE_KEY + " = ?", issueKey)
                                .order(ChangesetMapping.DATE));

                return Arrays.asList(mappings);
            }
        });

        return transform(changesetMappings);
    }

    @Override
    public List<Changeset> getLatestChangesets(final int maxResults, final GlobalFilter gf)
    {
        if (maxResults <= 0)
        {
            return Collections.emptyList();
        }
        final List<ChangesetMapping> changesetMappings = activeObjects.executeInTransaction(new TransactionCallback<List<ChangesetMapping>>()
        {
            @Override
            public List<ChangesetMapping> doInTransaction()
            {
                String baseWhereClause = new GlobalFilterQueryWhereClauseBuilder(gf).build();
                Query query = Query.select().where(baseWhereClause).limit(maxResults).order(ChangesetMapping.DATE + " DESC");
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, query);
                return Arrays.asList(mappings);
            }
        });

        return transform(changesetMappings);
    }

	@Override
	public void forEachLatestChangesetsAvailableForSmartcommitDo(final ForEachChangesetClosure closure)
	{
		Query query = createLatestChangesetsAvailableForSmartcommitQuery();
		activeObjects.stream(ChangesetMapping.class, query, new EntityStreamCallback<ChangesetMapping, Integer>() {
			@Override
			public void onRowRead(ChangesetMapping mapping)
			{
				closure.execute(mapping);
			}
		});
	}

	private Query createLatestChangesetsAvailableForSmartcommitQuery()
	{
		return Query.select("*").where(ChangesetMapping.SMARTCOMMIT_AVAILABLE + " = ? ", Boolean.TRUE)
		.order(ChangesetMapping.DATE + " DESC");
	}

    @Override
    public Set<String> findReferencedProjects(int repositoryId)
    {
        Query query = Query.select(IssueToChangesetMapping.PROJECT_KEY).distinct()
                .alias(ProjectKey.class, "pk")
                .alias(ChangesetMapping.class, "chm")
                .alias(RepositoryToChangesetMapping.class, "rtchm")
                .join(ChangesetMapping.class, "chm.ID = pk." + IssueToChangesetMapping.CHANGESET_ID)
                .join(RepositoryToChangesetMapping.class, "chm.ID = rtchm." + RepositoryToChangesetMapping.CHANGESET_ID)
                .where(RepositoryToChangesetMapping.REPOSITORY_ID + " = ?", repositoryId)
                .order(IssueToChangesetMapping.PROJECT_KEY);



        final Set<String> projectKeys = new HashSet<String>();
        activeObjects.stream(ProjectKey.class, query, new EntityStreamCallback<ProjectKey, String>()
        {
            @Override
            public void onRowRead(ProjectKey mapping)
            {
                projectKeys.add(mapping.getProjectKey());
            }
        });

        return projectKeys;
    }
    
    @Table("IssueToChangeset")
    static interface ProjectKey extends RawEntity<String> {

        @PrimaryKey(IssueToChangesetMapping.PROJECT_KEY)
        String getProjectKey();
        
        void setProjectKey();
    }

	@Override
	public void markSmartcommitAvailability(int id, boolean available)
	{
		final ChangesetMapping changesetMapping = activeObjects.get(ChangesetMapping.class, id);
		changesetMapping.setSmartcommitAvailable(available);
		activeObjects.executeInTransaction(new TransactionCallback<Void>()
		{
			@Override
			public Void doInTransaction()
			{
				changesetMapping.save();
				return null;
			}
		});
	}


}
