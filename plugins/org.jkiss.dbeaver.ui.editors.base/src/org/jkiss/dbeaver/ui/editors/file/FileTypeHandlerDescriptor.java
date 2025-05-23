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

package org.jkiss.dbeaver.ui.editors.file;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * DataFormatterDescriptor
 */
public class FileTypeHandlerDescriptor extends AbstractDescriptor {
    private static final Log log = Log.getLog(FileTypeHandlerDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.fileTypeHandler"; //$NON-NLS-1$

    private final String id;
    private final String[] extensions;
    private final ObjectType handlerType;
    private final int order;
    private boolean supportsRemote;

    public FileTypeHandlerDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.handlerType = new ObjectType(config.getAttribute("class"));
        this.extensions = CommonUtils.notEmpty(config.getAttribute("extensions")).split(",");
        this.supportsRemote = CommonUtils.toBoolean(config.getAttribute("remote"), true);
        this.order = CommonUtils.toInt(config.getAttribute("order"));
    }

    public String getId() {
        return id;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public boolean supportsRemoteFiles() {
        return supportsRemote;
    }

    public int getOrder() {
        return order;
    }

    public IFileTypeHandler createHandler() throws ReflectiveOperationException {
        Class<? extends IFileTypeHandler> clazz = handlerType.getObjectClass(IFileTypeHandler.class);
        if (clazz == null) {
            throw new NoClassDefFoundError(handlerType.getImplName());
        }
        return clazz.getConstructor().newInstance();
    }

}
