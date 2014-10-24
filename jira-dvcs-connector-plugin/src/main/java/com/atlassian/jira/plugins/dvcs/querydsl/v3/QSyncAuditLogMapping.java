package com.atlassian.jira.plugins.dvcs.querydsl.v3;

import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.mysema.query.sql.ColumnMetadata;
import com.mysema.query.sql.RelationalPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import java.sql.Types;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

/**
 * Generated by https://bitbucket.org/atlassian/querydsl-ao-code-gen
 */
public class QSyncAuditLogMapping extends RelationalPathBase<QSyncAuditLogMapping>
{

    private static final long serialVersionUID = 869337124L;

    public static final String AO_TABLE_NAME = "AO_E8B6CC_SYNC_AUDIT_LOG";

    public static final QSyncAuditLogMapping withSchema(SchemaProvider schemaProvider)
    {
        String schema = schemaProvider.getSchema(AO_TABLE_NAME);
        return new QSyncAuditLogMapping("SYNC_AUDIT_LOG", schema, AO_TABLE_NAME);
    }

    /**
     * Database Columns
     */
    // We have not yet built QueryDSL type support for java.util.Date getEndDate()


    public final StringPath EXC_TRACE = createString("EXC_TRACE");

    // We have not yet built QueryDSL type support for java.util.Date getFirstRequestDate()


    public final NumberPath<Integer> FLIGHT_TIME_MS = createNumber("FLIGHT_TIME_MS", Integer.class);

    public final NumberPath<Integer> ID = createNumber("ID", Integer.class);

    public final NumberPath<Integer> NUM_REQUESTS = createNumber("NUM_REQUESTS", Integer.class);

    public final NumberPath<Integer> REPO_ID = createNumber("REPO_ID", Integer.class);

    // We have not yet built QueryDSL type support for java.util.Date getStartDate()


    public final StringPath SYNC_STATUS = createString("SYNC_STATUS");

    public final StringPath SYNC_TYPE = createString("SYNC_TYPE");

    public final NumberPath<Integer> TOTAL_ERRORS = createNumber("TOTAL_ERRORS", Integer.class);


    public final com.mysema.query.sql.PrimaryKey<QSyncAuditLogMapping> SYNCAUDITLOG_PK = createPrimaryKey(ID);

    public QSyncAuditLogMapping(String variable, String schema, String table)
    {
        super(QSyncAuditLogMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    private void addMetadata()
    {
        /**
         * Database Metadata is not yet used by QueryDSL but it might one day.
         */

        addMetadata(EXC_TRACE, ColumnMetadata.named("EXC_TRACE").ofType(Types.VARCHAR)); // .withSize(2147483647)); // until detect primitive types, int ..

        addMetadata(FLIGHT_TIME_MS, ColumnMetadata.named("FLIGHT_TIME_MS").ofType(Types.INTEGER)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(ID, ColumnMetadata.named("ID").ofType(Types.INTEGER)); // .withSize(0).withNotNull()); // until detect primitive types, int ..
        addMetadata(NUM_REQUESTS, ColumnMetadata.named("NUM_REQUESTS").ofType(Types.INTEGER)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(REPO_ID, ColumnMetadata.named("REPO_ID").ofType(Types.INTEGER)); // .withSize(0)); // until detect primitive types, int ..

        addMetadata(SYNC_STATUS, ColumnMetadata.named("SYNC_STATUS").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(SYNC_TYPE, ColumnMetadata.named("SYNC_TYPE").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(TOTAL_ERRORS, ColumnMetadata.named("TOTAL_ERRORS").ofType(Types.INTEGER)); // .withSize(0)); // until detect primitive types, int ..
    }
}