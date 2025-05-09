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

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Document.
 * Document is a set of hierarchically organized nodes - similarly to JSON.
 * Each node can be a map, a list (collection) or a value (anything else).
 * Map key is always a string, value is a node.
 * List item is a value.
 */
public interface DBDDocument extends DBDValue {

    String PROP_ID = "id";
    String PROP_TITLE = "title";
    String PROP_CREATE_TIME = "createTime";
    String PROP_ID_ATTRIBUTE_NAME = "idAttributeName";

    @Nullable
    Object getDocumentId();

    /**
     * Document property
     * @param name    property name
     * @return property value
     */
    @Nullable
    Object getDocumentProperty(String name);

    /**
     * Document content type (mime type).
     * @return content type
     */
    @NotNull
    String getDocumentContentType();

    /**
     * Root node of document
     * @return root node
     */
    @NotNull
    Object getRootNode();

    /**
     * Serializes document into stream
     *
     * @param monitor progress monitor
     * @param writer  writer to serialize document into
     * @throws DBException on DB error
     * @throws IOException on IO error
     */
    void serializeDocument(@NotNull DBRProgressMonitor monitor, @NotNull Writer writer) throws IOException, DBException;

    /**
     * Updates document from stream
     *
     * @param monitor progress monitor
     * @param reader  reader to read document from
     * @throws DBException on DB error
     * @throws IOException on IO error
     */
    void updateDocument(@NotNull DBRProgressMonitor monitor, @NotNull Reader reader) throws IOException, DBException;
}