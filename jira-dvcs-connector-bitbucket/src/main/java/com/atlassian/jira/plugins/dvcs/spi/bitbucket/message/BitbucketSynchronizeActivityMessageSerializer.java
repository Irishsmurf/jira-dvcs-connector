package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;

public class BitbucketSynchronizeActivityMessageSerializer extends AbstractMessagePayloadSerializer<BitbucketSynchronizeActivityMessage>
{

    public BitbucketSynchronizeActivityMessageSerializer(RepositoryService repositoryService, Synchronizer synchronizer)
    {
        super(repositoryService, synchronizer);
    }

    @Override
    protected void serializeInternal(JSONObject json, BitbucketSynchronizeActivityMessage payload) throws Exception
    {
        json.put("page", payload.getPageNum());
        json.put("processedPullRequests", payload.getProcessedPullRequests());
        json.put("processedPullRequestsLocal", payload.getProcessedPullRequestsLocal());
        if (payload.getLastSyncDate() != null)
        {
            json.put("lastSyncDate", getDateFormat().format(payload.getLastSyncDate()));
        }
    }

    @Override
    protected BitbucketSynchronizeActivityMessage deserializeInternal(JSONObject json) throws Exception
    {
        Set<Integer> processedPullRequests;
        Set<Integer> processedPullRequestsLocal;
        Date lastSyncDate = null;
        int page = 1;

        page = json.optInt("page");
        processedPullRequests = asSet(json.optJSONArray("processedPullRequests"));
        processedPullRequestsLocal = asSet(json.optJSONArray("processedPullRequestsLocal"));
        String lastSyncOrNull = json.optString("lastSyncDate");
        if (StringUtils.isNotBlank(lastSyncOrNull))
        {
            lastSyncDate = getDateFormat().parse(lastSyncOrNull);
        }

        return new BitbucketSynchronizeActivityMessage(null, null, false, page, processedPullRequests, processedPullRequestsLocal, lastSyncDate, 0);
    }


    private Set<Integer> asSet(JSONArray optJSONArray)
    {
        Set<Integer> ret = new HashSet<Integer>();
        if (optJSONArray == null)
        {
            return ret;
        }
        for (int i = 0; i < optJSONArray.length(); i++)
        {
            ret.add(optJSONArray.optInt(i));
        }
        return ret;
    }


    @Override
    public Class<BitbucketSynchronizeActivityMessage> getPayloadType()
    {
        return BitbucketSynchronizeActivityMessage.class;
    }

}