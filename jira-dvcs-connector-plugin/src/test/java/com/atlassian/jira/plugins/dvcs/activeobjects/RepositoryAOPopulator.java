package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.google.common.collect.ImmutableMap;
import net.java.ao.EntityManager;

import java.util.Map;

public class RepositoryAOPopulator extends AOPopulator
{
    public RepositoryAOPopulator(final EntityManager entityManager)
    {
        super(entityManager);
    }

    public RepositoryMapping createEnabledRepository(OrganizationMapping organizationMapping)
    {
        return createRepository(organizationMapping, false, true);
    }

    public RepositoryMapping createRepository(OrganizationMapping organizationMapping, final boolean deleted, final boolean linked)
    {
        return createRepository(organizationMapping, deleted, linked, "user/someRepo");
    }

    public RepositoryMapping createRepository(OrganizationMapping organizationMapping, final boolean deleted,
            final boolean linked, String slug)
    {
        final Map<String, Object> params = ImmutableMap.<String, Object>of(
                RepositoryMapping.DELETED, deleted,
                RepositoryMapping.LINKED, linked,
                RepositoryMapping.ORGANIZATION_ID, organizationMapping.getID(),
                RepositoryMapping.SLUG, slug);
        RepositoryMapping repository = create(RepositoryMapping.class, params);

        return repository;
    }
}
