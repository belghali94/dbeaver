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
package org.jkiss.dbeaver.ui.controls.resultset.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.ApplicationPolicyProvider;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Open results in external application
 */
public class ResultSetHandlerOpenWith extends AbstractHandler implements IElementUpdater {

    private static final Log log = Log.getLog(ResultSetHandlerOpenWith.class);

    public static final String CMD_OPEN_WITH = "org.jkiss.dbeaver.core.resultset.openWith";
    public static final String PARAM_PROCESSOR_ID = "processorId";

    public static final String PARAM_ACTIVE_APP = "org.jkiss.dbeaver.core.resultset.openWith.currentApp";
    public static final String PREF_OPEN_WITH_DEFAULT_PROCESSOR = "org.jkiss.dbeaver.core.resultset.openWith.defaultprocessor";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (ApplicationPolicyProvider.getInstance().isPolicyEnabled(ApplicationPolicyProvider.POLICY_DATA_EXPORT)) {
            UIUtils.showMessageBox(HandlerUtil.getActiveShell(event),
                UIMessages.dialog_policy_data_export_title,
                UIMessages.dialog_policy_data_export_msg,
                SWT.ICON_WARNING
            );
            return null;
        }

        IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        DataTransferProcessorDescriptor processor = getActiveProcessor(event.getParameter(PARAM_PROCESSOR_ID));

