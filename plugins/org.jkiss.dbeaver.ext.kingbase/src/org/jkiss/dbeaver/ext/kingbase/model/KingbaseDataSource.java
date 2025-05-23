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

package org.jkiss.dbeaver.ext.kingbase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreCharset;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTablespace;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

public class KingbaseDataSource extends PostgreDataSource {

    private PostgreServerExtension serverExtension;
    
    public KingbaseDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        super(monitor, container, new KingbaseDialect());
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
    }

    @NotNull
    @Override
    public KingbaseDatabase createDatabaseImpl(@NotNull DBRProgressMonitor monitor, ResultSet dbResult) throws DBException {
        return new KingbaseDatabase(monitor, this, dbResult);
    }

    @NotNull
    @Override
    public KingbaseDatabase createDatabaseImpl(@NotNull DBRProgressMonitor monitor, String name) throws DBException {
        return new KingbaseDatabase(monitor, this, name);
    }

    @NotNull
    @Override
    public KingbaseDatabase createDatabaseImpl(DBRProgressMonitor monitor, String name, PostgreRole owner, String templateName,
        PostgreTablespace tablespace, PostgreCharset encoding) throws DBException {
        return new KingbaseDatabase(monitor, this, name, owner, templateName, tablespace, encoding);
    }

    // True if we need multiple databases
    @Override
    protected boolean isReadDatabaseList(DBPConnectionConfiguration configuration) {
        // It is configurable by default
        return configuration.getConfigurationType() != DBPDriverConfigurationType.URL
            && CommonUtils.getBoolean(configuration.getProviderProperty(PostgreConstants.PROP_SHOW_NON_DEFAULT_DB), true);
    }
    
    @Override
    public PostgreServerExtension getServerType() {
        if (serverExtension == null) {
            serverExtension = new PostgreServerKingbase(this);
        }
        return serverExtension;
    }
}
