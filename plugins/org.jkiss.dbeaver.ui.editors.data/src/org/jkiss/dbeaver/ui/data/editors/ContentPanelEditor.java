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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.data.StringContent;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerSwitchContentViewer;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.registry.StreamValueManagerDescriptor;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

/**
* ControlPanelEditor
*/
public class ContentPanelEditor extends BaseValueEditor<Control> implements IAdaptable {

    private static final Log log = Log.getLog(ContentPanelEditor.class);

    private static final String PROP_VALUE_MANAGER = "valueManager";

    private static Map<String, String> valueToManagerMap = new HashMap<>();

    private Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> streamManagers;
    private volatile StreamValueManagerDescriptor curStreamManager;
    private IStreamValueEditor<Control> streamEditor;
    private Control editorControl;

    private IContributionManager toolbarManager;

    public ContentPanelEditor(IValueController controller) {
        super(controller);

        // Load manager setting for current attribute
        if (controller.getExecutionContext() != null) {
            final DBPPreferenceStore store = controller.getExecutionContext().getDataSource().getContainer().getPreferenceStore();
            final String managerId = store.getString(PROP_VALUE_MANAGER + '.' + makeValueId(false));
            if (CommonUtils.isNotEmpty(managerId)) {
                valueToManagerMap.put(makeValueId(true), managerId);
            }
        }
    }

