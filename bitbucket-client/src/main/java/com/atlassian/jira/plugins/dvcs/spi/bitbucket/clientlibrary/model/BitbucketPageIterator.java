package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class BitbucketPageIterator<T> implements Iterator<T>, Iterable<T>
{

    // configs
	private final int requestLimit = 30;
    private BitbucketPullRequestPage<T> currentPage = null;

    // services
    private final RemoteRequestor requestor;
    private final String urlIncludingApi;

    /**
     * @param urlIncludingApi
     */
    public BitbucketPageIterator(RemoteRequestor requestor, String urlIncludingApi)
    {
        this.requestor = requestor;
        this.urlIncludingApi = urlIncludingApi + "?pagelen=" + requestLimit;
    }

    @Override
    public boolean hasNext()
    {
        // not initialized
        if (currentPage == null)
        {
            readPage();
        }

        return !currentPage.getValues().isEmpty() || currentPage.getNext() != null;
    }

    private void readPage()
    {
        String url = currentPage == null ? urlIncludingApi : currentPage.getNext();
        // maybe we're just at the end
        if (StringUtils.isBlank(url))
        {
            return;
        }

        requestor.get(url, null, createResponseCallback());
    }

    @Override
    public T next()
    {
        if (currentPage.getValues().isEmpty())
        {
            readPage();
        }

        if (currentPage.getValues().isEmpty())
        {
            throw new NoSuchElementException();
        }

        T currentItem = currentPage.getValues().remove(0);

        //
        // possibly read a next page
        //
        if (currentPage.getValues().isEmpty())
        {
            readPage();
        }

        return currentItem;
    }

    @Override
    public Iterator<T> iterator()
    {
        return this;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("This is unsupported.");
    }

    private ResponseCallback<BitbucketPullRequestPage<T>> createResponseCallback()
    {
        return new ResponseCallback<BitbucketPullRequestPage<T>>()
        {
            @Override
            public BitbucketPullRequestPage<T> onResponse(RemoteResponse response)
            {

                BitbucketPullRequestPage<T> remote = transformFromJson(response);

                BitbucketPageIterator.this.currentPage = remote;
                return remote;
            }
        };
    }

    protected abstract BitbucketPullRequestPage<T> transformFromJson(RemoteResponse response);

}
