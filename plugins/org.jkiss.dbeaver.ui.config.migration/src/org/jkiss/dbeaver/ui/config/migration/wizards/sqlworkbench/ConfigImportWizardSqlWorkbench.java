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
package org.jkiss.dbeaver.ui.config.migration.wizards.sqlworkbench;

import org.jkiss.dbeaver.ui.config.migration.wizards.ConfigImportWizard;

import java.io.File;

public class ConfigImportWizardSqlWorkbench extends ConfigImportWizard {

    private ConfigImportWizardPageSqlWorkbenchSettings pageSettings;

    @Override
    protected ConfigImportWizardPageSqlWorkbenchConnections createMainPage() {
        return new ConfigImportWizardPageSqlWorkbenchConnections();
    }

    @Override
    public void addPages() {
        pageSettings = new ConfigImportWizardPageSqlWorkbenchSettings();
        addPage(pageSettings);
        super.addPages();
    }

    public File getInputFile() {
        return pageSettings.getInputFile();
    }

}