    @Override
    public void dispose() {
        if (streamEditor != null) {
            streamEditor.disposeEditor();
            streamEditor = null;
        }
        super.dispose();
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {
        manager.add(new ContentTypeSwitchAction());
        if (streamEditor != null) {
            streamEditor.contributeActions(manager, editorControl);
        } else {
            toolbarManager = manager;
        }
    }

    @Override
    public void primeEditorValue(@Nullable final Object value) throws DBException
    {
        primeEditorValue(value, true);
    }

    protected void primeEditorValue(@Nullable final Object value, boolean loadInService) throws DBException
    {
        final Object content = valueController.getValue();
        if (streamEditor == null) {
            // Editor not yet initialized
            return;
        }
        if (content instanceof DBDContent) {
            streamManagers = ValueManagerRegistry.getInstance().getApplicableStreamManagers(
                new VoidProgressMonitor(),
                valueController.getValueType(),
                ((DBDContent) content)
            );
            // Check if existing manager is valid for the current value
            // If not, update current stream manager
            if (streamManagers != null && !streamManagers.containsKey(curStreamManager)) {
                if (curStreamManager != null) {
                    if (streamEditor != null) {
                        streamEditor.disposeEditor();
                        streamEditor = null;
                    }
                    if (editorControl != null) {
                        editorControl.dispose();
                        editorControl = null;
                    }
                    curStreamManager = null;
                    control.dispose();
                }
                control = createControl(valueController.getEditPlaceholder());
                valueController.getEditPlaceholder().layout(true, true);
            }
        }
        if (isStringValue()) {
            // It is a string
            streamEditor.primeEditorValue(
                new VoidProgressMonitor(),
                control,
                new StringContent(
                    valueController.getExecutionContext(), CommonUtils.toString(content)));
        } else if (content instanceof DBDContent) {
            loadInService = !(content instanceof DBDContentCached);
            if (loadInService) {
                StreamValueLoadService loadingService = new StreamValueLoadService((DBDContent) content);

                Composite ph = control instanceof Composite ? (Composite) control : valueController.getEditPlaceholder();
                LoadingJob.createService(
                    loadingService,
                    new StreamValueLoadVisualizer(loadingService, ph))
                    .schedule();
            } else {
                streamEditor.primeEditorValue(new VoidProgressMonitor(), control, (DBDContent) content);
            }

        } else if (content == null) {
            valueController.showMessage("NULL content value. Must be DBDContent.", DBPMessageType.ERROR);
        } else {
            valueController.showMessage("Unsupported content value. Must be DBDContent or String.", DBPMessageType.ERROR);
        }
    }

    private boolean isStringValue() {
        return !(valueController.getValue() instanceof DBDContent);
    }

    @Override
    public Object extractEditorValue() throws DBException
    {
        final Object content = valueController.getValue();
        if (isStringValue()) {
            StringContent stringContent = new StringContent(
                valueController.getExecutionContext(), null);
            streamEditor.extractEditorValue(new VoidProgressMonitor(), control, stringContent);
            return stringContent.getRawValue();
        } else {
            if (content == null) {
                log.warn("NULL content value. Must be DBDContent.");
            } else if (streamEditor == null) {
                log.warn("NULL content editor.");
            } else {
                try {
                    streamEditor.extractEditorValue(new VoidProgressMonitor(), control, (DBDContent) content);
                } catch (Throwable e) {
                    log.debug(e);
                    valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
                }
            }
            return content;
        }
    }

    @Override
    protected Control createControl(Composite editPlaceholder)
    {
        final Object content = valueController.getValue();

        if (curStreamManager == null) {
            if (isStringValue()) {
                try {
                    loadStringStreamManagers();
                } catch (Throwable e) {
                    DBWorkbench.getPlatformUI().showError("No string editor", "Can't load string content managers", e);
                }
            } else {
                try {
                    detectStreamManager(new VoidProgressMonitor(), (DBDContent) content);
                } catch (DBException e) {
                    log.error(e);
                    valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
                    return editPlaceholder;
                }
            }
        }
        return createStreamManagerControl(editPlaceholder);
    }

    private Control createStreamManagerControl(Composite editPlaceholder) {
        if (curStreamManager != null) {
            try {
                streamEditor = curStreamManager.getInstance().createPanelEditor(valueController);
            } catch (Throwable e) {
                DBWorkbench.getPlatformUI().showError("No stream editor", "Can't create stream editor", e);
            }
        }
        if (streamEditor == null) {
            return UIUtils.createInfoLabel(editPlaceholder, "No Editor");
        }

        editorControl = streamEditor.createControl(valueController);

        if (toolbarManager != null) {
            if (toolbarManager instanceof ToolBarManager) {
                ((ToolBarManager) toolbarManager).getControl().setRedraw(false);
            }
            // Lazy toolbar initialization
            try {
                streamEditor.contributeActions(toolbarManager, editorControl);
                toolbarManager.update(true);
            } catch (Exception e) {
                log.error(e);
            } finally {
                if (toolbarManager instanceof ToolBarManager) {
                    ((ToolBarManager) toolbarManager).getControl().setRedraw(true);
                }
                toolbarManager = null;
            }
        }

        return editorControl;
    }

    private void loadStringStreamManagers() throws DBException {
        streamManagers = ValueManagerRegistry.getInstance().getStreamManagersByMimeType(MimeTypes.TEXT, MimeTypes.TEXT_PLAIN);
        String savedManagerId = valueToManagerMap.get(makeValueId(true));
        detectCurrentStreamManager(savedManagerId);
    }

    private void detectCurrentStreamManager(String savedManagerId) throws DBException {
        if (savedManagerId != null) {
            curStreamManager = findManager(savedManagerId);
        }
        if (curStreamManager == null) {
            curStreamManager = findManager(IStreamValueManager.MatchType.EXCLUSIVE);
            if (curStreamManager == null)
                curStreamManager = findManager(IStreamValueManager.MatchType.PRIMARY);
            if (curStreamManager == null)
                curStreamManager = findManager(IStreamValueManager.MatchType.DEFAULT);
            if (curStreamManager == null)
                curStreamManager = findManager(IStreamValueManager.MatchType.APPLIES);
            if (curStreamManager == null) {
                throw new DBException("Can't find appropriate stream manager");
            }
        }
    }

    private void runSreamManagerDetector(final DBDContent content, Composite editPlaceholder) {
        StreamManagerDetectService loadingService = new StreamManagerDetectService(content);

        LoadingJob.createService(
            loadingService,
            new StreamManagerDetectVisualizer(loadingService, editPlaceholder))
            .schedule();
    }

    @Nullable
    public StreamValueManagerDescriptor getCurrentStreamManager() {
        return curStreamManager;
    }

    @Nullable
    public IStreamValueEditor<Control> getStreamEditor() {
        return streamEditor;
    }

    public void setCurrentStreamManager(@NotNull StreamValueManagerDescriptor newManager) {
        if (curStreamManager == newManager) {
            return;
        }

        curStreamManager = newManager;

        if (curStreamManager != null) {
            // Save manager setting for current attribute
            final String valueId = makeValueId(true);
            final String managerId = curStreamManager.getId();

            if (valueController.getExecutionContext() != null) {
                final DBPPreferenceStore store = valueController.getExecutionContext().getDataSource().getContainer().getPreferenceStore();
                store.setValue(PROP_VALUE_MANAGER + '.' + makeValueId(false), managerId);
                PrefUtils.savePreferenceStore(store);
            }

            valueToManagerMap.put(valueId, managerId);
            valueController.refreshEditor();
        }
    }

    private String makeValueId(boolean includeDataSource) {
        String valueId;
        DBSTypedObject valueType = valueController.getValueType();
        if (valueType instanceof DBDAttributeBinding) {
            valueType = ((DBDAttributeBinding) valueType).getAttribute();
        }
        if (valueType instanceof DBSObject) {
            DBSObject object = (DBSObject) valueType;
            valueId = DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
            if (object.getParentObject() != null) {
                valueId = DBUtils.getObjectFullName(object.getParentObject(), DBPEvaluationContext.DDL) + ":" + valueId;
            }

        } else {
            valueId = valueController.getValueName();
        }
        if (includeDataSource) {
            String dsId = "unknown";
            if (valueController.getExecutionContext() != null) {
                dsId = valueController.getExecutionContext().getDataSource().getContainer().getId();
            }
            return dsId + ":" + valueId;
        }
        return valueId;
    }

    private StreamValueManagerDescriptor findManager(String id) {
        for (Map.Entry<StreamValueManagerDescriptor, IStreamValueManager.MatchType> entry : streamManagers.entrySet()) {
            if (entry.getKey().getId().equals(id)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private StreamValueManagerDescriptor findManager(IStreamValueManager.MatchType matchType) {
        for (Map.Entry<StreamValueManagerDescriptor, IStreamValueManager.MatchType> entry : streamManagers.entrySet()) {
            if (entry.getValue() == matchType) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (streamEditor != null) {
            if (adapter.isAssignableFrom(streamEditor.getClass())) {
                return adapter.cast(streamEditor);
            }
            if (streamEditor instanceof IAdaptable) {
                return ((IAdaptable) streamEditor).getAdapter(adapter);
            }
        }
        return null;
    }

    @NotNull
    @Override
    protected Font getDefaultFont() {
        return BaseThemeSettings.instance.monospaceFont;
    }

    private void detectStreamManager(DBRProgressMonitor monitor, DBDContent content) throws DBException {
        streamManagers = ValueManagerRegistry.getInstance().getApplicableStreamManagers(monitor, valueController.getValueType(), content);
        String savedManagerId = valueToManagerMap.get(makeValueId(true));
        detectCurrentStreamManager(savedManagerId);
    }

    private class ContentTypeSwitchAction extends Action implements SelectionListener {
        private Menu menu;

        ContentTypeSwitchAction() {
            super(null, Action.AS_DROP_DOWN_MENU);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.PAGES));
            setToolTipText("Content viewer settings");
        }

        @Override
        public void runWithEvent(Event event)
        {
            if (event.widget instanceof ToolItem) {
                ToolItem toolItem = (ToolItem) event.widget;
                Menu menu = createMenu(toolItem);
                Rectangle bounds = toolItem.getBounds();
                Point point = toolItem.getParent().toDisplay(bounds.x, bounds.y + bounds.height);
                menu.setLocation(point.x, point.y);
                menu.setVisible(true);
            }
        }

        private Menu createMenu(ToolItem toolItem) {
            if (menu != null) {
                menu.dispose();
            }
            {
                MenuManager menuManager = new MenuManager();
                ToolBar toolBar = toolItem.getParent();
                menu = new Menu(toolBar);
                List<StreamValueManagerDescriptor> managers = new ArrayList<>(streamManagers.keySet());
                managers.sort(Comparator.comparing(StreamValueManagerDescriptor::getLabel));
                for (StreamValueManagerDescriptor manager : managers) {
                    final CommandContributionItemParameter parameters = new CommandContributionItemParameter(
                        valueController.getValueSite(),
                        manager.getId(),
                        ResultSetHandlerSwitchContentViewer.COMMAND_ID,
                        CommandContributionItem.STYLE_RADIO
                    );
                    parameters.parameters = Map.of(
                        ResultSetHandlerSwitchContentViewer.PARAM_STREAM_MANAGER_ID,
                        manager.getId()
                    );
                    menuManager.add(new CommandContributionItem(parameters));
                }
                try {
                    streamEditor.contributeSettings(menuManager, editorControl);
                } catch (DBCException e) {
                    log.error(e);
                }
                for (IContributionItem item : menuManager.getItems()) {
                    item.fill(menu, -1);
                }
                toolBar.addDisposeListener(e -> menu.dispose());
            }
            for (MenuItem item : menu.getItems()) {
                if (item.getData() instanceof StreamValueManagerDescriptor) {
                    item.setSelection(item.getData() == curStreamManager);
                }
            }
            return menu;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            for (MenuItem item : menu.getItems()) {
                if (item.getSelection()) {
                    Object itemData = item.getData();
                    if (itemData instanceof StreamValueManagerDescriptor) {
                        StreamValueManagerDescriptor newManager = (StreamValueManagerDescriptor) itemData;
                        if (newManager != curStreamManager) {
                            setCurrentStreamManager(newManager);
                        }
                    }
                }
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {

        }
    }

    abstract class ContentLoaderService extends AbstractLoadService<DBDContent> {

        protected DBDContent content;

        protected ContentLoaderService(DBDContent content) {
            super("Load LOB value");
            this.content = content;
        }

        @Override
        public Object getFamily() {
            return valueController.getExecutionContext();
        }
    }

    private class ContentLoadVisualizer extends ProgressLoaderVisualizer<DBDContent> {
        protected Composite editPlaceholder;
        public ContentLoadVisualizer(ContentLoaderService loadingService, Composite parent) {
            super(loadingService, parent);
            this.editPlaceholder = parent;
        }

        @Override
        public void completeLoading(DBDContent result) {
            super.completeLoading(result);
            super.visualizeLoading();
        }
    }

    private class StreamManagerDetectService extends ContentLoaderService {

        public StreamManagerDetectService(DBDContent content) {
            super(content);
        }

        @Override
        public DBDContent evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            monitor.beginTask("Detect appropriate editor", 1);
            try {
                monitor.subTask("Load LOB value");
                detectStreamManager(monitor, content);
            } catch (Exception e) {
                valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
            } finally {
                monitor.done();
            }
            return content;
        }

    }


    private class StreamManagerDetectVisualizer extends ContentLoadVisualizer {
        public StreamManagerDetectVisualizer(StreamManagerDetectService loadingService, Composite parent) {
            super(loadingService, parent);
        }

        @Override
        public void completeLoading(DBDContent result) {
            super.completeLoading(result);
            // Clear placeholder
            UIUtils.disposeChildControls(editPlaceholder);
            // Create and layout new editor
            Control editorControl = createStreamManagerControl(this.editPlaceholder);
            this.editPlaceholder.layout(true);
            setControl(editorControl);
            try {
                primeEditorValue(result, false);
            } catch (Exception e) {
                valueController.showMessage(CommonUtils.notEmpty(e.getMessage()), DBPMessageType.ERROR);
            }
        }
    }


    private class StreamValueLoadService extends ContentLoaderService {

        public StreamValueLoadService(DBDContent content) {
            super(content);
        }

        @Override
        public DBDContent evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            monitor.beginTask("Detect appropriate editor", 1);
            try {
                monitor.subTask("Prime LOB value");
                UIUtils.syncExec(() -> {
                    try {
                        if (streamEditor != null && !control.isDisposed()) {
                            streamEditor.primeEditorValue(monitor, control, content);
                        }
                    } catch (Exception e) {
                        valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
                        DBWorkbench.getPlatformUI().showError("Value panel", "Error loading contents", e);
                    }
                });
            } finally {
                monitor.done();
            }
            return content;
        }
    }


    private class StreamValueLoadVisualizer extends ContentLoadVisualizer {
        public StreamValueLoadVisualizer(StreamValueLoadService loadingService, Composite parent) {
            super(loadingService, parent);
        }

        @Override
        public void completeLoading(DBDContent result) {
            super.completeLoading(result);
        }
    }

}
