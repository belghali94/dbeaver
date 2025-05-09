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
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDatabase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSchema;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreSchemaManager;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;


/**
 * KingbaseSchemaManager
 */
public class KingbaseSchemaManager extends PostgreSchemaManager {

    @Override
    protected KingbaseSchema createDatabaseObject(@NotNull DBRProgressMonitor monitor,
            @NotNull DBECommandContext context,
            final Object container,
            Object copyFrom,
            @NotNull Map<String, Object> options) {
        KingbaseDatabase database = (KingbaseDatabase) container;
        return database.createSchemaImpl(database, "NewSchema", (PostgreRole) null);
    }
}
