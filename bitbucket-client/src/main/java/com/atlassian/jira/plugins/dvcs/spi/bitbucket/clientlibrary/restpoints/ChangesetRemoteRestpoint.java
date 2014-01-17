package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;

/**
 * ChangesetRemoteRestpoint
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class ChangesetRemoteRestpoint
{
    private static final ResponseCallback<BitbucketChangesetPage> BITBUCKET_CHANGESETS_PAGE_RESPONSE = new ResponseCallback<BitbucketChangesetPage>()
    {

        @Override
        public BitbucketChangesetPage onResponse(RemoteResponse response)
        {
            return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketChangesetPage>()
            {
            }.getType());
        }

    };
    private final RemoteRequestor requestor;

    public ChangesetRemoteRestpoint(RemoteRequestor remoteRequestor)
    {
        this.requestor = remoteRequestor;
    }

    public BitbucketChangeset getChangeset(String owner, String slug, String node)
    {
        String getChangesetUrl = URLPathFormatter.format("/repositories/%s/%s/changesets/%s", owner, slug, node);

        return requestor.get(getChangesetUrl, null, new ResponseCallback<BitbucketChangeset>()
        {

            @Override
            public BitbucketChangeset onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), BitbucketChangeset.class);
            }
        });
    }

    // "/api/1.0/repositories/erik/bitbucket/changesets/4a233e7b8596e5b17dd672f063e40f7c544c2c81"
    public BitbucketChangeset getChangeset(String urlIncludingApi)
    {
        return requestor.get(URLPathFormatter.format(urlIncludingApi), null, new ResponseCallback<BitbucketChangeset>()
        {

            @Override
            public BitbucketChangeset onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), BitbucketChangeset.class);
            }

        });

    }

    public List<BitbucketChangesetWithDiffstat> getChangesetDiffStat(String owner, String slug, String node, int limit)
    {
        String getChangesetDiffStatUrl = URLPathFormatter.format("/repositories/%s/%s/changesets/%s/diffstat", owner, slug, node);

        Map<String, String> parameters = null;
        // Requesting one more stat than limit to find out whether there are more stats
        parameters = Collections.singletonMap("limit", "" + (limit + 1));

        return requestor.get(getChangesetDiffStatUrl, parameters,
                new ResponseCallback<List<BitbucketChangesetWithDiffstat>>()
                {
                    @Override
                    public List<BitbucketChangesetWithDiffstat> onResponse(RemoteResponse response)
                    {
                        return ClientUtils.fromJson(response.getResponse(),
                                new TypeToken<List<BitbucketChangesetWithDiffstat>>()
                                {
                                }.getType());
                    }
                });
    }

    public Iterable<BitbucketNewChangeset> getChangesets(final String owner, final String slug, final List<String> includeNodes,
                                                         final List<String> excludeNodes, final int changesetsLimit, final BitbucketChangesetPage currentPage)
    {
        final ChangesetRemoteRestpoint changesetRemoteRestpoint = this;
        return new Iterable<BitbucketNewChangeset>()
        {
            @Override
            public Iterator<BitbucketNewChangeset> iterator()
            {
                return new BitbucketChangesetIterator(changesetRemoteRestpoint, owner, slug, includeNodes, excludeNodes, changesetsLimit, currentPage);
            }
        };
    }

    public BitbucketChangesetPage getNextChangesetsPage(String orgName, String slug, List<String> includeNodes, List<String> excludeNodes, int changesetLimit, BitbucketChangesetPage currentPage) {
        Map<String, List<String>> parameters = null;
        String url = null;

        if (currentPage == null || StringUtils.isBlank(currentPage.getNext()))
        {
            // this is the first request, first page
            url = getUrlForInitialRequest(orgName, slug, changesetLimit, currentPage);

            parameters = getHttpParametersMap(includeNodes, excludeNodes);
        }
        else
        {
            url = currentPage.getNext();
        }

        try
        {
            return requestor.getWithMultipleVals(url, parameters, new ResponseCallback<BitbucketChangesetPage >()
            {
                @Override
                public BitbucketChangesetPage onResponse(RemoteResponse response)
                {
                    return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketChangesetPage>(){}.getType());
                }
            });
        }
        catch (BitbucketRequestException.InternalServerError_500 e)
        {
            // TODO Ideally, we should parse the response. It looks like this:
            // {"error": {"message": "Traversal state lost. Unable to resume.", "detail": null}}
            // Example URL: https://bitbucket.org/api/2.0/repositories/atlassian/jira-bitbucket-connector/commits/?pagelen=10&ctx=foo

            if (e.getMessage().contains("Traversal state lost. Unable to resume."))
            {
                // Retry request at this point, using the page set in currentPage.
                url = getUrlForInitialRequest(orgName, slug, changesetLimit, currentPage);

                parameters = getHttpParametersMap(includeNodes, excludeNodes);

                return requestor.getWithMultipleVals(url, parameters, new ResponseCallback<BitbucketChangesetPage >()
                {
                    @Override
                    public BitbucketChangesetPage onResponse(RemoteResponse response)
                    {
                        return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketChangesetPage>(){}.getType());
                    }
                });
            }
            else
            {
                // Re-raise, as it's not one we can handle
                throw e;
            }
        }
    }

    private Map<String, List<String>> getHttpParametersMap(List<String> includeNodes, List<String> excludeNodes) {
        Map<String, List<String>> parameters;
        parameters = new HashMap<String, List<String>>();
        if (includeNodes != null)
        {
            parameters.put("include", new ArrayList<String>(includeNodes));
        }
        if (excludeNodes != null)
        {
            parameters.put("exclude", new ArrayList<String>(excludeNodes));
        }
        return parameters;
    }

    private String getUrlForInitialRequest(String orgName, String slug, int changesetLimit, BitbucketChangesetPage currentPage) {
        String url;
        url = URLPathFormatter.format("/api/2.0/repositories/%s/%s/commits/?pagelen=%s&page=%s", orgName,
                slug,
                String.valueOf(changesetLimit),
                String.valueOf(currentPage == null ? 0 : currentPage.getPage()));
        return url;
    }
}
