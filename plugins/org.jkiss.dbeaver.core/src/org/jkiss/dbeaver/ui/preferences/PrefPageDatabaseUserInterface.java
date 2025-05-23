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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DesktopPlatform;
import org.jkiss.dbeaver.core.ui.services.ApplicationPolicyService;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguage;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguageManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.registry.SWTBrowserRegistry;
import org.jkiss.dbeaver.registry.language.PlatformLanguageDescriptor;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.registry.timezone.TimezoneRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorPreferences;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorPreferences.BreadcrumbLocation;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * PrefPageDatabaseUserInterface
 */
public class PrefPageDatabaseUserInterface extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main"; //$NON-NLS-1$

    private Button automaticUpdateCheck;
    private Combo workspaceLanguage;

    @Nullable
    private Combo clientTimezone;

    private boolean isStandalone = DesktopPlatform.isStandalone();
    private Combo browserCombo;
    private Button useEmbeddedBrowserAuth;

    private Button statusBarShowBreadcrumbsCheck;
    private Button statusBarShowStatusCheck;
    private Combo statusBarBreadcrumbPositionCombo;

    public PrefPageDatabaseUserInterface()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        if (isStandalone && !ApplicationPolicyService.getInstance().isInstallUpdateDisabled()) {
            Group groupObjects = UIUtils.createControlGroup(
                composite, CoreMessages.pref_page_ui_general_group_general, 2,
                GridData.VERTICAL_ALIGN_BEGINNING,
                0);
            automaticUpdateCheck = UIUtils.createCheckbox(
                groupObjects,
                CoreMessages.pref_page_ui_general_checkbox_automatic_updates,
                null,
                false,
                2);
        }
        if (isStandalone) {
            Group regionalSettingsGroup = UIUtils.createControlGroup(composite,
                CoreMessages.pref_page_ui_general_group_regional,
                2,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING,
                0
            );
            workspaceLanguage = UIUtils.createLabelCombo(regionalSettingsGroup,
                CoreMessages.pref_page_ui_general_combo_language,
                CoreMessages.pref_page_ui_general_combo_language_tip,
                SWT.READ_ONLY | SWT.DROP_DOWN
            );
            workspaceLanguage.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            List<PlatformLanguageDescriptor> languages = PlatformLanguageRegistry.getInstance().getLanguages();
            DBPPlatformLanguage pLanguage = DBPPlatformDesktop.getInstance().getLanguage();
            for (int i = 0; i < languages.size(); i++) {
                PlatformLanguageDescriptor lang = languages.get(i);
                workspaceLanguage.add(lang.getLabel());
                if (CommonUtils.equalObjects(pLanguage, lang)) {
                    workspaceLanguage.select(i);
                }
            }
            if (workspaceLanguage.getSelectionIndex() < 0) {
                workspaceLanguage.select(0);
            }

            clientTimezone = UIUtils.createLabelCombo(regionalSettingsGroup,
                CoreMessages.pref_page_ui_general_combo_timezone,
                CoreMessages.pref_page_ui_general_combo_timezone_tip,
                SWT.DROP_DOWN
            );
            clientTimezone.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            clientTimezone.add(DBConstants.DEFAULT_TIMEZONE);
            for (String timezoneName : TimezoneRegistry.getTimezoneNames()) {
                clientTimezone.add(timezoneName);
            }
            clientTimezone.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    updateApplyButton();
                    getContainer().updateButtons();
                }
            });
            IContentProposalProvider proposalProvider = (contents, position) -> {
                List<IContentProposal> proposals = new ArrayList<>();
                for (String item : clientTimezone.getItems()) {
                    if (item.toLowerCase().contains(contents.toLowerCase())) {
                        proposals.add(new ContentProposal(item));
                    }
                }
                return proposals.toArray(IContentProposal[]::new);
            };
            ContentAssistUtils.installContentProposal(clientTimezone, new ComboContentAdapter(), proposalProvider);

            Control tipLabelRestart = UIUtils.createInfoLabel(regionalSettingsGroup,
                CoreMessages.pref_page_ui_general_label_options_take_effect_after_restart
            );
            tipLabelRestart.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
                GridData.VERTICAL_ALIGN_BEGINNING,
                false,
                false,
                2,
                1
            ));

            Group groupObjects = UIUtils.createControlGroup(
                composite,
                CoreMessages.pref_page_ui_general_group_browser, 2,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0
            );
            if (RuntimeUtils.isWindows()) {
                browserCombo = UIUtils.createLabelCombo(groupObjects, CoreMessages.pref_page_ui_general_combo_browser,
                    SWT.READ_ONLY
                );
                browserCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
                for (SWTBrowserRegistry.BrowserSelection value : SWTBrowserRegistry.BrowserSelection.values()) {
                    browserCombo.add(value.getFullName(), value.ordinal());
                }
                Control tipLabel =
                    UIUtils.createInfoLabel(groupObjects, CoreMessages.pref_page_ui_general_combo_browser_tip);
                tipLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
                    GridData.VERTICAL_ALIGN_BEGINNING, false, false, 2, 1
                ));
            }

            useEmbeddedBrowserAuth = UIUtils.createCheckbox(groupObjects,
                CoreMessages.pref_page_ui_general_check_browser_auth,
                CoreMessages.pref_page_ui_general_check_browser_auth_tip,
                false,
                2
            );
            useEmbeddedBrowserAuth.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING,
                GridData.VERTICAL_ALIGN_BEGINNING,
                false,
                false,
                2,
                1
            ));
            if (browserCombo != null) {
                browserCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (browserCombo.getSelectionIndex() == SWTBrowserRegistry.BrowserSelection.IE.ordinal()) {
                            useEmbeddedBrowserAuth.setEnabled(false);
                            useEmbeddedBrowserAuth.setSelection(false);
                        } else {
                            useEmbeddedBrowserAuth.setEnabled(true);
                        }
                    }
                });
            }
        }

        Group breadcrumbs = UIUtils.createControlGroup(
            composite,
            CoreMessages.pref_page_ui_status_bar,
            2,
            GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING,
            0
        );
        statusBarShowBreadcrumbsCheck = UIUtils.createCheckbox(
            breadcrumbs,
            CoreMessages.pref_page_ui_status_bar_show_breadcrumbs_check_label,
            CoreMessages.pref_page_ui_status_bar_show_breadcrumbs_check_tip,
            true,
            1
        );
        statusBarShowBreadcrumbsCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            statusBarBreadcrumbPositionCombo.setEnabled(statusBarShowBreadcrumbsCheck.getSelection());
        }));

        statusBarBreadcrumbPositionCombo = new Combo(breadcrumbs, SWT.READ_ONLY | SWT.DROP_DOWN);
        statusBarBreadcrumbPositionCombo.add(CoreMessages.pref_page_ui_status_bar_show_breadcrumbs_status_bar_label);
        statusBarBreadcrumbPositionCombo.add(CoreMessages.pref_page_ui_status_bar_show_breadcrumbs_editors_label);
        statusBarBreadcrumbPositionCombo.select(0);

        statusBarShowStatusCheck = UIUtils.createCheckbox(
            breadcrumbs,
            CoreMessages.pref_page_ui_status_bar_show_status_line_check_label,
            CoreMessages.pref_page_ui_status_bar_show_status_line_check_tip,
            true,
            2
        );

        setSettings();
        return composite;
    }

    private void setSettings() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        if (isWindowsDesktopClient()) {
            SWTBrowserRegistry.getActiveBrowser();
            browserCombo.select(SWTBrowserRegistry.getActiveBrowser().ordinal());
            useEmbeddedBrowserAuth.setEnabled(!SWTBrowserRegistry.getActiveBrowser().equals(SWTBrowserRegistry.BrowserSelection.IE));
        }
        if (isStandalone) { 
            if (!ApplicationPolicyService.getInstance().isInstallUpdateDisabled()) {
                automaticUpdateCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK));
            }
            useEmbeddedBrowserAuth.setSelection(store.getBoolean(DBeaverPreferences.UI_USE_EMBEDDED_AUTH));
        }
        final String timezone = store.getString(ModelPreferences.CLIENT_TIMEZONE);
        if (clientTimezone != null) {
            if (DBConstants.DEFAULT_TIMEZONE.equals(timezone)) {
                clientTimezone.setText(DBConstants.DEFAULT_TIMEZONE);
            } else {
                clientTimezone.setText(timezone);
            }
        }

        BreadcrumbLocation breadcrumbLocation = DatabaseEditorPreferences.BreadcrumbLocation.get(store);
        statusBarShowBreadcrumbsCheck.setSelection(breadcrumbLocation != DatabaseEditorPreferences.BreadcrumbLocation.HIDDEN);
        statusBarBreadcrumbPositionCombo.select(breadcrumbLocation == DatabaseEditorPreferences.BreadcrumbLocation.IN_EDITORS ? 1 : 0);
        statusBarBreadcrumbPositionCombo.setEnabled(statusBarShowBreadcrumbsCheck.getSelection());
        statusBarShowStatusCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_STATUS_BAR_SHOW_STATUS_LINE));
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        if (isStandalone) {
            useEmbeddedBrowserAuth.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_USE_EMBEDDED_AUTH));
            if (!ApplicationPolicyService.getInstance().isInstallUpdateDisabled()) {
                automaticUpdateCheck.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK));
            }
        }
        if (isWindowsDesktopClient()) {
            SWTBrowserRegistry.getActiveBrowser();
            browserCombo.select(SWTBrowserRegistry.getDefaultBrowser().ordinal());
        }
        if (clientTimezone != null) {
            UIUtils.setComboSelection(clientTimezone, store.getDefaultString(ModelPreferences.CLIENT_TIMEZONE));
        }

        BreadcrumbLocation location = BreadcrumbLocation.getDefault(store);
        statusBarShowBreadcrumbsCheck.setSelection(location != BreadcrumbLocation.HIDDEN);
        statusBarBreadcrumbPositionCombo.select(location == BreadcrumbLocation.IN_STATUS_BAR ? 0 : 1);
        statusBarShowStatusCheck.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_STATUS_BAR_SHOW_STATUS_LINE));
    }

    private boolean isWindowsDesktopClient() {
        return isStandalone && RuntimeUtils.isWindows();
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        if (isStandalone) {
            store.setValue(DBeaverPreferences.UI_USE_EMBEDDED_AUTH, useEmbeddedBrowserAuth.getSelection());
            if (!ApplicationPolicyService.getInstance().isInstallUpdateDisabled()) {
                store.setValue(DBeaverPreferences.UI_AUTO_UPDATE_CHECK, automaticUpdateCheck.getSelection());
            } else {
                store.setValue(DBeaverPreferences.UI_AUTO_UPDATE_CHECK, Boolean.FALSE);
            }


            if (isWindowsDesktopClient()) {
                SWTBrowserRegistry.setActiveBrowser(SWTBrowserRegistry.BrowserSelection.values()[browserCombo.getSelectionIndex()]);
            }

            PrefUtils.savePreferenceStore(store);
            if (clientTimezone != null) {
                if (DBConstants.DEFAULT_TIMEZONE.equals(clientTimezone.getText())) {
                    TimezoneRegistry.setDefaultZone(null, true);
                } else {
                    TimezoneRegistry.setDefaultZone(
                        ZoneId.of(TimezoneRegistry.extractTimezoneId(clientTimezone.getText())), true);
                }
            }
            if (workspaceLanguage.getSelectionIndex() >= 0) {
                PlatformLanguageDescriptor language = PlatformLanguageRegistry.getInstance().getLanguages()
                    .get(workspaceLanguage.getSelectionIndex());
                DBPPlatformLanguage curLanguage = DBPPlatformDesktop.getInstance().getLanguage();

                try {
                    if (curLanguage != language) {
                        ((DBPPlatformLanguageManager) DBWorkbench.getPlatform()).setPlatformLanguage(language);
                        if (UIUtils.confirmAction(
                            getShell(),
                            "Restart " + GeneralUtils.getProductName(),
                            "You need to restart " + GeneralUtils.getProductName()
                                + " to perform actual language change.\nDo you want to restart?"
                        )) {
                            restartWorkbenchOnPrefChange();
                        }
                    }
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Change language", "Can't switch language to " + language, e);
                }
            }
        }

        BreadcrumbLocation breadcrumbLocation;
        if (!statusBarShowBreadcrumbsCheck.getSelection()) {
            breadcrumbLocation = DatabaseEditorPreferences.BreadcrumbLocation.HIDDEN;
        } else if (statusBarBreadcrumbPositionCombo.getSelectionIndex() == 0) {
            breadcrumbLocation = DatabaseEditorPreferences.BreadcrumbLocation.IN_STATUS_BAR;
        } else {
            breadcrumbLocation = DatabaseEditorPreferences.BreadcrumbLocation.IN_EDITORS;
        }

        store.setValue(DBeaverPreferences.UI_STATUS_BAR_SHOW_BREADCRUMBS, breadcrumbLocation.name());
        store.setValue(DBeaverPreferences.UI_STATUS_BAR_SHOW_STATUS_LINE, statusBarShowStatusCheck.getSelection());

        return true;
    }

    @Nullable
    @Override
    public IAdaptable getElement()
    {
        return null;
    }

    @Override
    public void setElement(IAdaptable element)
    {
    }
}
