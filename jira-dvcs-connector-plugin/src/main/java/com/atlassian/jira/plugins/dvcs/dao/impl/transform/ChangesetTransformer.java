package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class ChangesetTransformer
{
    public static final Logger log = LoggerFactory.getLogger(ChangesetTransformer.class);
    private final ActiveObjects activeObjects;

    public ChangesetTransformer(final ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    public Changeset transform(ChangesetMapping changesetMapping, int mainRepositoryId, String dvcsType)
    {

        if (changesetMapping == null)
        {
            return null;
        }

//        log.debug("Changeset transformation: [{}] ", changesetMapping);

        FileData fileData = parseFilesData(changesetMapping.getFilesData());
        List<String> parents = parseParentsData(changesetMapping.getParentsData());

        final Changeset changeset = transform(mainRepositoryId, changesetMapping, fileData, changesetMapping.getFileDetailsJson(), parents);
        
        List<Integer> repositories = changeset.getRepositoryIds();
        int firstRepository = 0;

        for (RepositoryMapping repositoryMapping : changesetMapping.getRepositories())
        {
            if (repositoryMapping.isDeleted() || !repositoryMapping.isLinked())
            {
                continue;
            }

            if (!StringUtils.isEmpty(dvcsType))
            {
                OrganizationMapping organizationMapping = activeObjects.get(OrganizationMapping.class, repositoryMapping.getOrganizationId());

                if (!dvcsType.equals(organizationMapping.getDvcsType()))
                {
                   continue;
                }
            }

            if (repositories == null)
            {
                repositories = new ArrayList<Integer>();
                changeset.setRepositoryIds(repositories);

                // mark first repository
                firstRepository = repositoryMapping.getID();
            }

            // we found repository that is not fork and no main repository is set on changeset,let's use it
            if (changeset.getRepositoryId() == 0 && !repositoryMapping.isFork())
            {
                changeset.setRepositoryId(repositoryMapping.getID());
            }

            repositories.add(repositoryMapping.getID());
        }

        // no main repository was assigned, let's use the first one
        if (changeset.getRepositoryId() == 0)
        {
            changeset.setRepositoryId(firstRepository);
        }
        return CollectionUtils.isEmpty(changeset.getRepositoryIds())? null : changeset;
    }

    public Changeset transform(int repositoryId, ChangesetMapping changesetMapping)
    {
        if (changesetMapping == null)
        {
            return null;
        }

        FileData fileData = parseFilesData(changesetMapping.getFilesData());
        List<String> parents = parseParentsData(changesetMapping.getParentsData());

        return transform(repositoryId, changesetMapping, fileData, changesetMapping.getFileDetailsJson(), parents);
    }

    private Changeset transform(final int repositoryId, final ChangesetMapping changesetMapping, final FileData fileData, @Nullable String fileDetailsJson, final List<String> parents)
    {
        final Changeset changeset = new Changeset(repositoryId,
                changesetMapping.getNode(),
                changesetMapping.getRawAuthor(),
                changesetMapping.getAuthor(),
                changesetMapping.getDate(),
                changesetMapping.getRawNode(),
                changesetMapping.getBranch(),
                changesetMapping.getMessage(),
                parents,
                fileData.getFiles(),
                fileData.getFileCount(),
                changesetMapping.getAuthorEmail());

        changeset.setId(changesetMapping.getID());
        changeset.setVersion(changesetMapping.getVersion());
        changeset.setSmartcommitAvaliable(changesetMapping.isSmartcommitAvailable());
        changeset.setFileDetails(ChangesetFileDetails.fromJSON(fileDetailsJson));

        return changeset;
    }

    private List<String> parseParentsData(String parentsData)
    {
        if (ChangesetMapping.TOO_MANY_PARENTS.equals(parentsData))
        {
            return null;
        }
        
        List<String> parents = new ArrayList<String>();

        if (StringUtils.isBlank(parentsData))
        {
            return parents;
        }

        try
        {
            JSONArray parentsJson = new JSONArray(parentsData);
            for (int i = 0; i < parentsJson.length(); i++)
            {
                parents.add(parentsJson.getString(i));
            }
        } catch (JSONException e)
        {
            log.error("Failed parsing parents from ParentsJson data.");
        }

        return parents;
    }

    private FileData parseFilesData(String filesData)
    {
        List<ChangesetFile> files = new ArrayList<ChangesetFile>();
        int fileCount = 0;

        if (StringUtils.isNotBlank(filesData))
        {
            try
            {
                JSONObject filesDataJson = new JSONObject(filesData);
                fileCount = filesDataJson.getInt("count");
                JSONArray filesJson = filesDataJson.getJSONArray("files");

                for (int i = 0; i < filesJson.length(); i++)
                {
                    JSONObject file = filesJson.getJSONObject(i);
                    String filename = file.getString("filename");
                    String status = file.getString("status");

                    files.add(new ChangesetFile(CustomStringUtils.getChangesetFileAction(status), filename));
                }

            } catch (JSONException e)
            {
                log.error("Failed parsing files from FileJson data.");
            }
        }

        return new FileData(files, fileCount);
    }

    private static class FileData
    {
        private final List<ChangesetFile> files;
        private final int fileCount;

        FileData(List<ChangesetFile> files, int fileCount)
        {
            this.files = files;
            this.fileCount = fileCount;
        }

        public List<ChangesetFile> getFiles()
        {
            return files;
        }

        public int getFileCount()
        {
            return fileCount;
        }

    }
}
