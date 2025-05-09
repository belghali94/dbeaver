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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueRow;
import org.jkiss.dbeaver.model.data.storage.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCellLocation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetValueController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Find/Replace target for result set viewer
 */
class SpreadsheetFindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension3 {

    private static final Log log = Log.getLog(SpreadsheetFindReplaceTarget.class);
    private static final Object REDRAW_SYNC = new Object();
    private static SpreadsheetFindReplaceTarget instance;

    /**
     * Uses {@link Object#hashCode()} to identity the current owner and determine whether he was changed or not.
     */
    private int ownerIdentity;
    private Pattern searchPattern;
    private Color scopeHighlightColor;
    private boolean replaceAll;
    private boolean sessionActive = false;
    private boolean firstSearchInSession = true;
    private List<GridPos> originalSelection = new ArrayList<>();
    private final Set<DBDValueRow> updatedRows = new LinkedHashSet<>();
    private final Set<DBDAttributeBinding> updatedAttributes = new LinkedHashSet<>();
    private final Set<GridPos> processedCells = new HashSet<>();
    private AbstractJob redrawJob = null;
    private String currentFindString = "";
    private boolean currentCaseSensitive;
    private boolean currentWholeWord;
    private boolean currentRegEx;

    public static synchronized SpreadsheetFindReplaceTarget getInstance() {
        if (instance == null) {
            instance = new SpreadsheetFindReplaceTarget();
            instance.scopeHighlightColor = UIStyles.getDefaultTextColor("AbstractTextEditor.Color.FindScope", SWT.COLOR_LIST_SELECTION);
        }
        return instance;
    }

    public synchronized SpreadsheetFindReplaceTarget owned(@NotNull SpreadsheetPresentation newOwner) {
        refreshOwner(newOwner);
        return this;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public Pattern getSearchPattern() {
        return searchPattern;
    }

    public Color getScopeHighlightColor() {
        return scopeHighlightColor;
    }

    @Override
    public boolean canPerformFind() {
        return true;
    }

    @Override
    public Point getSelection() {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return new Point(0, 0);
        }
        Collection<Integer> rowSelection = owner.getSpreadsheet().getRowSelection();
        int minRow = rowSelection.stream().mapToInt(v -> v).min().orElse(-1);
        int maxRow = rowSelection.stream().mapToInt(v -> v).max().orElse(-1);

        return new Point(minRow, maxRow);
    }

