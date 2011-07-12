package com.atlassian.jira.plugins.bitbucket.bitbucket;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public interface Bitbucket
{
    /**
     * Retrieves information about a bitbucket user
     * @param username the user to load
     * @return the bitbucket user details
     */
    public BitbucketUser getUser(String username);

    /**
     * Retrieves information about a repository
     *
     * @param auth  the authentication rules for this request
     * @param owner the owner of the project
     * @param slug  the slug of the project
     * @return the project
     */
    public BitbucketRepository getRepository(BitbucketAuthentication auth, String owner, String slug);

    /**
     * Retrieves information about a changeset by changeset id
     *
     * @param auth  the authentication rules for this request
     * @param owner the owner of the project
     * @param slug  the slug of the project
     * @param id    the changeset id
     * @return the project
     */
    public BitbucketChangeset getChangeset(BitbucketAuthentication auth, String owner, String slug, String id);

    /**
     * Retrieves all changesets for the specified repository
     *
     * @param auth  the authentication rules for this request
     * @param owner the owner of the project
     * @param slug  the slug of the project
     * @return the project
     */
    public Iterable<BitbucketChangeset> getChangesets(BitbucketAuthentication auth, String owner, String slug);

}
