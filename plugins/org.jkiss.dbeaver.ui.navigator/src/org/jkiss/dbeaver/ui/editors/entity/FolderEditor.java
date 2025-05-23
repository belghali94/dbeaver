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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.editors.INavigatorEditorInput;
import org.jkiss.dbeaver.ui.editors.NodeEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.itemlist.ItemListControl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * FolderEditor
 */
public class FolderEditor extends EditorPart implements INavigatorModelView, IRefreshablePart, ISearchContextProvider {
    private static final Log log = Log.getLog(FolderEditor.class);

    private FolderListControl itemControl;
    private final List<String> history = new ArrayList<>();
    private int historyPosition = 0;

    @Override
    public void createPartControl(Composite parent) {
        itemControl = new FolderListControl(parent);
        itemControl.createProgressPanel();
        getSite().setSelectionProvider(itemControl.getSelectionProvider());

        UIExecutionQueue.queueExec(() -> {
            final DBNNode navigatorNode = getEditorInput().getNavigatorNode();
            setTitleImage(DBeaverIcons.getImage(navigatorNode.getNodeIconDefault()));
            setPartName(navigatorNode.getNodeDisplayName());

            itemControl.setRootNode(navigatorNode);
            itemControl.loadData();

            DBNNode rootNode = getRootNode();
            history.add(rootNode.getNodeUri());

            itemControl.getSelectionProvider().setDefaultSelection(new StructuredSelection(navigatorNode));

            parent.layout(true, true);
        });
    }

    @Override
    public void setFocus() {
        if (itemControl != null) {
            itemControl.setFocus();
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor) {

    }

    @Override
    public void doSaveAs() {

    }

    @Override
    public INavigatorEditorInput getEditorInput() {
        return (INavigatorEditorInput) super.getEditorInput();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        setSite(site);
        setInput(input);
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public DBNNode getRootNode() {
        return getEditorInput().getNavigatorNode();
    }

    @Nullable
    @Override
    public Viewer getNavigatorViewer() {
        return itemControl == null ? null : itemControl.getNavigatorViewer();
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        UIUtils.asyncExec(() -> {
            if (!itemControl.isDisposed()) {
                itemControl.loadData(false);
            }
        });
        return RefreshResult.REFRESHED;
    }

    @Override
    public boolean isSearchPossible() {
        return itemControl != null && itemControl.isSearchPossible();
    }

    @Override
    public boolean isSearchEnabled() {
        return itemControl != null && itemControl.isSearchEnabled();
    }

    @Override
    public boolean performSearch(SearchType searchType) {
        return itemControl.performSearch(searchType);
    }

    public int getHistoryPosition() {
        return historyPosition;
    }

    public int getHistorySize() {
        return history.size();
    }

    public void navigateHistory(int position) {
        if (position >= history.size()) {
            position = history.size() - 1;
        } else if (position < 0) {
            position = -1;
        }
        if (position < 0 || position >= history.size()) {
            return;
        }
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        String nodePath = history.get(position);
        try {
            DBNNode node = DBWorkbench.getPlatform().getNavigatorModel().getNodeByPath(monitor, nodePath);
            if (node != null) {
                historyPosition = position;
                itemControl.changeCurrentNode(node);
            } else {
                DBWorkbench.getPlatformUI().showMessageBox("Can't navigator to node", "Node '" + nodePath + "' not found", true);
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Can't obtain navigator to node", "Error reading navigator node", e);
            log.error(e);
        }

    }

    private class FolderListControl extends ItemListControl {
        public FolderListControl(Composite parent) {
            super(parent, SWT.SHEET, FolderEditor.this.getSite(), DBWorkbench.getPlatform().getNavigatorModel().getRoot(), null);
        }

        @Override
        protected void setListData(Collection<DBNNode> items, boolean append, boolean forUpdate) {
            if (!append) {
                // Add parent node reference (we actually add DBNRoot to avoid unneeded parent properties columns loading)
                final DBNNode rootNode = getRootNode();
                final DBNNode parentNode = rootNode.getParentNode();
                if (parentNode instanceof DBNProjectDatabases || parentNode instanceof DBNLocalFolder) {
                    List<DBNNode> nodesWithParent = new ArrayList<>(items);
                    nodesWithParent.add(0, DBWorkbench.getPlatform().getNavigatorModel().getRoot());
                    items = nodesWithParent;
                }
            }
            if (items != null) {
                items.removeIf(DBUtils::isHiddenObject);
            }
            super.setListData(items, append, forUpdate);
        }

        @Nullable
        @Override
        protected Object getCellValue(Object element, ObjectColumn objectColumn, boolean formatValue) {
            if (element instanceof DBNRoot) {
                return objectColumn.isNameColumn(getObjectValue((DBNRoot) element)) ? ".." : "";
            }
            return super.getCellValue(element, objectColumn, formatValue);
        }

        @Override
        protected void openNodeEditor(DBNNode node) {
            final DBNNode rootNode = getRootNode();
            if (!canOpenNode(node)) {
                if (node instanceof DBNRoot) {
                    if (rootNode instanceof DBNLocalFolder) {
                        node = ((DBNLocalFolder) rootNode).getLogicalParent();
                    } else {
                        node = rootNode.getParentNode();
                    }
                }

                if (historyPosition >= 0) {
                    while (historyPosition < history.size() - 1) {
                        history.remove(historyPosition + 1);
                    }
                }
                historyPosition++;
                history.add(node.getNodeUri());
                changeCurrentNode(node);
            } else {
                super.openNodeEditor(node);
            }
        }

        private void changeCurrentNode(DBNNode node) {
            if (getRootNode() != null && node.getClass() != getRootNode().getClass()) {
                // Different node type - cleanup
                clearListData();
            }
            setRootNode(node);
            loadData();
            setPartName(node.getNodeDisplayName());
            setTitleImage(DBeaverIcons.getImage(node.getNodeIconDefault()));
            updateActions();

            // Update editor input
            final INavigatorEditorInput editorInput = getEditorInput();
            if (editorInput instanceof NodeEditorInput) {
                ((NodeEditorInput) editorInput).setNavigatorNode(node);
            }
        }

        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            contributionManager.add(ActionUtils.makeCommandContribution(getSite(), IWorkbenchCommandConstants.NAVIGATE_BACKWARD_HISTORY, CommandContributionItem.STYLE_PUSH, UIIcon.RS_BACK));
            contributionManager.add(ActionUtils.makeCommandContribution(getSite(), IWorkbenchCommandConstants.NAVIGATE_FORWARD_HISTORY, CommandContributionItem.STYLE_PUSH, UIIcon.RS_FORWARD));
            contributionManager.add(new Separator());
            super.fillCustomActions(contributionManager);
        }
    }

    private boolean canOpenNode(DBNNode node) {
        return node instanceof DBNDatabaseNode || node.getAdapter(IResource.class) instanceof IFile;
    }

}
