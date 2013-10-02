package com.atlassian.jira.plugins.dvcs.dao.impl;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;

import java.util.Iterator;

/**
 *
 */
public class GlobalFilterQueryWhereClauseBuilder
{
    private final GlobalFilter gf;

    public GlobalFilterQueryWhereClauseBuilder(GlobalFilter gf)
    {
        this.gf = gf;
    }

    public String build()
    {
        StringBuilder whereClauseProjectsSb = new StringBuilder();
        StringBuilder whereClauseIssueKyesSb = new StringBuilder();
        StringBuilder whereClauseUsersSb = new StringBuilder();
        if (gf != null)
        {

            if (gf.getInProjects() != null && gf.getInProjects().iterator().hasNext())
            {
                whereClauseProjectsSb.append(renderSqlIn("ISSUE." + IssueToChangesetMapping.PROJECT_KEY, gf.getInProjects())).append(" ");
            }
            if (gf.getNotInProjects() != null && gf.getNotInProjects().iterator().hasNext())
            {
                if (whereClauseProjectsSb.length() != 0)
                {
                    whereClauseProjectsSb.append(" AND ");
                }


                whereClauseProjectsSb.append(renderSqlNotIn("ISSUE." + IssueToChangesetMapping.PROJECT_KEY, gf.getNotInProjects())).append(" ");
            }

            if (gf.getInIssues() != null && gf.getInIssues().iterator().hasNext())
            {
                whereClauseIssueKyesSb.append(renderSqlIn("ISSUE." + IssueToChangesetMapping.ISSUE_KEY, gf.getInIssues())).append(" ");
            }
            if (gf.getNotInIssues() != null && gf.getNotInIssues().iterator().hasNext())
            {
                if (whereClauseIssueKyesSb.length() != 0)
                {
                    whereClauseIssueKyesSb.append(" AND ");
                }

                whereClauseIssueKyesSb.append(renderSqlNotIn("ISSUE." + IssueToChangesetMapping.ISSUE_KEY, gf.getNotInIssues())).append(" ");
            }

            if (gf.getInUsers() != null && gf.getInUsers().iterator().hasNext())
            {
                whereClauseUsersSb.append(renderSqlIn("CHANGESET." + ChangesetMapping.AUTHOR, gf.getInUsers())).append(" ");
            }
            if (gf.getNotInUsers() != null && gf.getNotInUsers().iterator().hasNext())
            {
                whereClauseUsersSb.append(renderSqlNotIn("CHANGESET." + ChangesetMapping.AUTHOR, gf.getNotInUsers())).append(" ");
            }
        }
        StringBuilder whereClauseSb = new StringBuilder();
        if (whereClauseProjectsSb.length() != 0)
        {
            whereClauseSb.append("(").append(whereClauseProjectsSb.toString()).append(")");
        }
        if (whereClauseIssueKyesSb.length() != 0)
        {
            if (whereClauseSb.length() != 0)
            {
                whereClauseSb.append(" AND ");
            }
            whereClauseSb.append("(").append(whereClauseIssueKyesSb.toString()).append(")");
        }
        if (whereClauseUsersSb.length() != 0)
        {
            if (whereClauseSb.length() != 0)
            {
                whereClauseSb.append(" AND ");
            }
            whereClauseSb.append("(").append(whereClauseUsersSb.toString()).append(")");
        }

        // if no filter applied than "no" where clause should be used
        if (whereClauseSb.length() == 0)
        {
            whereClauseSb.append(" true ");
        }
        return whereClauseSb.toString();
    }

    private StringBuilder renderSqlNotIn(final String column, final Iterable<String> values)
    {
        return renderListOperator(column, "NOT IN", "AND", values);
    }

    private StringBuilder renderSqlIn(final String column, final Iterable<String> values)
    {
        return renderListOperator(column, "IN", "OR", values);
    }

    /**
     * Oracle can't handle IN with more than 1000 values, this code makes sure larger sets are split to contain no more than 1000 items each
     *
     * @param column
     * @param operator
     * @param joinWithOperator
     * @param values
     * @return
     */
    private StringBuilder renderListOperator(final String column, final String operator, final String joinWithOperator, final Iterable<String> values)
    {
        final StringBuilder builder = new StringBuilder(column);
        builder.append(" ").append(operator).append(" (");
        final Iterator<String> valuesIterator = values.iterator();
        int valuesInQuery = 0;
        boolean overThousandValues = false;
        while(valuesIterator.hasNext())
        {
            final String value = valuesIterator.next();
            if (StringUtils.isNotEmpty(value))
            {
                if (valuesInQuery > 0)
                {
                    builder.append(", ");
                }
                builder.append("'").append(value).append("'");
                ++valuesInQuery;
                if (valuesInQuery >= 1000)
                {
                    overThousandValues = true;
                    valuesInQuery = 0;
                    builder.append(") ").append(joinWithOperator).append(" ").append(column).append(" ").append(operator).append(" (");
                }
            }
        }
        builder.append(")");
        return overThousandValues ? builder.insert(0, "(").append(")") : builder;
    }
}
