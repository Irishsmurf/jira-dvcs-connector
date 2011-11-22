package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.impl.GithubOAuthAuthentication;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;

// TODO make it a component 
public class CommunicatorHelper
{

    private final Logger logger = LoggerFactory.getLogger(CommunicatorHelper.class);

    protected final RequestFactory<?> requestFactory;

    public CommunicatorHelper(RequestFactory<?> requestFactory)
    {
        this.requestFactory = requestFactory;
    }

    public String get(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl) throws ResponseException
    {
        return runRequest(Request.MethodType.GET, apiBaseUrl, urlPath, auth, params, null);
    }

    public void get(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl, ResponseHandler responseHandler) throws ResponseException
    {
        runRequest(Request.MethodType.GET, apiBaseUrl, urlPath, auth, params, null, responseHandler);
    }

    public String post(Authentication auth, String urlPath, String postData, String apiBaseUrl) throws ResponseException
    {
        return runRequest(Request.MethodType.POST, apiBaseUrl, urlPath, auth, null, postData);
    }

    public void delete(Authentication auth, String apiUrl, String urlPath) throws ResponseException
    {
        runRequest(Request.MethodType.DELETE, apiUrl, urlPath, auth, null, null);
    }

    private String runRequest(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth,
        Map<String, Object> params, String postData) throws ResponseException
    {
        return runRequest(methodType, apiBaseUrl, urlPath, auth, params, postData, null);
    }

    private String runRequest(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth,
                                Map<String, Object> params, String postData, ResponseHandler responseHandler) throws ResponseException
    {
        String url = apiBaseUrl + urlPath + buildQueryString(params);
        logger.debug("get [ " + url + " ]");
        if (auth instanceof GithubOAuthAuthentication)
        {
            url+="&access_token="+((GithubOAuthAuthentication) auth).getAccessToken();
        }
        Request<?, ?> request = requestFactory.createRequest(methodType, url);
        if (auth != null) auth.addAuthentication(request, url);
        if (postData != null) request.setRequestBody(postData);
        request.setSoTimeout(60000);
        if (responseHandler!=null)
        {
            request.execute(responseHandler);
            return null;
        } else
        {
            return request.execute();
        }
    }

    private String buildQueryString(Map<String, Object> params)
    {
        StringBuilder queryStringBuilder = new StringBuilder();

        if (params != null && !params.isEmpty())
        {
            queryStringBuilder.append("?");
            for (Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry<String, Object> entry = iterator.next();
                queryStringBuilder.append(CustomStringUtils.encode(entry.getKey()));
                queryStringBuilder.append("=");
                queryStringBuilder.append(CustomStringUtils.encode(String.valueOf(entry.getValue())));
                if (iterator.hasNext()) queryStringBuilder.append("&");
            }
        }
        return queryStringBuilder.toString();
    }

    
    public static class ExtendedResponseHandler implements ResponseHandler<Response>
    {
        private final AtomicReference<Integer> statusCode = new AtomicReference<Integer>();
        private final AtomicReference<String> responseString = new AtomicReference<String>();
        private final AtomicReference<Boolean> isSuccessful = new AtomicReference<Boolean>();
       
        @Override
        public void handle(Response response) throws ResponseException
        {
            isSuccessful.set(response.isSuccessful());
            statusCode.set(response.getStatusCode());
            responseString.set(response.getResponseBodyAsString());
        }
        
        public int getStatusCode()
        {
            return statusCode.get();
        }
        
        public String getResponseString()
        {
            return responseString.get();
        }
        
        public boolean isSuccessful()
        {
            return isSuccessful.get();
        }
    }
    
    public Boolean isRepositoryPrivate1(final RepositoryUri repositoryUri)
    {
        ExtendedResponseHandler responseHandler = new ExtendedResponseHandler();
        try
        {
            get(Authentication.ANONYMOUS, repositoryUri.getRepositoryInfoUrl(), null, repositoryUri.getApiUrl(), responseHandler);
            if (responseHandler.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                return true;
            } 
            // is this valid JSON?
            new JSONObject(responseHandler.getResponseString());
            return false;
        } catch (JSONException e)
        {
            logger.debug(e.getMessage());
        } catch (ResponseException e)
        {
            logger.debug(e.getMessage());
        }
        return null;
    }

    
}
