package com.atlassian.jira.plugins.bitbucket.api.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFileAction;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple mapper that uses ActiveObjects to store the mapping details
 */
public class DefaultRepositoryPersister implements RepositoryPersister
{
    private final Logger logger = LoggerFactory.getLogger(DefaultRepositoryPersister.class);

    private final ActiveObjects activeObjects;

    public DefaultRepositoryPersister(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    @Override
    public List<ProjectMapping> getRepositories(final String projectKey, final String repositoryType)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<ProjectMapping>>()
        {
			@Override
            public List<ProjectMapping> doInTransaction()
            {
                ProjectMapping[] mappings = activeObjects.find(ProjectMapping.class, "PROJECT_KEY = ? AND REPOSITORY_TYPE = ?", projectKey, repositoryType);
                return Lists.newArrayList(mappings);
            }
        });
    }

    @Override
    public ProjectMapping addRepository(String repositoryType, String projectKey, String repositoryUrl, String username, String password, String adminUsername, String adminPassword, String accessToken)
    {
        
        final ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "REPOSITORY_URL = ? and PROJECT_KEY = ?", repositoryUrl, projectKey);
        if (projectMappings.length>0)
        {
            throw new SourceControlException("Repository ["+repositoryUrl+"] is already linked to project ["+projectKey+"]");
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("REPOSITORY_URL", repositoryUrl);
        map.put("PROJECT_KEY", projectKey);
        map.put("REPOSITORY_TYPE", repositoryType);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
        {
            map.put("USERNAME", username);
            map.put("PASSWORD", password);
        }
        if (StringUtils.isNotBlank(adminUsername) && StringUtils.isNotBlank(adminPassword))
        {
        	map.put("ADMIN_USERNAME", adminUsername);
        	map.put("ADMIN_PASSWORD", adminPassword);
        }
        if (StringUtils.isNotBlank(accessToken))
        {
            map.put("ACCESS_TOKEN", accessToken);
        }
        return activeObjects.executeInTransaction(new TransactionCallback<ProjectMapping>()
        {
            @Override
            public ProjectMapping doInTransaction()
            {
        		return activeObjects.create(ProjectMapping.class, map);
            }
        });
    }

    @Override
    public void removeRepository(final int id)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {
                final ProjectMapping projectMapping = activeObjects.get(ProjectMapping.class, id);
                final IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class, "REPOSITORY_ID = ?", id);

                logger.debug("deleting project mapping [ {} ]", String.valueOf(id));
                logger.debug("deleting [ {} ] issue mappings [ {} ]", new String[]{String.valueOf(issueMappings.length), String.valueOf(id)});

                activeObjects.delete(projectMapping);
                activeObjects.delete(issueMappings);
                return null;
            }
        });
    }

    @Override

    public List<IssueMapping> getIssueMappings(final String issueId, final String repositoryType)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<IssueMapping>>()
        {
            @SuppressWarnings("unchecked")
            @Override
            public List<IssueMapping> doInTransaction()
            {
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, "ISSUE_ID = ?", issueId);
                if (mappings.length==0)
                    return Collections.EMPTY_LIST;
                
                // get list of all project mappings with our type
                ProjectMapping[] myProjectMappings = activeObjects.find(ProjectMapping.class, "REPOSITORY_TYPE = ?", repositoryType);
                
                final Set<Integer> projectMappingsIds = Sets.newHashSet();
                for (int i = 0; i < myProjectMappings.length; i++)
                {
                    projectMappingsIds.add(myProjectMappings[i].getID());
                }
                
                return Lists.newArrayList(CollectionUtils.select(Arrays.asList(mappings), new Predicate()
                {
                    @Override
                    public boolean evaluate(Object o)
                    {
                        IssueMapping issueMapping = (IssueMapping) o;
                        return projectMappingsIds.contains(issueMapping.getRepositoryId());
                    }
                }));
            }
        });
    }
    
    @Override
    public void addChangeset(final String issueId, final Changeset changeset)
    {
        final int repositoryId = changeset.getRepositoryId();
        final String node = changeset.getNode();

        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {
                logger.debug("create issue mapping [ {} ] [ {} - {} ] ", new String[]{issueId, String.valueOf(repositoryId), node});
                // delete existing
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, "ISSUE_ID = ? and NODE = ?", issueId, node);
                if (ArrayUtils.isNotEmpty(mappings))
				{
					activeObjects.delete(mappings);
				}
                // add new
				Map<String, Object> map = Maps.newHashMap();
				map.put("REPOSITORY_ID", repositoryId);
				map.put("ISSUE_ID", issueId);
				map.put("NODE", node);
                map.put("RAW_AUTHOR", changeset.getRawAuthor());
                map.put("AUTHOR", changeset.getAuthor());
                map.put("TIMESTAMP", changeset.getTimestamp());
                map.put("RAW_NODE", changeset.getRawNode());
                map.put("BRANCH", changeset.getBranch());
                map.put("MESSAGE", changeset.getMessage());

                JSONObject filesJson = new JSONObject();
                JSONArray added = new JSONArray();
                JSONArray removed = new JSONArray();
                JSONArray modified = new JSONArray();
                try
                {
                    List<ChangesetFile> files = changeset.getFiles();
                    int count = files.size();
                    filesJson.accumulate("count", count);
                    for (int i=0; i< Math.min(count, DvcsRepositoryManager.MAX_VISIBLE_FILES); i++)
                    {
                        ChangesetFile changesetFile = files.get(i);
                        if (changesetFile.getFileAction().equals(ChangesetFileAction.ADDED))
                        {
                            added.put(changesetFile.getFile());
                        } else if (changesetFile.getFileAction().equals(ChangesetFileAction.REMOVED))
                        {
                            removed.put(changesetFile.getFile());
                        } else if (changesetFile.getFileAction().equals(ChangesetFileAction.MODIFIED))
                        {
                            modified.put(changesetFile.getFile());
                        }

                    }
                    filesJson.put("added", added);
                    filesJson.put("removed", removed);
                    filesJson.put("modified", modified);

                    map.put("FILES_DATA", filesJson.toString());
                } catch (JSONException e)
                {
                    logger.error("Creating files JSON failed!", e);
                }


                return activeObjects.create(IssueMapping.class, map);
            }
        });
    }
    @Override
    public List<IssueMapping> getLastChangesetMappings(final int count, final Set<String> inProjects, final Set<String> notInProjects)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<IssueMapping>>()
        {
            @Override
            public List<IssueMapping> doInTransaction()
            {
                StringBuilder whereClauseSb = new StringBuilder();
                if(!CollectionUtils.isEmpty(inProjects)){
                    for(String projectKey : inProjects){
                        if(whereClauseSb.length() != 0){
                            whereClauseSb.append(" OR ");
                        }
                        whereClauseSb.append("ISSUE_ID like '").append(projectKey).append("-%' ");
                    }
                }
                if(!CollectionUtils.isEmpty(notInProjects)){
                    for(String projectKey : notInProjects){
                        if(whereClauseSb.length() != 0){
                            whereClauseSb.append(" AND ");
                        }
                        whereClauseSb.append("ISSUE_ID not like '").append(projectKey).append("-%' ");
                    }
                }
                if(whereClauseSb.length() == 0){
                    whereClauseSb.append(" true ");
                }
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, Query.select().where(whereClauseSb.toString()).limit(count).order("TIMESTAMP DESC"));
                return mappings == null ? new ArrayList<IssueMapping>() : Lists.newArrayList(mappings);
            }
        });
    }
    
	@Override
    public ProjectMapping getRepository(final int id)
	{
		return activeObjects.executeInTransaction(new TransactionCallback<ProjectMapping>()
		{
			@Override
            public ProjectMapping doInTransaction()
			{
				return activeObjects.get(ProjectMapping.class, id);
			}
		});
	}

    @Override
    public IssueMapping getIssueMapping(final String node)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<IssueMapping>()
        {
            @Override
            public IssueMapping doInTransaction()
            {
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, "NODE = ?", node);
                return mappings.length != 0 ? mappings[0] : null;
            }
        });
    }
}
