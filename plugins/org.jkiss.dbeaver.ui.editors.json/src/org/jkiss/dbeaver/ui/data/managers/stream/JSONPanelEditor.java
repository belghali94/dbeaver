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
package org.jkiss.dbeaver.ui.data.managers.stream;

import com.google.gson.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.AbstractTextPanelEditor;
import org.jkiss.dbeaver.ui.editors.json.JSONFormattingStrategy;
import org.jkiss.dbeaver.ui.editors.json.JSONTextEditor;

/**
* JSONPanelEditor
*/
public class JSONPanelEditor extends AbstractTextPanelEditor<JSONTextEditor> {

    @Override
    protected JSONTextEditor createEditorParty(IValueController valueController) {
        // Override init function because standard is VEEERY slow
        return new JSONTextEditor() {
            @Override
            public void init(IEditorSite site, IEditorInput input) throws PartInitException {
                setSite(site);
                try {
                    doSetInput(input);
                } catch (CoreException e) {
                    throw new PartInitException("Error initializing panel JSON editor", e);
                }
            }
        };
    }

    @Override
    protected String getFileFolderName() {
        return "dbeaver-json";
    }

    @Override
    protected String getFileExtension() {
        return ".json";
    }

    @Override
    public boolean supportMinify() {
        return true;
    }

    @Override
    public String minify(String value) {
        JsonElement jsonElement;
        try {
            jsonElement = JsonParser.parseString(value);
        } catch (JsonSyntaxException ex) {
            return value;
        }

        return JSONFormattingStrategy.GSON_UNFORMATTED.toJson(jsonElement);
    }
}
