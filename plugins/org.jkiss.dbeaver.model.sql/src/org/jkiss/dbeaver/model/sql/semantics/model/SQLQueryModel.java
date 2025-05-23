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
package org.jkiss.dbeaver.model.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryPureResultTupleContext;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsCteModel;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryConnectionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Query model for recognition
 */
public class SQLQueryModel extends SQLQueryNodeModel {
    @NotNull
    private final Set<SQLQuerySymbolEntry> symbolEntries;
    @Nullable
    private final SQLQueryModelContent queryContent;
    @NotNull
    private final List<SQLQueryLexicalScopeItem> lexicalItems;
    @Nullable
    private SQLQueryDataContext dataContext = null;

    public SQLQueryModel(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQueryModelContent queryContent,
        @NotNull Set<SQLQuerySymbolEntry> symbolEntries,
        @NotNull List<SQLQueryLexicalScopeItem> lexicalItems
    ) {
        super(syntaxNode.getRealInterval(), syntaxNode, queryContent);
        this.queryContent = queryContent;
        this.symbolEntries = symbolEntries;
        this.lexicalItems = lexicalItems;
    }

    @NotNull
    public Collection<SQLQuerySymbolEntry> getAllSymbols() {
        return symbolEntries;
    }

    @Nullable
    public SQLQueryModelContent getQueryModel() {
        return this.queryContent;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getGivenDataContext() {
        return this.dataContext;
    }

    @Nullable
    @Override
    public SQLQueryDataContext getResultDataContext() {
        return this.queryContent == null ? null : this.queryContent.getResultDataContext();
    }

    /**
     * Propagate semantics context and establish relations through the query model
     */
    public void propagateContext(
        @NotNull SQLQueryDataContext dataContext,
        @NotNull SQLQueryConnectionContext connectionContext,
        @NotNull SQLQueryRecognitionContext recognitionContext
    ) {
        this.dataContext = dataContext;

        if (this.queryContent != null) {
            if (this.queryContent instanceof SQLQueryRowsCteModel rowsSource) {
                SQLQueryRowsSourceContext rowsContext = new SQLQueryRowsSourceContext(connectionContext);
                rowsSource.resolveObjectAndRowsReferences(rowsContext, recognitionContext);
                rowsSource.resolveValueRelations(rowsContext.makeEmptyTuple(), recognitionContext);
            } else {
                this.queryContent.applyContext(dataContext, recognitionContext);
            }
        }

        int actualTailPosition = this.getSyntaxNode().getRealInterval().b;
        SQLQueryNodeModel tailNode = this.findNodeContaining(actualTailPosition);
        if (tailNode != this) {
            SQLQuerySymbolOrigin tailOrigin = tailNode.getTailOrigin();
            if (tailOrigin == null) {
                SQLQueryLexicalScope tailNodeScope = tailNode.findLexicalScope(actualTailPosition);
                if (tailNodeScope != null) {
                    tailOrigin = tailNodeScope.getSymbolsOrigin();
                }
            }
            if (tailOrigin != null) {
                this.setTailOrigin(tailOrigin);
            }
        }
    }

// TODO uncomment after propagateContext being removed
//
//    public void resolveRelations(@NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics) {
//        this.queryContent.resolveObjectAndRowsReferences(context, statistics);
//        this.queryContent.resolveValueRelations(context.makeEmptyTuple(), statistics);
//    }

    /**
     * Returns nested node of the query model for the specified offset in the source text
     */
    @NotNull
    public SQLQueryNodeModel findNodeContaining(int textOffset) {
        SQLQueryNodeModel node = this;
        SQLQueryNodeModel nested = node.findChildNodeContaining(textOffset);
        while (nested != null) {
            node = nested;
            nested = nested.findChildNodeContaining(textOffset);
        }
        return node;
    }

    public record LexicalContextResolutionResult(
        int textOffset,
        SQLQueryDataContext nearestResultContext,
        SQLQueryDataContext deepestContext,
        SQLQueryLexicalScopeItem lexicalItem,
        SQLQuerySymbolOrigin symbolsOrigin
    ) {
    }

    /**
     * Returns nested node of the query model for the specified offset in the source text
     */
    @NotNull
    public LexicalContextResolutionResult findLexicalContext(int textOffset) {
        return this.queryContent != null && this.queryContent.getGivenDataContext() == null
            ? this.findLexicalContextNew(textOffset)
            : this.findLexicalContextOld(textOffset);
    }


    @NotNull
    private LexicalContextResolutionResult findLexicalContextNew(int textOffset) {
        ListNode<SQLQueryNodeModel> stack = ListNode.of(this);
        { // walk down through the model till the deepest node describing given position
            SQLQueryNodeModel node = this;
            SQLQueryNodeModel nested = node.findChildNodeContaining(textOffset);
            while (nested != null) {
                stack = ListNode.push(stack, nested);
                nested = nested.findChildNodeContaining(textOffset);
            }
        }

        SQLQueryLexicalScopeItem lexicalItem = null;
        SQLQueryLexicalScope scope = null;

        // walk up till the lexical scope covering given position
        // TODO consider corner-cases with adjacent scopes, maybe better use condition on lexicalItem!=null instead of the scope?
        while (stack != null && scope == null) {
            SQLQueryNodeModel node = stack.data;
            scope = node.findLexicalScope(textOffset);
            if (scope != null) {
                lexicalItem = scope.findNearestItem(textOffset);
            }
            stack = stack.next;
        }

        if (lexicalItem == null) {
            // table refs are not registered in lexical scopes properly for now (because rowsets model being build bottom-to-top),
            // so trying to find their components in the global list
            int index = STMUtils.binarySearchByKey(this.lexicalItems, n -> n.getSyntaxNode().getRealInterval().a, textOffset - 1, Comparator.comparingInt(x -> x));
            if (index < 0) {
                index = ~index - 1;
            }
            if (index >= 0) {
                SQLQueryLexicalScopeItem item = lexicalItems.get(index);
                Interval interval = item.getSyntaxNode().getRealInterval();
                if (interval.a < textOffset && interval.b + 1 >= textOffset) {
                    lexicalItem = item;
                }
            }
        }

        SQLQuerySymbolOrigin symbolsOrigin = lexicalItem == null ? null : lexicalItem.getOrigin();
        if (symbolsOrigin == null && textOffset > this.getInterval().b) {
            symbolsOrigin = this.getTailOrigin();
        }
        if (symbolsOrigin == null && scope != null) {
            symbolsOrigin = scope.getSymbolsOrigin();
        }

        return new LexicalContextResolutionResult(textOffset, this.dataContext, this.dataContext, lexicalItem, symbolsOrigin);
    }

    @NotNull
    private LexicalContextResolutionResult findLexicalContextOld(int textOffset) {
        ListNode<SQLQueryNodeModel> stack = ListNode.of(this);
        SQLQueryDataContext nearestResultContext = this.getResultDataContext();
        SQLQueryDataContext deepestContext;
        { // walk down through the model till the deepest node describing given position
            SQLQueryNodeModel node = this;
            SQLQueryNodeModel nested = node.findChildNodeContaining(textOffset);
            while (nested != null) {
                if (nested.getResultDataContext() instanceof SQLQueryPureResultTupleContext resultTupleContext) {
                    nearestResultContext = resultTupleContext;
                }
                stack = ListNode.push(stack, nested);
                node = nested;
                nested = nested.findChildNodeContaining(textOffset);
            }
            deepestContext = node.getGivenDataContext();
        }

        SQLQueryDataContext context = null;
        SQLQueryLexicalScopeItem lexicalItem = null;
        SQLQueryLexicalScope scope = null;

        // walk up till the lexical scope covering given position
        // TODO consider corner-cases with adjacent scopes, maybe better use condition on lexicalItem!=null instead of the scope?
        while (stack != null && scope == null) {
            SQLQueryNodeModel node = stack.data;
            scope = node.findLexicalScope(textOffset);
            if (scope != null) {
                if (scope.getSymbolsOrigin() != null) {
                    context = scope.getSymbolsOrigin().getDataContext();
                }
                lexicalItem = scope.findNearestItem(textOffset);
            }
            stack = stack.next;
        }

        // if context was not provided by the lexical scope, use one from the deepest model node
        if (context == null) {
            context = deepestContext;
        }

        if (lexicalItem == null) {
            // table refs are not registered in lexical scopes properly for now (because rowsets model being build bottom-to-top),
            // so trying to find their components in the global list
            int index = STMUtils.binarySearchByKey(this.lexicalItems, n -> n.getSyntaxNode().getRealInterval().a, textOffset - 1, Comparator.comparingInt(x -> x));
            if (index < 0) {
                index = ~index - 1;
            }
            if (index >= 0) {
                SQLQueryLexicalScopeItem item = lexicalItems.get(index);
                Interval interval = item.getSyntaxNode().getRealInterval();
                if (interval.a < textOffset && interval.b + 1 >= textOffset) {
                    lexicalItem = item;
                }
            }
        }

        SQLQuerySymbolOrigin symbolsOrigin = lexicalItem == null ? null : lexicalItem.getOrigin();
        if (symbolsOrigin == null && textOffset > this.getInterval().b) {
            symbolsOrigin = this.getTailOrigin();
        }
        if (symbolsOrigin == null && scope != null) {
            symbolsOrigin = scope.getSymbolsOrigin();
        }

        return new LexicalContextResolutionResult(textOffset, nearestResultContext, context, lexicalItem, symbolsOrigin);
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T arg) {
        return visitor.visitSelectionModel(this, arg);
    }
}
