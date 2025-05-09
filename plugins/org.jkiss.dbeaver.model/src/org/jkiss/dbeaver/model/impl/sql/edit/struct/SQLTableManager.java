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
package org.jkiss.dbeaver.model.impl.sql.edit.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLStructEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * JDBC table manager
 */
public abstract class SQLTableManager<OBJECT_TYPE extends DBSEntity, CONTAINER_TYPE extends DBSObjectContainer>
    extends SQLStructEditor<OBJECT_TYPE, CONTAINER_TYPE>
{

    public static final String BASE_TABLE_NAME = "NewTable"; //$NON-NLS-1$
    public static final String BASE_VIEW_NAME = "NewView"; //$NON-NLS-1$
    public static final String BASE_MATERIALIZED_VIEW_NAME = "NewMView"; //$NON-NLS-1$

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        long options = FEATURE_EDITOR_ON_CREATE;
        {
            if (dataSource.getSQLDialect().supportsTableDropCascade()) {
                options |= FEATURE_DELETE_CASCADE;
            }
        }
        return options;
    }
    
    protected String beginCreateTableStatement(DBRProgressMonitor monitor, OBJECT_TYPE table, String tableName, Map<String, Object> options) throws DBException {
        return "CREATE " + getCreateTableType(table) + " " + tableName + " (" + GeneralUtils.getDefaultLineSeparator(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
    
    protected boolean hasAttrDeclarations(OBJECT_TYPE table) {
        return true;
    }

    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) throws DBException {
        // Make options modifiable
        options = new HashMap<>(options);
        final OBJECT_TYPE table = command.getObject();

        final NestedObjectCommand<?,?> tableProps = command.getObjectCommands().get(table);
        if (tableProps == null) {
            log.warn("Object change command not found"); //$NON-NLS-1$
            return;
        }
        final String tableName = DBUtils.getEntityScriptName(table, options);

        final String slComment = SQLUtils.getDialectFromObject(table).getSingleLineComments()[0];
        final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
        StringBuilder createQuery = new StringBuilder(100);
        createQuery.append(beginCreateTableStatement(monitor, table, tableName, options));
        boolean hasNestedDeclarations = false;
        final Collection<NestedObjectCommand> orderedCommands = getNestedOrderedCommands(command);
        for (NestedObjectCommand<?,?> nestedCommand : orderedCommands) {
            if (nestedCommand.getObject() == table) {
                continue;
            }
            if (excludeFromDDL(nestedCommand, orderedCommands)) {
                continue;
            }
            options.put(DBPScriptObject.OPTION_COMPOSITE_OBJECT, table);
            final String nestedDeclaration = nestedCommand.getNestedDeclaration(monitor, table, options);
            options.remove(DBPScriptObject.OPTION_COMPOSITE_OBJECT);
            if (!CommonUtils.isEmpty(nestedDeclaration)) {
                // Insert nested declaration
                if (hasNestedDeclarations) {
                    // Check for embedded comment
                    int lastLFPos = createQuery.lastIndexOf(lineSeparator);
                    int lastCommentPos = createQuery.lastIndexOf(slComment);
                    if (lastCommentPos != -1) {
                        while (lastCommentPos > 0 && Character.isWhitespace(createQuery.charAt(lastCommentPos - 1))) {
                            lastCommentPos--;
                        }
                    }
                    if (lastCommentPos < 0 || lastCommentPos < lastLFPos) {
                          createQuery.append(","); //$NON-NLS-1$
                    } else {
                           createQuery.insert(lastCommentPos, ","); //$NON-NLS-1$
                    }
                    createQuery.append(lineSeparator);
                }
                if (!hasNestedDeclarations && !hasAttrDeclarations(table)) {
                    createQuery.append("(\n\t").append(nestedDeclaration); //$NON-NLS-1$  
                } else {
                 createQuery.append("\t").append(nestedDeclaration); //$NON-NLS-1$
                }
                hasNestedDeclarations = true;
            } else {
                // This command should be executed separately
                final DBEPersistAction[] nestedActions = nestedCommand.getPersistActions(monitor, executionContext, options);
                if (nestedActions != null) {
                    Collections.addAll(actions, nestedActions);
                }
            }
        }
        if (hasAttrDeclarations(table) || hasNestedDeclarations) {
            createQuery.append(lineSeparator);
            createQuery.append(")"); //$NON-NLS-1$
        }

        appendTableModifiers(monitor, table, tableProps, createQuery, false);
        actions.add( 0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_table, createQuery.toString()) );
    }

    @Override
    protected boolean isIncludeChildObjectReference(DBRProgressMonitor monitor, DBSObject childObject) throws DBException {
        if (childObject instanceof DBSTableIndex) {
            return isIncludeIndexInDDL(monitor, (DBSTableIndex) childObject);
        }
        return super.isIncludeChildObjectReference(monitor, childObject);
    }

    protected String getCreateTableType(OBJECT_TYPE table) {
        return DBUtils.isView(table) ? "VIEW" : "TABLE";//$NON-NLS-1$ //$NON-NLS-2$
    }

    protected String getDropTableType(OBJECT_TYPE table) {
        return getCreateTableType(table);
    }

    protected boolean excludeFromDDL(NestedObjectCommand command, Collection<NestedObjectCommand> orderedCommands) {
        return false;
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options)
    {
        OBJECT_TYPE object = command.getObject();
        final String tableName = DBUtils.getEntityScriptName(object, options);
        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP " + getDropTableType(object) +  //$NON-NLS-2$
                " " + tableName + //$NON-NLS-2$
                (!DBUtils.isView(object) && CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE" : "") //$NON-NLS-2$
            )
        );
    }

    protected void appendTableModifiers(
        DBRProgressMonitor monitor,
        OBJECT_TYPE table,
        NestedObjectCommand tableProps,
        StringBuilder ddl,
        boolean alter
    ) throws DBException {

    }

    protected String getBaseObjectName() {
        return BASE_TABLE_NAME;
    }

    public DBEPersistAction[] getTableDDL(DBRProgressMonitor monitor, OBJECT_TYPE table, Map<String, Object> options) throws DBException
    {
        List<DBEPersistAction> actions = new ArrayList<>();

        final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();
        SQLObjectEditor<DBSEntityAttribute, OBJECT_TYPE> tcm = getObjectEditor(editorsRegistry, DBSEntityAttribute.class);
        /*
         * FIXME: We have a pretty major problem with inheritance and managers
         * FIXME: we search for constraint manager by class which is also a parent
         * FIXME: for indexes and foreign keys this may lead to incorrect manager provided for key
         * Temporary workaround - provide primary key before indexes and foreign keys in getChildTypes
         */
        SQLObjectEditor<DBSEntityConstraint, OBJECT_TYPE> pkm = getObjectEditor(editorsRegistry, DBSEntityConstraint.class);
        SQLObjectEditor<DBSTableForeignKey, OBJECT_TYPE> fkm = getObjectEditor(editorsRegistry, DBSTableForeignKey.class);
        SQLObjectEditor<DBSTableIndex, OBJECT_TYPE> im = getObjectEditor(editorsRegistry, DBSTableIndex.class);
        SQLObjectEditor<DBSTableCheckConstraint, OBJECT_TYPE> ccm = getObjectEditor(editorsRegistry, DBSTableCheckConstraint.class);

        DBCExecutionContext executionContext = DBUtils.getDefaultContext(table, true);

        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS)) {
            if (fkm != null) {
                // Create only foreign keys
                try {
                    for (DBSEntityAssociation foreignKey : CommonUtils.safeCollection(table.getAssociations(monitor))) {
                        if (!(foreignKey instanceof DBSTableForeignKey) ||
                            skipObject(foreignKey)) {
                            continue;
                        }
                        DBEPersistAction[] cmdActions = fkm.makeCreateCommand((DBSTableForeignKey) foreignKey, options).getPersistActions(monitor, executionContext, options);
                        if (cmdActions != null) {
                            Collections.addAll(actions, cmdActions);
                        }
                    }
                } catch (DBException e) {
                    // Ignore primary keys
                    log.debug(e);
                }
            }
            return actions.toArray(new DBEPersistAction[0]);
        }

        if (table.isPersisted() && isIncludeDropInDDL(table) &&
            (table.getDataSource() == null ||
                table.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_EXTRA_DDL_INFO))
        ) {
            actions.add(new SQLDatabasePersistActionComment(table.getDataSource(), "Drop table"));
            for (DBEPersistAction delAction : new ObjectDeleteCommand(table, ModelMessages.model_jdbc_delete_object).getPersistActions(monitor, executionContext, options)) {
                String script = delAction.getScript();
                String delimiter = SQLUtils.getScriptLineDelimiter(SQLUtils.getDialectFromObject(table));
                if (!script.endsWith(delimiter)) {
                    script += delimiter;
                }
                actions.add(
                    new SQLDatabasePersistActionComment(
                        table.getDataSource(),
                        script));
            }
        }

        StructCreateCommand command = makeCreateCommand(table, options);
        if (tcm != null) {
            final boolean skipNonPersisted = CommonUtils.getOption(options, DBPScriptObject.OPTION_DDL_ONLY_PERSISTED_ATTRIBUTES);
            // Aggregate nested column, constraint and index commands
            for (DBSEntityAttribute column : CommonUtils.safeCollection(table.getAttributes(monitor))) {
                if (skipObject(column) || (skipNonPersisted && !column.isPersisted())) {
                    // Do not include hidden (pseudo?) and inherited columns in DDL
                    continue;
                }
                command.aggregateCommand(tcm.makeCreateCommand(column, options));
            }
        }
        if (pkm != null && !CommonUtils.getOption(options, DBPScriptObject.OPTION_SKIP_UNIQUE_KEYS)) {
            try {
                for (DBSEntityConstraint constraint : CommonUtils.safeCollection(table.getConstraints(monitor))) {
                    if (!isIncludeConstraintInDDL(monitor, constraint)) {
                        continue;
                    }
                    command.aggregateCommand(pkm.makeCreateCommand(constraint, options));
                }
            } catch (DBException e) {
                // Ignore primary keys
                log.debug(e);
            }
        }
        if (ccm != null) {
            try {
                if (table instanceof DBSCheckConstraintContainer) {
                    for (DBSTableCheckConstraint constraint : CommonUtils.safeCollection(((DBSCheckConstraintContainer)table).getCheckConstraints(monitor))) {
                        if (skipObject(constraint)) {
                            continue;
                        }
                        command.aggregateCommand(ccm.makeCreateCommand(constraint, options));
                    }
                }
            } catch (DBException e) {
                // Ignore check constraints
                log.debug(e);
            }
        }
        if (fkm != null && !CommonUtils.getOption(options, DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS)) {
            try {
                for (DBSEntityAssociation foreignKey : CommonUtils.safeCollection(table.getAssociations(monitor))) {
                    if (!(foreignKey instanceof DBSTableForeignKey) || skipObject(foreignKey)) {
                        continue;
                    }
                    command.aggregateCommand(fkm.makeCreateCommand((DBSTableForeignKey) foreignKey, options));
                }
            } catch (DBException e) {
                // Ignore foreign keys
                log.debug(e);
            }
        }
        if (im != null && table instanceof DBSTable && !CommonUtils.getOption(options, DBPScriptObject.OPTION_SKIP_INDEXES)) {
            try {
                for (DBSTableIndex index : CommonUtils.safeCollection(((DBSTable)table).getIndexes(monitor))) {
                    if (!isIncludeIndexInDDL(monitor, index)) {
                        continue;
                    }
                    command.aggregateCommand(im.makeCreateCommand(index, options));
                }
            } catch (DBException e) {
                // Ignore indexes
                log.debug(e);
            }
        }
        addExtraDDLCommands(monitor, table, options, command);
        Collections.addAll(actions, command.getPersistActions(monitor, executionContext, options));

        return actions.toArray(new DBEPersistAction[0]);
    }

    private boolean skipObject(Object object) {
        return DBUtils.isHiddenObject(object) || DBUtils.isInheritedObject(object);
    }

    protected void addExtraDDLCommands(DBRProgressMonitor monitor, OBJECT_TYPE table, Map<String, Object> options, StructCreateCommand createCommand) {

    }

    protected boolean isIncludeConstraintInDDL(DBRProgressMonitor monitor, DBSEntityConstraint constraint) {
        return !skipObject(constraint);
    }

    protected boolean isIncludeIndexInDDL(DBRProgressMonitor monitor, DBSTableIndex index) throws DBException {
        return !DBUtils.isHiddenObject(index) && !DBUtils.isInheritedObject(index);
    }

    protected boolean isIncludeDropInDDL(@NotNull OBJECT_TYPE table) {
        return true;
    }

}

