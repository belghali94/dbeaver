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
package org.jkiss.dbeaver.ext.postgresql.ui.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.jkiss.dbeaver.ext.postgresql.model.session.PostgreSession;
import org.jkiss.dbeaver.ext.postgresql.model.session.PostgreSessionManager;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.session.AbstractSessionEditor;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSessionEditor
 */
public class PostgreSessionEditor extends AbstractSessionEditor
{
    private KillSessionAction terminateQueryAction;
    private CancelQueryAction cancelQueryAction;
    private boolean showIdle = true;

    @Override
    public void createEditorControl(Composite parent) {
        terminateQueryAction = new KillSessionAction();
        cancelQueryAction = new CancelQueryAction();
        super.createEditorControl(parent);
    }

    @Override
    protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
        DBAServerSessionManager sessionManager = DBUtils.getAdapter(DBAServerSessionManager.class, executionContext.getDataSource());
        return new SessionManagerViewer<PostgreSession>(this, parent, sessionManager) {
            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager, IContributionManager contributionManager)
            {
                contributionManager.add(new ShowIdleAction());
                contributionManager.add(new Separator());
                contributionManager.add(terminateQueryAction);
                contributionManager.add(cancelQueryAction);
                contributionManager.add(new Separator());
            }

            @Override
            protected void onSessionSelect(DBAServerSession session) {
                super.onSessionSelect(session);
                terminateQueryAction.setEnabled(session != null);
                cancelQueryAction.setEnabled(session != null);
            }
            
            @Override
            protected void loadSettings(IDialogSettings settings) {
                showIdle = CommonUtils.getBoolean(settings.get(PostgreSessionManager.OPTION_SHOW_IDLE), true);
                super.loadSettings(settings);
            }
            
            @Override
            protected void saveSettings(IDialogSettings settings) {
                super.saveSettings(settings);
                settings.put(PostgreSessionManager.OPTION_SHOW_IDLE, showIdle);
            }
            
            @Override
            public Map<String, Object> getSessionOptions() {
                Map<String, Object> options = new HashMap<>();
                options.put(PostgreSessionManager.OPTION_SHOW_IDLE, showIdle);
                
                return options;
            }
            
        };
    }
    
    private class ShowIdleAction extends Action {
        
        ShowIdleAction() {
            super(
                "Show idle connections",
                DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
            setToolTipText("Show idle connections");
            setChecked(showIdle);
        }
        
        @Override
        public void run() {
            showIdle = isChecked();
            refreshPart(PostgreSessionEditor.this, true);
        }
    }
    private class CancelQueryAction extends Action {
        CancelQueryAction() {
            super(
                "Cancel query",
                UIUtils.getShardImageDescriptor(ISharedImages.IMG_ELCL_STOP));
            setToolTipText("Cancel query");
        }
        @Override
        public void run() {
            final List<DBAServerSession> sessions = getSessionsViewer().getSelectedSessions();
            if (sessions != null && UIUtils.confirmAction(
                getSite().getShell(),
                this.getText(),
                NLS.bind("Cancel query of session {0}?", sessions)))
            {
                Map<String, Object> options = new HashMap<String,Object>();
                options.put(PostgreSessionManager.OPTION_QUERY_CANCEL, true);
                getSessionsViewer().alterSessions(
                    sessions,
                    options);
            }
        }
    }

    private class KillSessionAction extends Action {
        KillSessionAction()
        {
            super(
                "Terminate",
                UIUtils.getShardImageDescriptor(ISharedImages.IMG_ELCL_STOP));
        }

        @Override
        public void run()
        {
            final List<DBAServerSession> sessions = getSessionsViewer().getSelectedSessions();
            if (sessions != null && UIUtils.confirmAction(
                getSite().getShell(),
                this.getText(),
                NLS.bind("Terminate session {0}?", sessions)))
            {
                getSessionsViewer().alterSessions(
                    sessions,
                    null);
            }
        }
    }

}