    @Override
    public String getSelectionText() {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return "";
        }
        GridPos selection = owner.getSelection().getFirstElement();
        if (selection == null) {
            return "";
        }
        Spreadsheet spreadsheet = owner.getSpreadsheet();
        GridCell cell = spreadsheet.posToCell(selection);
        String value = cell == null ? "" : CommonUtils.toString(spreadsheet.getContentProvider().getCellValue(cell.col, cell.row, false));
        return CommonUtils.toString(value);
    }

    @Override
    public boolean isEditable() {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        return owner != null && owner.getController().getReadOnlyStatus() == null;
    }

    @Override
    public void beginSession() {
        synchronized (REDRAW_SYNC) {
            updatedRows.clear();
            updatedAttributes.clear();
        }
        processedCells.clear();

        final SpreadsheetPresentation owner = getActiveSpreadsheet(false);
        if (owner == null) {
            return;
        }
        this.sessionActive = true;
        owner.getControl().redraw();
        this.originalSelection = new ArrayList<>(owner.getSpreadsheet().getSelection());
        owner.highlightRows(-1, -1, null);
    }

    @Override
    public void endSession() {
        final SpreadsheetPresentation owner = getActiveSpreadsheet(false);
        if (owner == null) {
            return;
        }
        this.sessionActive = false;
        this.searchPattern = null;
        this.firstSearchInSession = true;
        Control control = owner.getControl();
        if (control != null && !control.isDisposed()) {
            owner.getSpreadsheet().deselectAll();
            owner.getSpreadsheet().selectCells(this.originalSelection);
        }
    }

    @Override
    public IRegion getScope() {
        return null;
    }

    @Override
    public void setScope(IRegion scope) {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return;
        }
        if (scope == null || scope.getLength() == 0) {
            owner.highlightRows(-1, -1, null);
            if (scope == null) {
                owner.getSpreadsheet().deselectAll();
                owner.getSpreadsheet().selectCells(this.originalSelection);
            }
        } else {
            owner.highlightRows(scope.getOffset(), scope.getLength(), scopeHighlightColor);
        }
    }

    @Override
    public Point getLineSelection() {
        return getSelection();
    }

    @Override
    public void setSelection(int offset, int length) {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return;
        }
        int columnCount = owner.getSpreadsheet().getColumnCount();
        List<GridPos> selRows = new ArrayList<>();
        for (int rowNum = 0; rowNum < length; rowNum++) {
            for (int col = 0; col < columnCount; col++) {
                selRows.add(new GridPos(col, offset + rowNum));
            }
        }
        owner.setSelection(
            new StructuredSelection(selRows));
    }

    @Override
    public void setScopeHighlightColor(Color color) {
        this.scopeHighlightColor = color;
    }

    @Override
    public void setReplaceAllMode(boolean replaceAll) {
        this.replaceAll = replaceAll;
    }

    @Override
    public void replaceSelection(String text) {
        replaceSelection(text, false);
    }

    @Override
    public void replaceSelection(
        @NotNull String text,
        boolean regExReplace
    ) {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return;
        }

        // Lazy initialization of search pattern
        if (searchPattern == null && !currentFindString.isEmpty()) {
            searchPattern = createSearchPattern(
                currentFindString,
                currentCaseSensitive,
                currentWholeWord,
                currentRegEx
            );
        }

        GridPos selection = owner.getSelection().getFirstElement();
        if (selection == null) {
            return;
        }

        if (replaceAll && processedCells.contains(selection)) {
            return;
        }

        GridCell cell = owner.getSpreadsheet().posToCell(selection);
        if (cell == null) {
            return;
        }

        ResultSetCellLocation cellLocation = owner.getCellLocation(cell);
        String oldValue = CommonUtils.toString(owner.getSpreadsheet().getContentProvider().getCellValue(
            cell.col, cell.row, true));
        String newValue = oldValue;

        if (searchPattern != null) {
            newValue = searchPattern.matcher(oldValue).replaceAll(text);
        }

        try {
            if (oldValue.equals(newValue)) {
                return;
            }

            Object originalValue = owner.getSpreadsheet().getContentProvider().getCellValue(
                cell.col, cell.row, false);

            if (originalValue instanceof DBDContent content) {
                // Special handling for content/blob values
                content.updateContents(new VoidProgressMonitor(), new StringContentStorage(newValue));
                new ResultSetValueController(owner.getController(), cellLocation, IValueController.EditType.NONE, null)
                    .updateValue(originalValue, !replaceAll);
            } else {
                // Standard value update
                owner.getController().updateCellValue(
                    cellLocation.getAttribute(),
                    cellLocation.getRow(),
                    cellLocation.getRowIndexes(),
                    newValue,
                    !replaceAll);
            }

            if (replaceAll) {
                processedCells.add(selection);
            }
        } catch (DBException e) {
            log.error("Error updating cell value", e);
        } finally {
            if (replaceAll) {
                searchPattern = null;
                currentFindString = "";
            }
        }

        GridPos currentPos = owner.getSpreadsheet().getFocusPos();
        storeLastFoundPosition(currentPos);
        if (!replaceAll) {
            owner.getController().redrawData(true, true);
            synchronized (REDRAW_SYNC) {
                updatedAttributes.add(cellLocation.getAttribute());
                updatedRows.add(cellLocation.getRow());
                if (redrawJob == null) {
                    redrawJob = new AbstractJob("Redraw grid after replace") {
                        @Override
                        protected IStatus run(DBRProgressMonitor monitor) {
                            Set<DBDAttributeBinding> attrs;
                            Set<DBDValueRow> rows;
                            synchronized (REDRAW_SYNC) {
                                attrs = new LinkedHashSet<>(updatedAttributes);
                                rows = new LinkedHashSet<>(updatedRows);
                                updatedAttributes.clear();
                                updatedRows.clear();
                                redrawJob = null;
                            }
                            UIUtils.syncExec(() -> {
                                owner.getController().refreshHintCache(attrs, rows, null);
                                owner.getController().redrawData(false, true);
                                owner.getController().updatePanelsContent(false);
                            });
                            return Status.OK_STATUS;
                        }
                    };
                    redrawJob.schedule(150);
                }
            }
        }
    }

    @Override
    public int findAndSelect(
        int widgetOffset,
        @NotNull String findString,
        boolean searchForward,
        boolean caseSensitive,
        boolean wholeWord
    ) {
        return findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord, false);
    }

    @Override
    public int findAndSelect(
        int offset,
        @NotNull String findString,
        boolean searchForward,
        boolean caseSensitive,
        boolean wholeWord,
        boolean regExSearch
    ) {
        this.currentFindString = findString;
        this.currentCaseSensitive = caseSensitive;
        this.currentWholeWord = wholeWord;
        this.currentRegEx = regExSearch;

        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return -1;
        }
        ResultSetModel model = owner.getController().getModel();
        if (model.isEmpty()) {
            return -1;
        }

        Spreadsheet spreadsheet = owner.getSpreadsheet();
        int rowCount = spreadsheet.getItemCount();
        int columnCount = spreadsheet.getColumnCount();

        // Handle record mode special column (-1 indicates record selector column)
        boolean recordMode = owner.getController().isRecordMode();
        int minColumn = recordMode ? -1 : 0;

        int firstRow = Math.max(owner.getHighlightScopeFirstLine(), 0);
        int lastRow = Math.min(owner.getHighlightScopeLastLine(), rowCount - 1);
        if (lastRow < 0) {
            lastRow = rowCount - 1;
        }

        GridPos startPos = getStartPosition(
            spreadsheet,
            searchForward,
            firstRow,
            lastRow,
            minColumn,
            columnCount,
            firstSearchInSession
        );

        if (firstSearchInSession) {
            firstSearchInSession = false;
            storeLastFoundPosition(startPos);
        }

        Pattern pattern = createSearchPattern(currentFindString, currentCaseSensitive, currentWholeWord, currentRegEx);
        if (pattern == null) {
            return -1;
        }
        this.searchPattern = pattern;

        GridPos currentPos = new GridPos(startPos.col, startPos.row);
        boolean wrapped = false;
        int totalCells = (lastRow - firstRow + 1) * (columnCount - minColumn);
        int checked = 0;

        while (checked <= totalCells) {
            if (isCellInScope(currentPos, firstRow, lastRow, minColumn, columnCount)) {
                if (!replaceAll || !processedCells.contains(currentPos)) {
                    String cellText = getCellText(spreadsheet, currentPos, recordMode, minColumn);
                    if (cellText != null && pattern.matcher(cellText).find()) {
                        selectCell(spreadsheet, currentPos, minColumn);
                        storeLastFoundPosition(currentPos);
                        return currentPos.row;
                    }
                }
                checked++;
            }

            currentPos = getNextPosition(currentPos, searchForward, columnCount, minColumn, firstRow, lastRow);

            // Handle search wrap-around
            if (!isCellInScope(currentPos, firstRow, lastRow, minColumn, columnCount)) {
                if (wrapped) { // Prevent infinite loop
                    break;
                }
                currentPos = getWrapAroundPosition(searchForward, firstRow, lastRow, minColumn, columnCount);
                wrapped = true;
            }
        }
        processedCells.clear();
        return -1; // No matches found
    }

    private Pattern createSearchPattern(
        @NotNull String findString,
        boolean caseSensitive,
        boolean wholeWord,
        boolean regEx
    ) {
        if (findString.isEmpty()) {
            return null;
        }

        try {
            if (regEx) {
                return Pattern.compile(findString, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            } else {
                String pattern = wholeWord
                    ? "\\b" + Pattern.quote(findString) + "\\b"
                    : Pattern.quote(findString);
                return Pattern.compile(pattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            }
        } catch (PatternSyntaxException e) {
            log.error("Invalid search pattern: " + findString, e);
            return null;
        }
    }

    // Helper method to determine initial search position
    private GridPos getStartPosition(
        @NotNull Spreadsheet spreadsheet,
        boolean searchForward,
        int firstRow,
        int lastRow,
        int minColumn,
        int columnCount,
        boolean isFirstSearch
    ) {
        GridPos currentPos = spreadsheet.getCursorPosition();

        // Default to boundary positions when no cursor
        if (currentPos == null) {
            return searchForward ?
                new GridPos(minColumn, firstRow) :  // Start from top-left
                new GridPos(columnCount - 1, lastRow); // Start from bottom-right
        }

        // Use current position for first search, next position otherwise
        return isFirstSearch ? currentPos :
            getNextPosition(currentPos, searchForward, columnCount, minColumn, firstRow, lastRow);
    }

    // Stores the last found position for subsequent operations
    private void storeLastFoundPosition(@NotNull GridPos pos) {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner != null) {
            Spreadsheet spreadsheet = owner.getSpreadsheet();
            // Adjust column for record mode
            if (owner.getController().isRecordMode() && pos.col == -1) {
                pos = new GridPos(0, pos.row);
            }
            // Update spreadsheet focus and selection
            spreadsheet.setFocusColumn(pos.col);
            spreadsheet.setFocusItem(pos.row);
            spreadsheet.setCellSelection(pos);
            spreadsheet.showSelection();
        }
    }

    // Retrieves cell text based on mode and position
    private String getCellText(
        @NotNull Spreadsheet spreadsheet,
        @NotNull GridPos pos,
        boolean recordMode,
        int minColumn
    ) {
        // Handle record mode special column
        if (recordMode && pos.col == minColumn) {
            return spreadsheet.getLabelProvider().getText(spreadsheet.getRow(pos.row));
        }
        // Standard cell value retrieval
        GridCell cell = spreadsheet.posToCell(pos);
        return cell != null ?
            CommonUtils.toString(spreadsheet.getContentProvider().getCellValue(cell.col, cell.row, false)) :
            null;
    }

    // Selects a cell and updates UI focus
    private void selectCell(
        @NotNull Spreadsheet spreadsheet,
        @NotNull GridPos pos,
        int minColumn
    ) {
        // Adjust position for record mode
        if (pos.col == minColumn) {
            pos = new GridPos(0, pos.row);
        }
        // Update spreadsheet state
        spreadsheet.setFocusColumn(pos.col);
        spreadsheet.setFocusItem(pos.row);
        spreadsheet.setCellSelection(pos);
        spreadsheet.showSelection();
    }

    // Calculates next cell position based on search direction
    private GridPos getNextPosition(
        @NotNull GridPos pos,
        boolean searchForward,
        int columnCount,
        int minColumn,
        int firstRow,
        int lastRow
    ) {
        GridPos next = new GridPos(pos.col, pos.row);

        if (searchForward) {
            // Move right, wrap to next row
            next.col++;
            if (next.col >= columnCount) {
                next.col = minColumn;
                next.row = (next.row >= lastRow) ? firstRow : next.row + 1;
            }
        } else {
            // Move left, wrap to previous row
            next.col--;
            if (next.col < minColumn) {
                next.col = columnCount - 1;
                next.row = (next.row <= firstRow) ? lastRow : next.row - 1;
            }
        }
        return next;
    }

    // Returns wrap-around position when reaching boundary
    private GridPos getWrapAroundPosition(
        boolean searchForward,
        int firstRow,
        int lastRow,
        int minColumn,
        int columnCount
    ) {
        return searchForward ?
            new GridPos(minColumn, firstRow) :  // Top-left corner
            new GridPos(columnCount - 1, lastRow); // Bottom-right corner
    }

    private boolean isCellInScope(
        GridPos pos,
        int firstRow,
        int lastRow,
        int minColumn,
        int columnCount
    ) {
        return pos.row >= firstRow &&
               pos.row <= lastRow &&
               pos.col >= minColumn &&
               pos.col < columnCount;
    }

    @Override
    public String toString() {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return super.toString();
        }
        DBSDataContainer dataContainer = owner.getController().getDataContainer();
        return "Target: " + (dataContainer == null ? null : dataContainer.getName());
    }

    private void refreshOwner(@NotNull SpreadsheetPresentation newOwner) {
        if (this.ownerIdentity == newOwner.hashCode()) {
            return;
        }
        final boolean refreshSession = this.sessionActive;
        final Pattern searchPattern = this.searchPattern;
        if (refreshSession) {
            this.endSession();
        }
        this.ownerIdentity = newOwner.hashCode();
        if (refreshSession) {
            this.beginSession();
            this.searchPattern = searchPattern;
        }
    }

    @Nullable
    private SpreadsheetPresentation getActiveSpreadsheet() {
        return getActiveSpreadsheet(true);
    }

    @Nullable
    private SpreadsheetPresentation getActiveSpreadsheet(boolean refreshActiveSpreadsheet) {
        final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            return null;
        }
        final IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return null;
        }
        final SpreadsheetPresentation spreadsheet = activeEditor.getAdapter(SpreadsheetPresentation.class);
        if (spreadsheet == null) {
            return null;
        }
        if (refreshActiveSpreadsheet) {
            refreshOwner(spreadsheet);
        }
        return spreadsheet;
    }
}
