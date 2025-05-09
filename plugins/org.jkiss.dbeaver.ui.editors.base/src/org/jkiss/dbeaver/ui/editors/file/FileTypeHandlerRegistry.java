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
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.*;

public class FileTypeHandlerRegistry {

    private static FileTypeHandlerRegistry instance = null;

    public synchronized static FileTypeHandlerRegistry getInstance() {
        if (instance == null) {
            instance = new FileTypeHandlerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<FileTypeHandlerDescriptor> handlers = new ArrayList<>();
    private final Map<String, FileTypeHandlerDescriptor> handlerByExtension = new HashMap<>();

    private FileTypeHandlerRegistry(IExtensionRegistry registry) {
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(FileTypeHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                FileTypeHandlerDescriptor formatterDescriptor = new FileTypeHandlerDescriptor(ext);
                handlers.add(formatterDescriptor);
                for (String fileExt : formatterDescriptor.getExtensions()) {
                    handlerByExtension.put(fileExt, formatterDescriptor);
                }
            }
            handlers.sort(Comparator.comparingInt(FileTypeHandlerDescriptor::getOrder));
        }
    }

    @NotNull
    public List<FileTypeHandlerDescriptor> getHandlers() {
        return handlers;
    }

    @Nullable
    public FileTypeHandlerDescriptor findHandler(String fileExtension) {
        return handlerByExtension.get(fileExtension);
    }

}
