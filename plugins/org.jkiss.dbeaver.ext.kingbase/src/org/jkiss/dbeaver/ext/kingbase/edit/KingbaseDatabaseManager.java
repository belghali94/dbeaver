/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ext.kingbase.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDatabase;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreDatabaseManager;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * KingbaseDatabaseManager
 */
public class KingbaseDatabaseManager extends PostgreDatabaseManager {

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext,
                                          @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, 
                                          @NotNull Map<String, Object> options) {
        final KingbaseDatabase database = (KingbaseDatabase) command.getObject();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE DATABASE ").append(DBUtils.getQuotedIdentifier(database));

        if (database.getInitialOwner() != null) {
            sql.append("\nOWNER = ").append(DBUtils.getQuotedIdentifier(database.getInitialOwner()));
        }
        if (!CommonUtils.isEmpty(database.getTemplateName())) {
            sql.append("\nTEMPLATE = ").append(DBUtils.getQuotedIdentifier(database.getDataSource(), database.getTemplateName()));
        }
        if (database.getInitialEncoding() != null) {
            sql.append("\nENCODING = '").append(database.getInitialEncoding().getName()).append("'");
        }
        if (database.getInitialTablespace() != null) {
            sql.append("\nTABLESPACE = ")
                .append(DBUtils.getQuotedIdentifier(database.getDataSource(), database.getInitialTablespace().getName()));
        }
        actions.add(new CreateDatabaseAction(database, sql));
    }


    private static class CreateDatabaseAction extends SQLDatabasePersistActionAtomic {
        private final KingbaseDatabase database;

        public CreateDatabaseAction(PostgreDatabase database, StringBuilder sql) {
            super("Create database", sql.toString());
            this.database = (KingbaseDatabase) database;
        }

        @Override
        public void afterExecute(DBCSession session, Throwable error) throws DBCException {
            super.afterExecute(session, error);
            if (error == null) {
                try {
                    DBRProgressMonitor monitor = session.getProgressMonitor();
                    database.checkInstanceConnection(monitor);
                    database.readDatabaseInfo(monitor);
                } catch (DBException e) {
                    log.error("Can't connect to the new database");
                }
            }
        }
    }

}
