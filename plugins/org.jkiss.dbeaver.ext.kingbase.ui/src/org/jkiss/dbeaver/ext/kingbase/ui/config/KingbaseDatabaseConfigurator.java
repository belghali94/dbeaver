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

package org.jkiss.dbeaver.ext.kingbase.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDatabase;
import org.jkiss.dbeaver.ext.kingbase.ui.KingbaseCreateDatabaseDialog;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;


public class KingbaseDatabaseConfigurator implements DBEObjectConfigurator<KingbaseDatabase> {

    @Override
    public KingbaseDatabase configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
        @Nullable Object container, @NotNull KingbaseDatabase database, @NotNull Map<String, Object> options) {
        return new UITask<KingbaseDatabase>() {
            @Override
            protected KingbaseDatabase runTask() throws DBException {
                KingbaseCreateDatabaseDialog dialog = new KingbaseCreateDatabaseDialog(UIUtils.getActiveWorkbenchShell(),
                    database.getDataSource());
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                database.setName(dialog.getName());
                database.setInitialOwner(dialog.getOwner());
                database.setTemplateName(dialog.getTemplateName());
                database.setInitialTablespace(dialog.getTablespace());
                database.setInitialEncoding(dialog.getEncoding());
                database.setDatabaseCompatibleMode(dialog.getCompatibleMode());
                return database;
            }
        }.execute();
    }
}
