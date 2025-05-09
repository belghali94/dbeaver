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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class CubridSynonym extends GenericSynonym
{
    private String owner;
    private String targetName;
    private String targetOwner;
    private String description;

    public CubridSynonym(
            @NotNull GenericStructContainer container,
            @NotNull String name,
            @Nullable String description,
            @NotNull JDBCResultSet dbResult) {
        super(container, name, description);
        this.description = description;
        this.owner = JDBCUtils.safeGetString(dbResult, "synonym_owner_name");
        this.targetName = JDBCUtils.safeGetString(dbResult, "target_name");
        this.targetOwner = JDBCUtils.safeGetString(dbResult, "target_owner_name");
    }

    public CubridSynonym(
            @NotNull GenericStructContainer container,
            @NotNull String name) {
        super(container, name, null);
        this.owner = container.getName();
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public CubridUser getOwner() {
        return new CubridUser(getDataSource(), owner, null);
    }

    @Override
    public DBSObject getTargetObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getDataSource().findTable(monitor, null, targetOwner, targetName);
    }

    @Property(viewable = true, editable = true, updatable = true, order = 4)
    public String getTargetObject() {
        return targetName;
    }

    public void setTargetObject(String targetObject) {
        this.targetName = targetObject;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
