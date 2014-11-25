package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.fugue.Function2;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QBranchMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QIssueToBranchMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QOrganizationMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryMapping;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.atlassian.pocketknife.api.querydsl.SelectQuery;
import com.atlassian.pocketknife.api.querydsl.StreamyResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.mysema.query.Tuple;
import com.mysema.query.types.Predicate;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@SuppressWarnings ("SpringJavaAutowiringInspection")
@Component
public class BranchQDSL
{
    private final Logger log = LoggerFactory.getLogger(BranchQDSL.class);

    private final QueryFactory queryFactory;
    private final SchemaProvider schemaProvider;

    @Autowired
    public BranchQDSL(QueryFactory queryFactory, final SchemaProvider schemaProvider)
    {
        this.queryFactory = queryFactory;
        this.schemaProvider = schemaProvider;
    }

    public List<Branch> getByIssueKeys(final Iterable<String> issueKeys, final String dvcsType)
    {
        PullRequestByIssueKeyClosure closure = new PullRequestByIssueKeyClosure(dvcsType, issueKeys, schemaProvider);
        Map<Integer, Branch> result = queryFactory.streamyFold(new HashMap<Integer, Branch>(), closure);

        return ImmutableList.copyOf(result.values());
    }

    @VisibleForTesting
    static class PullRequestByIssueKeyClosure implements QueryFactory.StreamyFoldClosure<Map<Integer, Branch>>
    {
        final String dvcsType;
        final Iterable<String> issueKeys;

        final QBranchMapping branchMapping;
        final QIssueToBranchMapping issueMapping;
        final QRepositoryMapping repositoryMapping;
        final QOrganizationMapping orgMapping;

        PullRequestByIssueKeyClosure(final String dvcsType, final Iterable<String> issueKeys, final SchemaProvider schemaProvider)
        {
            super();
            this.dvcsType = dvcsType;
            this.issueKeys = issueKeys;
            this.branchMapping = QBranchMapping.withSchema(schemaProvider);
            this.issueMapping = QIssueToBranchMapping.withSchema(schemaProvider);
            this.repositoryMapping = QRepositoryMapping.withSchema(schemaProvider);
            this.orgMapping = QOrganizationMapping.withSchema(schemaProvider);
        }

        @Override
        public Function<SelectQuery, StreamyResult> query()
        {
            return new Function<SelectQuery, StreamyResult>()
            {
                @Override
                public StreamyResult apply(@Nullable final SelectQuery select)
                {
                    final Predicate predicate = IssueKeyPredicateFactory.buildIssueKeyPredicate(issueKeys, issueMapping);

                    SelectQuery sql = select.from(branchMapping)
                            .join(issueMapping).on(branchMapping.ID.eq(issueMapping.BRANCH_ID))
                            .join(repositoryMapping).on(repositoryMapping.ID.eq(branchMapping.REPOSITORY_ID))
                            .join(orgMapping).on(orgMapping.ID.eq(repositoryMapping.ORGANIZATION_ID))
                            .where(repositoryMapping.DELETED.eq(false)
                                    .and(repositoryMapping.LINKED.eq(true))
                                    .and(predicate));

                    if (StringUtils.isNotBlank(dvcsType))
                    {
                        sql = sql.where(orgMapping.DVCS_TYPE.eq(dvcsType));
                    }

                    return sql.stream(
                            branchMapping.ID,
                            branchMapping.NAME,
                            branchMapping.REPOSITORY_ID,
                            issueMapping.ISSUE_KEY);
                }
            };
        }

        @Override
        public Function2<Map<Integer, Branch>, Tuple, Map<Integer, Branch>> getFoldFunction()
        {
            return new Function2<Map<Integer, Branch>, Tuple, Map<Integer, Branch>>()
            {
                @Override
                public Map<Integer, Branch> apply(final Map<Integer, Branch> integerBranchMap, final Tuple tuple)
                {
                    Integer id = tuple.get(branchMapping.ID);
                    Branch branch = integerBranchMap.get(id);
                    if (branch == null)
                    {
                        branch = new Branch(tuple.get(branchMapping.ID), tuple.get(branchMapping.NAME),
                                tuple.get(branchMapping.REPOSITORY_ID));
                        integerBranchMap.put(id, branch);
                    }
                    String issueKey = tuple.get(issueMapping.ISSUE_KEY);

                    if (!branch.getIssueKeys().contains(issueKey))
                    {
                        branch.getIssueKeys().add(issueKey);
                    }

                    return integerBranchMap;
                }
            };
        }
    }
}
