package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;
import java.util.Map;

/**
 * Gets the changeset for one or more issue keys or repository from connected dvcs account
 *
 * @since v1.4.3
 */
/*
This is a subset/partial service of {@link com.atlassian.jira.plugins.dvcs.service.ChangesetService}, that
only exposes basic and read-only information designed to be consumed externally
*/
@PublicApi
public interface DvcsChangesetService
{
    /**
     * Find all changesets by repository
     *
     * @param repository the repository to find
     * @return list of {@link Changeset}
     */
    List<Changeset> getChangesets(Repository repository);

    /**
     * Find all changesets by one or more issue keys
     *
     * @param issueKeys the list of issue keys to find
     * @return list of (@link Changeset}
     */
    List<Changeset> getChangesets(Iterable<String> issueKeys);

    /**
     * Find all changesets by defining a {@link GlobalFilter}.
     * Result is ordered in date descending order (latest first), limited by maxResults
     *
     * @param maxResults the maximum number of changeset to include
     * @param globalFilter
     * @return
     */
    List<Changeset> getChangesets(int maxResults, GlobalFilter globalFilter);

    /**
     * Retrieve the commit statistics for a particular {@link Changeset} from the given {@link Repository}
     *
     * @param repository the repository to find
     * @param changeset the changeset to get the commit statistics
     * @return {@link Changeset} with the commit statistics filled in
     */
    Changeset getDetailChangeset(Repository repository, Changeset changeset);

    /**
     * Retrieve the commit URL of a the given {@link Changeset} from the given {@link Repository}
     *
     * @param repository the repository to find
     * @param changeset the changeset to get the commit URL
     * @return String
     */
    String getChangesetURL(Repository repository, Changeset changeset);


    /**
     * Find the changeset for each individual file for all the files committed within a {@link Changeset}.
     * Also includes the commit URL for each respective file changeset.
     *
     * @param repository the repository to find
     * @param changeset the changeset to get all the file changesets and urls
     * @return {@link Map<ChangesetFile, String>} a mapping of the file changeset to changeset url
     */
    Map<ChangesetFile, String> getFileChangesets(Repository repository, Changeset changeset);
}