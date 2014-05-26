package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.annotations.VisibleForTesting;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.Immutable;

import static com.atlassian.util.concurrent.ThreadFactories.Type.DAEMON;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class EventServiceImpl implements EventService
{
    private static final Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);
    private static final int DESTROY_TIMEOUT_SECS = 10;

    private final EventPublisher eventPublisher;
    private final SyncEventDao syncEventDao;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService eventDispatcher;

    @Autowired
    public EventServiceImpl(EventPublisher eventPublisher, SyncEventDao syncEventDao)
    {
        this(eventPublisher, syncEventDao, createEventDispatcher());
    }

    @VisibleForTesting
    EventServiceImpl(EventPublisher eventPublisher, SyncEventDao syncEventDao, ExecutorService executorService)
    {
        this.eventPublisher = eventPublisher;
        this.syncEventDao = syncEventDao;
        this.eventDispatcher = executorService;
    }

    public void storeEvent(Repository repository, SyncEvent event) throws IllegalArgumentException
    {
        try
        {
            syncEventDao.save(toSyncEventMapping(repository, event));
            logger.debug("Saved event for repository {}: {}", repository, event);
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("Can't store event (unable to convert to JSON): " + event, e);
        }
    }

    public void dispatchEvents(Repository repository)
    {
        final DispatchRequest dispatchRequest = new DispatchRequest(repository);

        // do the event dispatching asynchronously
        eventDispatcher.submit(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                try
                {
                    doDispatchEvents(dispatchRequest);
                    return null;
                }
                catch (Throwable t)
                {
                    logger.error("Error dispatching events for: " + dispatchRequest, t);
                    throw new RuntimeException(t);
                }
            }
        });
    }

    /**
     * Shuts down the event dispatcher pool. This method waits for up to {@link #DESTROY_TIMEOUT_SECS} for the executor
     * to shut down before logging an error and returning.
     */
    @PreDestroy
    public void destroy()
    {
        destroyEventDispatcher();
    }

    /**
     * Dispatches all events for a repo. If the thread running this method is interrupted while this method is
     * dispatching events this method will return early, preserving the thread's interrupted status.
     *
     * @param dispatch a DispatchRequest
     */
    private void doDispatchEvents(DispatchRequest dispatch)
    {
        List<SyncEventMapping> eventMappings = syncEventDao.findAllByRepoId(dispatch.repoId());
        for (int i = 0, size = eventMappings.size(); i < size; i++)
        {
            if (Thread.interrupted())
            {
                logger.error("Thread interrupted after dispatching {}/{} events for: {}", new Object[] { i, size, dispatch });
                Thread.currentThread().interrupt();
                return;
            }

            final SyncEventMapping syncEventMapping = eventMappings.get(i);
            try
            {
                final SyncEvent event = fromSyncEventMapping(syncEventMapping);

                logger.debug("Publishing event for repository {}: {}", dispatch, event);
                eventPublisher.publish(event);
            }
            catch (ClassNotFoundException e)
            {
                logger.error("Can't dispatch event (event class not found): " + syncEventMapping.getEventClass(), e);
            }
            catch (IOException e)
            {
                logger.error("Can't dispatch event (unable to convert from JSON): " + syncEventMapping.getEventJson(), e);
            }
            finally
            {
                syncEventDao.delete(syncEventMapping);
            }
        }
    }

    @Override
    public void discardEvents(Repository repository)
    {
        int deleted = syncEventDao.deleteAll(repository.getId());
        logger.debug("Deleted {} events from repo: {}", deleted, repository);
    }

    private SyncEventMapping toSyncEventMapping(Repository repository, SyncEvent event) throws IOException
    {
        SyncEventMapping mapping = syncEventDao.create();
        mapping.setRepoId(repository.getId());
        mapping.setEventDate(event.getDate());
        mapping.setEventClass(event.getClass().getName());
        mapping.setEventJson(objectMapper.writeValueAsString(event));

        return mapping;
    }

    private SyncEvent fromSyncEventMapping(SyncEventMapping mapping) throws ClassNotFoundException, IOException
    {
        final Class<?> eventClass = Class.forName(mapping.getEventClass());

        return (SyncEvent) objectMapper.readValue(mapping.getEventJson(), eventClass);
    }

    private void destroyEventDispatcher()
    {
        eventDispatcher.shutdownNow();
        try
        {
            boolean destroyed = eventDispatcher.awaitTermination(DESTROY_TIMEOUT_SECS, SECONDS);
            if (!destroyed)
            {
                logger.error("ExecutorService did not shut down within {}s", DESTROY_TIMEOUT_SECS);
            }
        }
        catch (InterruptedException e)
        {
            logger.error("Interrupted while waiting for ExecutorService to shut down.");
            Thread.currentThread().interrupt();
        }
    }

    private static ExecutorService createEventDispatcher()
    {
        return Executors.newSingleThreadExecutor(ThreadFactories
                        .named("DVCSConnector.EventService")
                        .type(DAEMON)
                        .build()
        );
    }

    /**
     * Parameter object for dispatching events. Makes it easier to provide a sane toString() in logging, etc.
     */
    @Immutable
    private static class DispatchRequest
    {
        private final int repoId;
        private final String repoToString;

        public DispatchRequest(@Nonnull Repository repository)
        {
            this.repoId = repository.getId();
            this.repoToString = repository.toString();
        }

        public int repoId()
        {
            return repoId;
        }

        @Override
        public String toString()
        {
            return String.format("Repository[%s]", repoToString);
        }
    }
}