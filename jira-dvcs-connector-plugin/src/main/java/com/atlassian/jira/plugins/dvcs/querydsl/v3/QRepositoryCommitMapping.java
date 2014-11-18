package com.atlassian.jira.plugins.dvcs.querydsl.v3;

import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import java.sql.Types;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.Path;

import com.mysema.query.sql.ColumnMetadata;
import com.mysema.query.sql.RelationalPathBase;

/**
 *
 * Generated by https://bitbucket.org/atlassian/querydsl-ao-code-gen
 */
public class QRepositoryCommitMapping extends RelationalPathBase<QRepositoryCommitMapping> {

    private static final long serialVersionUID = -2137284743L;

    public static final String AO_TABLE_NAME  = "AO_E8B6CC_COMMIT";

    public static final QRepositoryCommitMapping withSchema(SchemaProvider schemaProvider)
    {
        String schema = schemaProvider.getSchema(AO_TABLE_NAME);
        return new QRepositoryCommitMapping("COMMIT", schema, AO_TABLE_NAME);
    }

    /**
     * Database Columns
     */
    public final StringPath AUTHOR = createString("AUTHOR");

    public final StringPath AUTHOR_AVATAR_URL = createString("AUTHOR_AVATAR_URL");

    // We have not yet built QueryDSL type support for java.util.Date getDate()


    public final NumberPath<Integer> ID = createNumber("ID", Integer.class);

    public final StringPath MESSAGE = createString("MESSAGE");

    public final StringPath NODE = createString("NODE");

    public final StringPath RAW_AUTHOR = createString("RAW_AUTHOR");


    public final com.mysema.query.sql.PrimaryKey<QRepositoryCommitMapping> COMMIT_PK = createPrimaryKey(ID);

    public QRepositoryCommitMapping(String variable, String schema, String table) {
        super(QRepositoryCommitMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    private void addMetadata() {
        /**
         * Database Metadata is not yet used by QueryDSL but it might one day.
         */
        addMetadata(AUTHOR, ColumnMetadata.named("AUTHOR").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(AUTHOR_AVATAR_URL, ColumnMetadata.named("AUTHOR_AVATAR_URL").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        
        addMetadata(ID, ColumnMetadata.named("ID").ofType(Types.INTEGER)); // .withSize(0).withNotNull()); // until detect primitive types, int ..
        addMetadata(MESSAGE, ColumnMetadata.named("MESSAGE").ofType(Types.VARCHAR)); // .withSize(2147483647)); // until detect primitive types, int ..
        addMetadata(NODE, ColumnMetadata.named("NODE").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(RAW_AUTHOR, ColumnMetadata.named("RAW_AUTHOR").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
    }
}