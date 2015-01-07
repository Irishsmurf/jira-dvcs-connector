package com.atlassian.jira.plugins.dvcs.querydsl.v3;

import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.mysema.query.sql.ColumnMetadata;
import com.mysema.query.sql.RelationalPathBase;
import com.mysema.query.types.path.NumberPath;

import java.sql.Types;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

/**
 * Generated by https://bitbucket.org/atlassian/querydsl-ao-code-gen
 *
 * Changes made by hand:
 *    Map and FK mappings to integers
 *    Map Booleans to BooleanPaths
 *    Map Dates as DateTimePath<Date>
 *
 * Future approach is documented at https://extranet.atlassian.com/x/AAuQj
 */
public class QRepositoryToChangesetMapping extends RelationalPathBase<QRepositoryToChangesetMapping>
{

    private static final long serialVersionUID = 1711043431L;

    public static final String AO_TABLE_NAME = "AO_E8B6CC_REPO_TO_CHANGESET";

    public static final QRepositoryToChangesetMapping withSchema(SchemaProvider schemaProvider)
    {
        String schema = schemaProvider.getSchema(AO_TABLE_NAME);
        return new QRepositoryToChangesetMapping("REPO_TO_CHANGESET", schema, AO_TABLE_NAME);
    }

    /**
     * Database Columns
     */
    // We have not yet built QueryDSL type support for com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping getChangeset()


    public final NumberPath<Integer> ID = createNumber("ID", Integer.class);

    public final NumberPath<Integer> CHANGESET_ID = createNumber("CHANGESET_ID", Integer.class);

    public final NumberPath<Integer> REPOSITORY_ID = createNumber("REPOSITORY_ID", Integer.class);

    // We have not yet built QueryDSL type support for com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping getRepository()


    public final com.mysema.query.sql.PrimaryKey<QRepositoryToChangesetMapping> REPOTOCHANGESET_PK = createPrimaryKey(ID);

    public QRepositoryToChangesetMapping(String variable, String schema, String table)
    {
        super(QRepositoryToChangesetMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    private void addMetadata()
    {
        /**
         * Database Metadata is not yet used by QueryDSL but it might one day.
         */

        addMetadata(ID, ColumnMetadata.named("ID").ofType(Types.INTEGER)); // .withSize(0).withNotNull()); // until detect primitive types, int ..
        addMetadata(CHANGESET_ID, ColumnMetadata.named("CHANGESET_ID").ofType(Types.INTEGER));
        addMetadata(REPOSITORY_ID, ColumnMetadata.named("REPOSITORY_ID").ofType(Types.INTEGER));
    }
}