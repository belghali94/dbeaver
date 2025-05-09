/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.util.List;
import java.util.Map;

/**
 * OracleProcedureManager
 */
public class OracleProcedureManager extends SQLObjectEditor<OracleProcedureStandalone, OracleSchema> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleProcedureStandalone> getObjectsCache(OracleProcedureStandalone object)
    {
        return object.getSchema().proceduresCache;
    }

    @Override
    protected OracleProcedureStandalone createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container, Object copyFrom, @NotNull Map<String, Object> options)
    {
        return new OracleProcedureStandalone(
            (OracleSchema) container,
            "NEW_PROCEDURE",
            DBSProcedureType.PROCEDURE);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand objectCreateCommand, @NotNull Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(executionContext, actions, objectCreateCommand.getObject());
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand objectDeleteCommand, @NotNull Map<String, Object> options)
    {
        final OracleProcedureStandalone object = objectDeleteCommand.getObject();
        actions.add(
            new SQLDatabasePersistAction("Drop procedure",
                "DROP " + object.getProcedureType().name() + " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-1$ //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand objectChangeCommand, @NotNull Map<String, Object> options)
    {
        createOrReplaceProcedureQuery(executionContext, actionList, objectChangeCommand.getObject());
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    private void createOrReplaceProcedureQuery(DBCExecutionContext executionContext, List<DBEPersistAction> actionList, OracleProcedureStandalone procedure)
    {
        String source = OracleUtils.normalizeSourceName(procedure, false);
        if (source == null) {
            return;
        }
        actionList.add(new OracleObjectValidateAction(procedure, OracleObjectType.PROCEDURE, "Create procedure", source)); //$NON-NLS-2$
        OracleUtils.addSchemaChangeActions(executionContext, actionList, procedure);
    }

}