        if (processor == null) {
            return null;
        }
        switch (event.getCommand().getId()) {
            case CMD_OPEN_WITH:
                openResultsWith(resultSet, processor);
                break;
        }
        return null;
    }

    static DataTransferProcessorDescriptor getActiveProcessor(String processorId) {
        if (CommonUtils.isEmpty(processorId)) {
            processorId = DBWorkbench.getPlatform().getPreferenceStore().getString(PARAM_ACTIVE_APP);
        }
        if (CommonUtils.isEmpty(processorId)) {
            DataTransferProcessorDescriptor defaultAppProcessor = getDefaultProcessor();
            if (defaultAppProcessor != null) {
                return defaultAppProcessor;
            }
        } else {
            return DataTransferRegistry.getInstance().getProcessor(processorId);
        }
        return null;
    }

    static DataTransferProcessorDescriptor getDefaultProcessor() {
        DataTransferProcessorDescriptor defaultAppProcessor = getDefaultAppProcessor();
        if (defaultAppProcessor != null) {
            return defaultAppProcessor;
        }
        return null;
    }

    private static void openResultsWith(IResultSetController resultSet, DataTransferProcessorDescriptor processor) {

        ResultSetDataContainerOptions options = new ResultSetDataContainerOptions();

        IResultSetSelection rsSelection = resultSet.getSelection();
        List<ResultSetRow> rsSelectedRows = rsSelection.getSelectedRows();
        List<DBDAttributeBinding> rsSelectedAttributes = rsSelection.getSelectedAttributes();
        if (rsSelectedRows.size() > 1 || rsSelectedAttributes.size() > 1) {
            List<Integer> selectedRows = new ArrayList<>();
            for (ResultSetRow selectedRow : rsSelectedRows) {
                selectedRows.add(selectedRow.getRowNumber());
            }

            options.setSelectedRows(selectedRows);
            options.setSelectedColumns(rsSelectedAttributes);
        }
        ResultSetDataContainer dataContainer = new ResultSetDataContainer(resultSet, options);
        if (dataContainer.getDataSource() == null) {
            DBWorkbench.getPlatformUI().showError("Open " + processor.getAppName(), ModelMessages.error_not_connected_to_database);
            return;
        }

        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        String prevActiveApp = preferenceStore.getString(PARAM_ACTIVE_APP);
        if (!CommonUtils.equalObjects(prevActiveApp, processor.getFullId())) {
            //preferenceStore.setValue(PARAM_ACTIVE_APP, processor.getFullId());
            //resultSet.updateEditControls();
            //resultSet.getControl().layout(true);
        }

        AbstractJob exportJob = new AbstractJob("Open " + processor.getAppName()) {

            {
                setUser(true);
                setSystem(false);
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    Path tempDir = DBWorkbench.getPlatform().getTempFolder(monitor, "data-files");
                    Path tempFile = tempDir.resolve(new SimpleDateFormat(
                        "yyyyMMdd-HHmmss").format(System.currentTimeMillis()) + "." + processor.getAppFileExtension());
                    tempFile.toFile().deleteOnExit();

                    IDataTransferProcessor processorInstance = processor.getInstance();
                    if (!(processorInstance instanceof IStreamDataExporter)) {
                        return Status.CANCEL_STATUS;
                    }
                    IStreamDataExporter exporter = (IStreamDataExporter) processorInstance;

                    StreamTransferConsumer consumer = new StreamTransferConsumer();
                    StreamConsumerSettings settings = new StreamConsumerSettings();

                    settings.setOutputEncodingBOM(false);
                    settings.setOutputFolder(tempDir.toAbsolutePath().toString());
                    settings.setOutputFilePattern(tempFile.getFileName().toString());

                    Map<String, Object> properties = new HashMap<>();
                    // Default values from wizard
                    IDialogSettings dtSettings = DataTransferWizard.getWizardDialogSettings();
                    IDialogSettings procListSection = dtSettings.getSection("processors");
                    IDialogSettings procSettings = null;
                    if (procListSection != null) {
                        procSettings = procListSection.getSection("stream_consumer:" + processor.getId());
                    }

                    for (DBPPropertyDescriptor prop : processor.getProperties()) {
                        Object defValue = procSettings == null ? null : procSettings.get(CommonUtils.toString(prop.getId()));
                        properties.put(prop.getId(), defValue != null ? defValue : prop.getDefaultValue());
                    }
                    // Remove extension property (we specify file name directly)
                    properties.remove(StreamConsumerSettings.PROP_FILE_EXTENSION);

                    consumer.initTransfer(
                        dataContainer,
                        settings,
                        new IDataTransferConsumer.TransferParameters(processor.isBinaryFormat(), processor.isHTMLFormat()),
                        exporter,
                        properties,
                        null);

                    DBDDataFilter dataFilter = resultSet.getModel().getDataFilter();
                    DatabaseTransferProducer producer = new DatabaseTransferProducer(dataContainer, dataFilter);
                    DatabaseProducerSettings producerSettings = new DatabaseProducerSettings();
                    producerSettings.setExtractType(DatabaseProducerSettings.ExtractType.SINGLE_QUERY);
                    producerSettings.setQueryRowCount(false);
                    // disable OpenNewconnection by default (#6432)
                    producerSettings.setOpenNewConnections(false);
                    producerSettings.setSelectedRowsOnly(!CommonUtils.isEmpty(options.getSelectedRows()));
                    producerSettings.setSelectedColumnsOnly(!CommonUtils.isEmpty(options.getSelectedColumns()));

                    producer.transferData(monitor, consumer, null, producerSettings, null);

                    consumer.finishTransfer(monitor, false);

                    UIUtils.asyncExec(() -> {
                        if (!ShellUtils.launchProgram(tempFile.toAbsolutePath().toString())) {
                            DBWorkbench.getPlatformUI().showError(
                                "Open " + processor.getAppName(),
                                "Can't open " + processor.getAppFileExtension() + " file '" + tempFile.toAbsolutePath() + "'");
                        }
                    });
                } catch (Exception e) {
                    DBWorkbench.getPlatformUI().showError("Error opening in " + processor.getAppName(), null, e);
                }
                return Status.OK_STATUS;
            }
        };
        exportJob.schedule();
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        // Put processor name in command label
        DataTransferProcessorDescriptor processor = getActiveProcessor((String) parameters.get(PARAM_PROCESSOR_ID));
        if (processor != null) {
            element.setText(processor.getAppName());
            if (!CommonUtils.isEmpty(processor.getDescription())) {
                element.setTooltip(processor.getDescription());
            }
            if (processor.getIcon() != null) {
                element.setIcon(DBeaverIcons.getImageDescriptor(processor.getIcon()));
            }
        }
    }

    private static DataTransferProcessorDescriptor getDefaultAppProcessor() {
        List<DataTransferProcessorDescriptor> processors = new ArrayList<>();
        for (final DataTransferNodeDescriptor consumerNode : DataTransferRegistry.getInstance().getNodes(DataTransferNodeDescriptor.NodeType.CONSUMER)) {
            for (DataTransferProcessorDescriptor processor : consumerNode.getProcessors()) {
                if (processor.getAppFileExtension() != null) {
                    processors.add(processor);
                }
            }
        }
        processors.sort(Comparator.comparingInt(DataTransferProcessorDescriptor::getOrder));
        return processors.isEmpty() ? null : processors.get(0);
    }

    public static class OpenWithParameterValues implements IParameterValues {

        @Override
        public Map<String,String> getParameterValues() {
            final Map<String,String> values = new HashMap<>();

            for (final DataTransferNodeDescriptor consumerNode : DataTransferRegistry.getInstance().getNodes(DataTransferNodeDescriptor.NodeType.CONSUMER)) {
                for (DataTransferProcessorDescriptor processor : consumerNode.getProcessors()) {
                    if (processor.getAppFileExtension() != null) {
                        values.put(processor.getAppName(), processor.getFullId());
                    }
                }
            }

            return values;
        }

    }

    public static class OpenWithMenuContributor extends CompoundContributionItem
    {
        @Override
        protected IContributionItem[] getContributionItems() {
            final ResultSetViewer rsv = (ResultSetViewer) ResultSetHandlerMain.getActiveResultSet(
                UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart());
            if (rsv == null) {
                return new IContributionItem[0];
            }
            ContributionManager menu = new MenuManager();
            fillOpenWithMenu(rsv, menu);
            return menu.getItems();
        }
    }

    public static class OpenWithMenuDefaultContributor extends CompoundContributionItem
    {

        @Override
        protected IContributionItem[] getContributionItems() {
            final ResultSetViewer rsv = (ResultSetViewer) ResultSetHandlerMain.getActiveResultSet(
                UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart());
            if (rsv == null) {
                return new IContributionItem[0];
            }
            ContributionManager menu = new MenuManager();
            // Def processor is null
            if (!ApplicationPolicyProvider.getInstance().isPolicyEnabled(ApplicationPolicyProvider.POLICY_DATA_EXPORT)) {
                menu.add(new Action(ActionUtils.findCommandDescription(
                    ResultSetHandlerMain.CMD_EXPORT, rsv.getSite(), false),
                    Action.AS_RADIO_BUTTON) {
                    {
                        setChecked(CommonUtils.isEmpty(getDefaultOpenWithProcessor()));
                    }

                    @Override
                    public void run() {
                        DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_OPEN_WITH_DEFAULT_PROCESSOR, "");
                        updateResultSetToolbar(rsv);
                    }
                });
            }
            for (DataTransferProcessorDescriptor processor : getDataFileTransferProcessors(rsv)) {
                Action setDefaultAction = new Action(processor.getAppName(), Action.AS_RADIO_BUTTON) {
                    {
                        //setImageDescriptor(DBeaverIcons.getImageDescriptor(processor.getIcon()));
                        if (!CommonUtils.isEmpty(processor.getDescription())) {
                            setToolTipText(processor.getDescription());
                        }
                        setChecked(processor.getFullId().equals(getDefaultOpenWithProcessor()));
                    }

                    @Override
                    public void run() {
                        DBWorkbench.getPlatform().getPreferenceStore().setValue(
                            PREF_OPEN_WITH_DEFAULT_PROCESSOR, processor.getFullId());
                        updateResultSetToolbar(rsv);
                    }
                };
                menu.add(setDefaultAction);
            }
            return menu.getItems();
        }
    }

    public static String getDefaultOpenWithProcessor() {
        return DBWorkbench.getPlatform().getPreferenceStore().getString(PREF_OPEN_WITH_DEFAULT_PROCESSOR);
    }

    public static void fillOpenWithMenu(ResultSetViewer viewer, IContributionManager openWithMenu) {
        for (DataTransferProcessorDescriptor processor : getDataFileTransferProcessors(viewer)) {
            CommandContributionItemParameter params = new CommandContributionItemParameter(
                viewer.getSite(),
                processor.getId(),
                ResultSetHandlerOpenWith.CMD_OPEN_WITH,
                CommandContributionItem.STYLE_RADIO
            );
            params.label = processor.getAppName();
            if (processor.getIcon() != null) {
                params.icon = DBeaverIcons.getImageDescriptor(processor.getIcon());
            }
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(ResultSetHandlerOpenWith.PARAM_PROCESSOR_ID, processor.getFullId());
            params.parameters = parameters;
            openWithMenu.add(new CommandContributionItem(params));
        }
    }

    @NotNull
    private static List<DataTransferProcessorDescriptor> getDataFileTransferProcessors(ResultSetViewer viewer) {
        ResultSetDataContainerOptions options = new ResultSetDataContainerOptions();
        ResultSetDataContainer dataContainer = new ResultSetDataContainer(viewer, options);

        List<DataTransferProcessorDescriptor> appProcessors = new ArrayList<>();

        for (final DataTransferNodeDescriptor consumerNode : DataTransferRegistry.getInstance().getAvailableConsumers(Collections.singleton(dataContainer))) {
            for (DataTransferProcessorDescriptor processor : consumerNode.getProcessors()) {
                if (processor.getAppFileExtension() != null) {
                    appProcessors.add(processor);
                }
            }
        }

        appProcessors.sort(Comparator.comparingInt(DataTransferProcessorDescriptor::getOrder));
        return appProcessors;
    }

    private static void updateResultSetToolbar(@NotNull IResultSetController controller) {
        final ICommandService service = PlatformUI.getWorkbench().getService(ICommandService.class);

        if (service != null) {
            service.refreshElements(ResultSetHandlerMain.CMD_EXPORT, null);
            controller.updateToolbar();
        }
    }
}
