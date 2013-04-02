package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.dao.impl.ChangesetDaoImpl;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.util.IssueKeyExtractor;

public class DefaultSynchronisationOperation implements SynchronisationOperation
{
    private static final Logger log = LoggerFactory.getLogger(DefaultSynchronisationOperation.class);

    private final Repository repository;
    private final RepositoryService repositoryService;
    private final DefaultProgress progress;
    private final ChangesetService changesetService;

    private final DvcsCommunicator communicator;

    private final RepositoryActivitySynchronizer activitySynchronizer;

    private final EnumSet<SynchronizationFlag> synchronizationFlags;

    public DefaultSynchronisationOperation(DvcsCommunicator communicator, Repository repository,
            RepositoryService repositoryService, ChangesetService changesetService,
            RepositoryActivitySynchronizer activitySynchronizer, EnumSet<SynchronizationFlag> synchronizationFlags)
    {
        this.communicator = communicator;
        this.repository = repository;
        this.repositoryService = repositoryService;
        this.changesetService = changesetService;
        this.progress = new DefaultProgress();
        this.activitySynchronizer = activitySynchronizer;
        this.synchronizationFlags = synchronizationFlags;
    }

    @Override
    public void synchronise()
    {
    	log.debug("Operation going to sync repo " + repository.getSlug() + " softs sync = " + isSoftSync() );
    	
    	if (!isSoftSync()) 
        {
            // we are doing full sync, lets delete all existing changesets
            // also required as GHCommunicator.getChangesets() returns only changesets not already stored in database
            changesetService.removeAllInRepository(repository.getId());
            repository.setLastCommitDate(null);
            repositoryService.save(repository);
        }

    	if (synchronizationFlags.contains(SynchronizationFlag.SYNC_CHANGESETS)) {
    	    syncChangesets();
    	}
    	
    	if (synchronizationFlags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS)) {
    	    syncActivity();
    	}
    }

    private void syncChangesets()
    {
        int changesetCount = 0;
        int jiraCount = 0;

        Iterable<Changeset> allOrLatestChangesets = changesetService.getChangesetsFromDvcs(repository);

        Set<String> foundProjectKeys = new HashSet<String>();

        for (Changeset changeset : allOrLatestChangesets)
        {
        	if (progress.isShouldStop())
        	{
        		return;
        	}
        	
        	if (changeset == null)
        	{
        	    continue;
        	}
        	
            if (repository.getLastCommitDate() == null || repository.getLastCommitDate().before(changeset.getDate()))
            {
                repository.setLastCommitDate(changeset.getDate());
                repositoryService.save(repository);
            }

            changesetCount++;
            String message = changeset.getMessage();
            log.debug("syncing changeset [{}] [{}]", changeset.getNode(), changeset.getMessage());

            Set<String> extractedIssues = extractIssueKeys(message);

            if (CollectionUtils.isEmpty(extractedIssues) ) // storing only issues without issueKeys as
            {
                changeset.setIssueKey("NON_EXISTING-0");
                changesetService.save(changeset);
                progress.inProgress(changesetCount, jiraCount, 0);
                continue;
            }

            // get detail changeset because in this response is not information about files
            Changeset detailChangeset = null;
            
            if (CollectionUtils.isNotEmpty(extractedIssues) ) 
            {
            	try
                {
                    detailChangeset = changesetService.getDetailChangesetFromDvcs(repository, changeset);
                } catch (Exception e)
                {
                    log.warn("Unable to retrieve details for changeset " + changeset.getNode(), e);
                }
            	
                boolean changesetAlreadyMarkedForSmartCommits = false;
                for (String extractedIssue : extractedIssues)
                {
                    jiraCount++;
                    String issueKey = extractedIssue.toUpperCase();
                    try
                    {
                    	Changeset changesetForSave = detailChangeset == null ? changeset : detailChangeset;
                    	changesetForSave.setIssueKey(issueKey);
                        //--------------------------------------------
                        // mark smart commit can be processed
                        // + store extracted project key for incremental linking
                        if (isSoftSync() && !changesetAlreadyMarkedForSmartCommits)
                        {
                            markChangesetForSmartCommit(changesetForSave, true);
                            changesetAlreadyMarkedForSmartCommits = true;
                        } else
                        {
                            markChangesetForSmartCommit(changesetForSave, false);
                        }
                        
                        foundProjectKeys.add(ChangesetDaoImpl.parseProjectKey(issueKey));
                        //--------------------------------------------
                        log.debug("Save changeset [{}]", changesetForSave);
                        changesetService.save(changesetForSave);
                    
                    } catch (SourceControlException e)
                    {
                        log.error("Error adding changeset " + changeset, e);
                    }
                }
            }
            progress.inProgress(changesetCount, jiraCount, 0);
        }
        
        // linkers
        setupNewLinkers(foundProjectKeys);
    }
    
    private void setupNewLinkers(Set<String> extractedProjectKeys)
    {
        if (!extractedProjectKeys.isEmpty())
        {
            if (synchronizationFlags.contains(isSoftSync()))
            {
                communicator.linkRepositoryIncremental(repository, extractedProjectKeys);
            } else
            {
                communicator.linkRepository(repository, extractedProjectKeys);
            }
        }
    }

    private void markChangesetForSmartCommit(Changeset changesetForSave, boolean mark)
	{
        if (repository.isSmartcommitsEnabled())
        {
            log.debug("Marking changeset node = {} to be processed by smart commits", changesetForSave.getNode());
            changesetForSave.setSmartcommitAvaliable(mark);
        } else {
        	log.debug("Changeset node = {}. Repository not enabled for smartcommits.", changesetForSave.getNode());
        }
	}

	private Set<String> extractIssueKeys(String message)
    {
	    // TODO check if these issues actually exists...
        return IssueKeyExtractor.extractIssueKeys(message);
    }

    @Override
    public DefaultProgress getProgress()
    {
        return progress;
    }

    @Override
    public boolean isSoftSync()
    {
        return synchronizationFlags.contains(SynchronizationFlag.SOFT_SYNC);
    }
    
    private void syncActivity()
    {

        activitySynchronizer.synchronize(repository, isSoftSync());

    }
}
