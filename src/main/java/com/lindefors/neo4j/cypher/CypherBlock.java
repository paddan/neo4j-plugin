package com.lindefors.neo4j.cypher;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.formatting.WrapType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import com.intellij.psi.formatter.common.AbstractBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Formatting block that applies Cypher-aware spacing and indentation rules. Most spacing is delegated
 * to the {@link SpacingBuilder}, but keyword clauses and relationship operators get custom handling
 * to mirror typical Cypher formatting.
 */
public class CypherBlock extends AbstractBlock {
    private final @Nullable SpacingBuilder spacingBuilder;
    private final Indent indent;
    private final int indentSize;
    private final boolean useTabs;
    private final GroupLayout groupLayout;

    protected CypherBlock(@NotNull ASTNode node,
                          @Nullable Wrap wrap,
                          @Nullable Alignment alignment,
                          @NotNull Indent indent,
                          @Nullable SpacingBuilder spacingBuilder,
                          int indentSize,
                          boolean useTabs,
                          @NotNull GroupLayout groupLayout) {
        super(node, wrap, alignment);
        this.spacingBuilder = spacingBuilder;
        this.indent = indent;
        this.indentSize = indentSize;
        this.useTabs = useTabs;
        this.groupLayout = groupLayout;
    }

    /**
     * Builds child blocks while tracking brace depth to assign indentation to nested maps or pattern braces.
     */
    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = new ArrayList<>();
        ASTNode child = myNode.getFirstChildNode();
        int codeBlockBraceDepth = 0;
        Deque<Boolean> braceStack = new ArrayDeque<>();
        int caseBalance = 0;
        while (child != null) {
            if (child.getElementType() == TokenType.WHITE_SPACE) {
                child = child.getTreeNext();
                continue;
            }
            if (child.getElementType() == CypherTokenTypes.BRACE_CLOSE && !braceStack.isEmpty()) {
                boolean counted = braceStack.pop();
                if (counted && codeBlockBraceDepth > 0) {
                    codeBlockBraceDepth--;
                }
            }
            if (isCaseEndKeyword(child) && caseBalance > 0) {
                caseBalance--;
            }
            boolean onCreateOrMatch = isOnCreateOrMatch(child);
            int brokenGroupDepth = groupLayout.brokenDepthForIndent(child);
            int caseIndent = caseBalance > 0 && isCaseBranchKeyword(child) ? CASE_BRANCH_INDENT : 0;
            Indent baseIndent = onCreateOrMatch
                    ? indentForOnAction(codeBlockBraceDepth)
                    : codeBlockBraceDepth > 0 ? indentForBraceDepth(codeBlockBraceDepth) : CypherIndents.none();
            Indent childIndent = (brokenGroupDepth == 0 && caseIndent == 0)
                    ? baseIndent
                    : Indent.getSpaceIndent(baseIndentSpaces(onCreateOrMatch, codeBlockBraceDepth)
                    + brokenGroupDepth * BROKEN_GROUP_INDENT
                    + caseIndent);
            Wrap childWrap = spacingBuilder == null ? null : Wrap.createWrap(WrapType.NONE, false);
            blocks.add(new CypherBlock(child, childWrap, null, childIndent, spacingBuilder, indentSize, useTabs, groupLayout));
            if (child.getElementType() == CypherTokenTypes.BRACE_OPEN) {
                boolean counted = isCodeBlockBrace(child);
                braceStack.push(counted);
                if (counted) {
                    codeBlockBraceDepth++;
                }
            }
            if (isCaseStartKeyword(child)) {
                caseBalance++;
            }
            child = child.getTreeNext();
        }
        return blocks;
    }

    /**
     * Computes spacing between tokens, giving precedence to clause keywords, relationship operators,
     * and brace blocks before deferring to the shared {@link SpacingBuilder}.
     */
    @Override
    public @Nullable Spacing getSpacing(Block child1, @NotNull Block child2) {
        if (child1 == null) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }
        Spacing groupBreakSpacing = groupBreakSpacing(child1, child2);
        if (groupBreakSpacing != null) {
            return groupBreakSpacing;
        }
        Spacing literalCommaSpacing = literalCommaSpacing(child1, child2);
        if (literalCommaSpacing != null) {
            return literalCommaSpacing;
        }
        Spacing caseKeywordSpacing = caseKeywordSpacing(child1, child2);
        if (caseKeywordSpacing != null) {
            return caseKeywordSpacing;
        }
        Spacing keywordSpacing = keywordSpacing(child1, child2);
        if (keywordSpacing != null) {
            return keywordSpacing;
        }
        Spacing relationshipSpacing = relationshipSpacing(child1, child2);
        if (relationshipSpacing != null) {
            return relationshipSpacing;
        }
        Spacing colonSpacing = colonSpacing(child1, child2);
        if (colonSpacing != null) {
            return colonSpacing;
        }
        Spacing braceSpacing = braceSpacing(child1, child2);
        if (braceSpacing != null) {
            return braceSpacing;
        }
        if (spacingBuilder == null) {
            return null;
        }
        return spacingBuilder.getSpacing(this, child1, child2);
    }

    @Override
    public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
        int codeBlockBraceDepth = 0;
        Deque<Boolean> braceStack = new ArrayDeque<>();
        int brokenGroupDepth = 0;
        List<Block> subBlocks = getSubBlocks();
        int scanLimit = Math.min(newChildIndex, subBlocks.size());
        for (int i = 0; i < scanLimit; i++) {
            ASTNode childNode = extractNode(subBlocks.get(i));
            if (childNode == null) {
                continue;
            }
            IElementType type = childNode.getElementType();
            if (type == CypherTokenTypes.BRACE_CLOSE && !braceStack.isEmpty()) {
                boolean counted = braceStack.pop();
                if (counted && codeBlockBraceDepth > 0) {
                    codeBlockBraceDepth--;
                }
            } else if (type == CypherTokenTypes.BRACE_OPEN) {
                boolean counted = isCodeBlockBrace(childNode);
                braceStack.push(counted);
                if (counted) {
                    codeBlockBraceDepth++;
                }
            }
            if (groupLayout.isBrokenGroupStart(childNode)) {
                brokenGroupDepth++;
            } else if (groupLayout.isBrokenGroupEnd(childNode) && brokenGroupDepth > 0) {
                brokenGroupDepth--;
            }
        }
        Indent childIndent = codeBlockBraceDepth > 0 ? indentForBraceDepth(codeBlockBraceDepth) : CypherIndents.none();
        if (brokenGroupDepth > 0) {
            childIndent = Indent.getSpaceIndent(baseIndentSpaces(false, codeBlockBraceDepth)
                    + brokenGroupDepth * BROKEN_GROUP_INDENT);
        }
        return new ChildAttributes(childIndent, null);
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    @Override
    public Indent getIndent() {
        return indent;
    }

    private Indent indentForBraceDepth(int braceDepth) {
        if (braceDepth <= 0) {
            return CypherIndents.none();
        }
        if (useTabs) {
            return braceDepth == 1 ? CypherIndents.normal() : CypherIndents.continuationWithoutFirst();
        }
        int indentSpaces = Math.max(1, indentSize) * braceDepth;
        return Indent.getSpaceIndent(indentSpaces);
    }

    /**
     * Indentation for MERGE actions (ON CREATE/ON MATCH), which should sit two spaces deeper than the
     * surrounding clause regardless of brace depth.
     */
    private Indent indentForOnAction(int braceDepth) {
        int baseIndentSpaces = Math.max(0, indentSizeForBraceDepth(braceDepth));
        return Indent.getSpaceIndent(baseIndentSpaces + ON_ACTION_INDENT);
    }

    private int indentSizeForBraceDepth(int braceDepth) {
        if (braceDepth <= 0) {
            return 0;
        }
        return Math.max(1, indentSize) * braceDepth;
    }

    private int baseIndentSpaces(boolean onAction, int braceDepth) {
        if (onAction) {
            return indentSizeForBraceDepth(braceDepth) + ON_ACTION_INDENT;
        }
        if (braceDepth > 0) {
            return indentSizeForBraceDepth(braceDepth);
        }
        return 0;
    }

    /**
     * Spacing adjustments for sequences of Cypher clause keywords, ensuring new clauses start on a new line
     * and inline combinations (e.g., {@code OPTIONAL MATCH}) remain compact.
     */
    private @Nullable Spacing groupBreakSpacing(Block left, Block right) {
        ASTNode leftNode = extractNode(left);
        ASTNode rightNode = extractNode(right);
        if (leftNode == null || rightNode == null) {
            return null;
        }
        if (groupLayout.isBrokenGroupStart(leftNode) || groupLayout.isBrokenGroupEnd(rightNode)) {
            return Spacing.createSpacing(0, 0, 1, true, 1);
        }
        return null;
    }

    private @Nullable Spacing literalCommaSpacing(Block left, Block right) {
        ASTNode leftNode = extractNode(left);
        ASTNode rightNode = extractNode(right);
        if (leftNode == null || rightNode == null) {
            return null;
        }
        if (leftNode.getElementType() != CypherTokenTypes.COMMA) {
            return null;
        }
        ASTNode groupOpen = groupLayout.innermostGroupOpenContainingOffset(leftNode.getStartOffset());
        if (groupOpen == null || !groupLayout.isBrokenGroupStart(groupOpen)) {
            return null;
        }
        IElementType openType = groupOpen.getElementType();
        if (openType == CypherTokenTypes.BRACKET_OPEN) {
            return Spacing.createSpacing(0, 0, 1, true, 1);
        }
        if (openType == CypherTokenTypes.BRACE_OPEN && !isCodeBlockBrace(groupOpen)) {
            return Spacing.createSpacing(0, 0, 1, true, 1);
        }
        return null;
    }

    private @Nullable Spacing caseKeywordSpacing(Block left, Block right) {
        ASTNode rightNode = extractNode(right);
        if (rightNode == null || rightNode.getElementType() != CypherTokenTypes.KEYWORD) {
            return null;
        }
        String keyword = rightNode.getText().toUpperCase(Locale.ENGLISH);
        if (!("WHEN".equals(keyword) || "ELSE".equals(keyword) || "END".equals(keyword))) {
            return null;
        }
        if (!isInsideCaseExpression(rightNode)) {
            return null;
        }
        return Spacing.createSpacing(0, 0, 1, true, 1);
    }

    private @Nullable Spacing keywordSpacing(Block left, Block right) {
        ASTNode rightNode = extractNode(right);
        if (rightNode == null || rightNode.getElementType() != CypherTokenTypes.KEYWORD) {
            return null;
        }

        String keyword = rightNode.getText().toUpperCase(Locale.ENGLISH);
        if (isOnCreateOrMatch(rightNode)) {
            return Spacing.createSpacing(0, 0, 1, true, 1);
        }
        ASTNode leftNode = extractNode(left);
        String leftKeyword = leftNode != null && leftNode.getElementType() == CypherTokenTypes.KEYWORD
                ? leftNode.getText().toUpperCase(Locale.ENGLISH)
                : null;

        if (isOnActionBodyKeyword(keyword, leftNode)) {
            return SINGLE_SPACE;
        }

        if (leftKeyword != null && INLINE_KEYWORD_PAIRS.contains(leftKeyword + " " + keyword)) {
            return SINGLE_SPACE;
        }

        if (CLAUSE_START_KEYWORDS.contains(keyword)) {
            return Spacing.createSpacing(0, 0, 1, true, 1);
        }

        if (CLAUSE_CONTINUATION_KEYWORDS.contains(keyword)) {
            return SINGLE_SPACE;
        }

        return null;
    }

    private boolean isInsideCaseExpression(@NotNull ASTNode node) {
        int nestedEnds = 0;
        ASTNode current = previousNonWhitespaceOrComment(node);
        while (current != null) {
            if (current.getElementType() == CypherTokenTypes.KEYWORD) {
                String keyword = current.getText().toUpperCase(Locale.ENGLISH);
                if ("END".equals(keyword)) {
                    nestedEnds++;
                } else if ("CASE".equals(keyword)) {
                    if (nestedEnds == 0) {
                        return true;
                    }
                    nestedEnds--;
                }
            }
            current = previousNonWhitespaceOrComment(current);
        }
        return false;
    }

    private boolean isCaseStartKeyword(@NotNull ASTNode node) {
        return node.getElementType() == CypherTokenTypes.KEYWORD
                && "CASE".equalsIgnoreCase(node.getText());
    }

    private boolean isCaseBranchKeyword(@NotNull ASTNode node) {
        if (node.getElementType() != CypherTokenTypes.KEYWORD) {
            return false;
        }
        String keyword = node.getText();
        return "WHEN".equalsIgnoreCase(keyword) || "ELSE".equalsIgnoreCase(keyword);
    }

    private boolean isCaseEndKeyword(@NotNull ASTNode node) {
        return node.getElementType() == CypherTokenTypes.KEYWORD
                && "END".equalsIgnoreCase(node.getText());
    }

    private boolean isOnActionBodyKeyword(@NotNull String keyword, @Nullable ASTNode leftNode) {
        if (!"SET".equals(keyword)) {
            return false;
        }
        return followsOnActionKeyword(leftNode);
    }

    private boolean followsOnActionKeyword(@Nullable ASTNode node) {
        if (node == null || node.getElementType() != CypherTokenTypes.KEYWORD) {
            return false;
        }
        String text = node.getText().toUpperCase(Locale.ENGLISH);
        if (!"CREATE".equals(text) && !"MATCH".equals(text)) {
            return false;
        }
        ASTNode previousKeyword = previousNonWhitespaceOrComment(node);
        return previousKeyword != null
                && previousKeyword.getElementType() == CypherTokenTypes.KEYWORD
                && "ON".equalsIgnoreCase(previousKeyword.getText());
    }

    /**
     * Keeps relationship patterns tight (no spaces around {@code -[]->}) unless crossing pattern boundaries.
     */
    private @Nullable Spacing relationshipSpacing(Block left, Block right) {
        ASTNode leftNode = extractNode(left);
        ASTNode rightNode = extractNode(right);
        if (leftNode == null || rightNode == null) {
            return null;
        }

        if (isRelationshipOperator(leftNode) && isPatternBoundary(rightNode.getElementType())) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }
        if (isPatternBoundary(leftNode.getElementType()) && isRelationshipOperator(rightNode)) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }
        if (isRelationshipOperator(leftNode) && isRelationshipOperator(rightNode)) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }

        return null;
    }

    /**
     * Adds breathing room around code blocks inside braces while keeping empty or inline maps compact.
     */
    private @Nullable Spacing braceSpacing(Block left, Block right) {
        ASTNode leftNode = extractNode(left);
        ASTNode rightNode = extractNode(right);
        if (leftNode == null || rightNode == null) {
            return null;
        }

        boolean leftBraceOpen = leftNode.getElementType() == CypherTokenTypes.BRACE_OPEN;
        boolean rightBraceClose = rightNode.getElementType() == CypherTokenTypes.BRACE_CLOSE;

        boolean codeBlockBrace = (leftBraceOpen && isCodeBlockBrace(leftNode))
                || (rightBraceClose && isCodeBlockBrace(rightNode));

        if (leftBraceOpen && rightBraceClose) {
            return Spacing.createSpacing(0, 0, 0, false, 0);
        }
        if (codeBlockBrace) {
            return Spacing.createSpacing(0, 0, 1, true, 1);
        }
        if (leftBraceOpen) {
            return isMapBrace(leftNode) ? NO_SPACE : SINGLE_SPACE;
        }
        if (rightBraceClose) {
            return isMapBrace(rightNode) ? NO_SPACE : SINGLE_SPACE;
        }
        if (rightNode.getElementType() == CypherTokenTypes.BRACE_OPEN) {
            return SINGLE_SPACE;
        }

        return null;
    }

    /**
     * Determines whether a brace pair contains Cypher clauses, in which case blank lines are preferred.
     */
    private boolean isCodeBlockBrace(@NotNull ASTNode braceNode) {
        ASTNode openingBrace = braceNode.getElementType() == CypherTokenTypes.BRACE_OPEN
                ? braceNode
                : findOpeningBrace(braceNode);
        ASTNode closingBrace = braceNode.getElementType() == CypherTokenTypes.BRACE_CLOSE
                ? braceNode
                : findClosingBrace(braceNode);

        if (openingBrace == null || closingBrace == null) {
            return false;
        }

        return containsClauseKeyword(openingBrace.getTreeNext(), closingBrace);
    }

    private @Nullable ASTNode findOpeningBrace(@NotNull ASTNode braceClose) {
        int braceDepth = 0;
        ASTNode current = braceClose.getTreePrev();
        while (current != null) {
            IElementType type = current.getElementType();
            if (type == CypherTokenTypes.BRACE_CLOSE) {
                braceDepth++;
            } else if (type == CypherTokenTypes.BRACE_OPEN) {
                if (braceDepth == 0) {
                    return current;
                }
                braceDepth--;
            }
            current = current.getTreePrev();
        }
        return null;
    }

    private @Nullable ASTNode findClosingBrace(@NotNull ASTNode braceOpen) {
        int braceDepth = 0;
        ASTNode current = braceOpen.getTreeNext();
        while (current != null) {
            IElementType type = current.getElementType();
            if (type == CypherTokenTypes.BRACE_OPEN) {
                braceDepth++;
            } else if (type == CypherTokenTypes.BRACE_CLOSE) {
                if (braceDepth == 0) {
                    return current;
                }
                braceDepth--;
            }
            current = current.getTreeNext();
        }
        return null;
    }

    /**
     * Scans forward until the closing brace to see if any clause keyword appears within the block.
     */
    private boolean containsClauseKeyword(@Nullable ASTNode startExclusive, @NotNull ASTNode endExclusive) {
        ASTNode current = startExclusive;
        while (current != null && current != endExclusive) {
            if (current.getElementType() == CypherTokenTypes.KEYWORD) {
                String keyword = current.getText().toUpperCase(Locale.ENGLISH);
                if (CLAUSE_START_KEYWORDS.contains(keyword) || CLAUSE_CONTINUATION_KEYWORDS.contains(keyword)) {
                    return true;
                }
            }
            current = current.getTreeNext();
        }
        return false;
    }

    private boolean isOnCreateOrMatch(@NotNull ASTNode node) {
        if (node.getElementType() != CypherTokenTypes.KEYWORD) {
            return false;
        }
        if (!"ON".equalsIgnoreCase(node.getText())) {
            return false;
        }
        ASTNode next = nextNonWhitespaceOrComment(node);
        if (next == null || next.getElementType() != CypherTokenTypes.KEYWORD) {
            return false;
        }
        String nextKeyword = next.getText().toUpperCase(Locale.ENGLISH);
        return "CREATE".equals(nextKeyword) || "MATCH".equals(nextKeyword);
    }

    private @Nullable ASTNode nextNonWhitespaceOrComment(@NotNull ASTNode startExclusive) {
        ASTNode current = startExclusive.getTreeNext();
        while (current != null && isSkippableSpacingNode(current)) {
            current = current.getTreeNext();
        }
        return current;
    }

    private @Nullable ASTNode previousNonWhitespaceOrComment(@NotNull ASTNode startExclusive) {
        ASTNode current = startExclusive.getTreePrev();
        while (current != null && isSkippableSpacingNode(current)) {
            current = current.getTreePrev();
        }
        return current;
    }

    private boolean isSkippableSpacingNode(@NotNull ASTNode node) {
        IElementType type = node.getElementType();
        return type == TokenType.WHITE_SPACE || type == CypherTokenTypes.COMMENT;
    }

    /**
     * Applies map-style spacing for colon inside braces: tight before the colon, padded after.
     */
    private @Nullable Spacing colonSpacing(Block left, Block right) {
        ASTNode leftNode = extractNode(left);
        ASTNode rightNode = extractNode(right);
        if (leftNode == null || rightNode == null) {
            return null;
        }
        boolean leftIsColon = leftNode.getElementType() == CypherTokenTypes.COLON;
        boolean rightIsColon = rightNode.getElementType() == CypherTokenTypes.COLON;
        if (!leftIsColon && !rightIsColon) {
            return null;
        }

        ASTNode colonNode = leftIsColon ? leftNode : rightNode;
        if (!isInsideBraces(colonNode)) {
            return null;
        }

        if (leftIsColon) {
            return SINGLE_SPACE;
        }
        return NO_SPACE;
    }

    private boolean isInsideBraces(@NotNull ASTNode node) {
        int depth = 0;
        ASTNode current = node.getTreePrev();
        while (current != null) {
            IElementType type = current.getElementType();
            if (type == CypherTokenTypes.BRACE_CLOSE) {
                depth++;
            } else if (type == CypherTokenTypes.BRACE_OPEN) {
                if (depth == 0) {
                    return true;
                }
                depth--;
            }
            current = current.getTreePrev();
        }
        return false;
    }

    private boolean isPatternBoundary(IElementType type) {
        return type == CypherTokenTypes.PAREN_OPEN
                || type == CypherTokenTypes.PAREN_CLOSE
                || type == CypherTokenTypes.BRACKET_OPEN
                || type == CypherTokenTypes.BRACKET_CLOSE;
    }

    private boolean isRelationshipOperator(ASTNode node) {
        if (node.getElementType() != CypherTokenTypes.OPERATOR) {
            return false;
        }
        String text = node.getText();
        return "-".equals(text)
                || "->".equals(text)
                || "<-".equals(text)
                || "--".equals(text)
                || "-->".equals(text)
                || "<--".equals(text)
                || "<->".equals(text)
                || "<-->".equals(text);
    }

    private @Nullable ASTNode extractNode(Block block) {
        if (block instanceof CypherBlock) {
            return ((CypherBlock) block).getNode();
        }
        return null;
    }

    private boolean isMapBrace(@NotNull ASTNode braceNode) {
        ASTNode openingBrace = braceNode.getElementType() == CypherTokenTypes.BRACE_OPEN
                ? braceNode
                : findOpeningBrace(braceNode);
        ASTNode closingBrace = braceNode.getElementType() == CypherTokenTypes.BRACE_CLOSE
                ? braceNode
                : findClosingBrace(braceNode);

        if (openingBrace == null || closingBrace == null) {
            return false;
        }

        ASTNode firstContent = nextNonWhitespace(openingBrace, closingBrace);
        if (firstContent == null) {
            return true;
        }
        IElementType type = firstContent.getElementType();
        return type != CypherTokenTypes.KEYWORD
                && type != CypherTokenTypes.PAREN_OPEN
                && type != CypherTokenTypes.BRACKET_OPEN;
    }

    private @Nullable ASTNode nextNonWhitespace(@NotNull ASTNode startExclusive, @NotNull ASTNode limitExclusive) {
        ASTNode current = startExclusive.getTreeNext();
        while (current != null && current != limitExclusive) {
            if (current.getElementType() != TokenType.WHITE_SPACE) {
                return current;
            }
            current = current.getTreeNext();
        }
        return null;
    }

    static final class GroupLayout {
        private static final Set<IElementType> GROUP_OPEN_TOKENS = Set.of(
                CypherTokenTypes.PAREN_OPEN,
                CypherTokenTypes.BRACE_OPEN,
                CypherTokenTypes.BRACKET_OPEN
        );
        private static final Set<IElementType> GROUP_CLOSE_TOKENS = Set.of(
                CypherTokenTypes.PAREN_CLOSE,
                CypherTokenTypes.BRACE_CLOSE,
                CypherTokenTypes.BRACKET_CLOSE
        );

        private final IdentityHashMap<ASTNode, GroupInfo> openGroups;
        private final IdentityHashMap<ASTNode, GroupInfo> closeGroups;
        private final List<GroupInfo> brokenGroups;

        private GroupLayout(IdentityHashMap<ASTNode, GroupInfo> openGroups,
                            IdentityHashMap<ASTNode, GroupInfo> closeGroups,
                            List<GroupInfo> brokenGroups) {
            this.openGroups = openGroups;
            this.closeGroups = closeGroups;
            this.brokenGroups = brokenGroups;
        }

        static GroupLayout forRoot(@NotNull ASTNode root, int maxLineLength) {
            IdentityHashMap<ASTNode, GroupInfo> openGroups = new IdentityHashMap<>();
            IdentityHashMap<ASTNode, GroupInfo> closeGroups = new IdentityHashMap<>();
            List<GroupInfo> topGroups = new ArrayList<>();
            Deque<GroupInfo> stack = new ArrayDeque<>();

            ASTNode current = root.getFirstChildNode();
            while (current != null) {
                IElementType type = current.getElementType();
                if (GROUP_OPEN_TOKENS.contains(type)) {
                    GroupInfo group = new GroupInfo(current);
                    if (!stack.isEmpty()) {
                        stack.peek().children.add(group);
                    } else {
                        topGroups.add(group);
                    }
                    stack.push(group);
                    openGroups.put(current, group);
                } else if (GROUP_CLOSE_TOKENS.contains(type) && !stack.isEmpty()
                        && matches(stack.peek().open, type)) {
                    GroupInfo group = stack.pop();
                    group.close = current;
                    group.computeInlineLength();
                    closeGroups.put(current, group);
                }
                current = current.getTreeNext();
            }

            for (GroupInfo group : openGroups.values()) {
                if (group.close == null) {
                    group.computeInlineLength();
                }
            }

            markBreaks(topGroups, 0, maxLineLength);

            List<GroupInfo> broken = new ArrayList<>();
            collectBroken(topGroups, broken);
            return new GroupLayout(openGroups, closeGroups, broken);
        }

        boolean isBrokenGroupStart(@NotNull ASTNode node) {
            GroupInfo info = openGroups.get(node);
            return info != null && info.broken;
        }

        boolean isBrokenGroupEnd(@NotNull ASTNode node) {
            GroupInfo info = closeGroups.get(node);
            return info != null && info.broken;
        }

        @Nullable ASTNode innermostGroupOpenContainingOffset(int offset) {
            GroupInfo best = null;
            for (GroupInfo info : openGroups.values()) {
                if (info.close == null) {
                    continue;
                }
                if (offset > info.startOffset() && offset < info.endOffset()) {
                    if (best == null || info.span() < best.span()) {
                        best = info;
                    }
                }
            }
            return best == null ? null : best.open;
        }

        int brokenDepthForIndent(@NotNull ASTNode node) {
            int offset = node.getStartOffset();
            int depth = 0;
            for (GroupInfo info : brokenGroups) {
                if (offset > info.startOffset() && offset < info.endOffset()) {
                    depth++;
                }
            }
            return depth;
        }

        private static void markBreaks(List<GroupInfo> groups, int brokenDepth, int maxLineLength) {
            for (GroupInfo group : groups) {
                int available = maxLineLength - brokenDepth * BROKEN_GROUP_INDENT;
                int childDepth = brokenDepth;
                if (group.containsCase || group.inlineLength > available) {
                    group.broken = true;
                    childDepth = brokenDepth + 1;
                }
                markBreaks(group.children, childDepth, maxLineLength);
            }
        }

        private static void collectBroken(List<GroupInfo> groups, List<GroupInfo> output) {
            for (GroupInfo group : groups) {
                if (group.broken) {
                    output.add(group);
                }
                collectBroken(group.children, output);
            }
        }

        private static boolean matches(@NotNull ASTNode open, @NotNull IElementType close) {
            IElementType openType = open.getElementType();
            if (openType == CypherTokenTypes.PAREN_OPEN) {
                return close == CypherTokenTypes.PAREN_CLOSE;
            }
            if (openType == CypherTokenTypes.BRACE_OPEN) {
                return close == CypherTokenTypes.BRACE_CLOSE;
            }
            return openType == CypherTokenTypes.BRACKET_OPEN && close == CypherTokenTypes.BRACKET_CLOSE;
        }

        private static final class GroupInfo {
            private final ASTNode open;
            private ASTNode close;
            private final List<GroupInfo> children = new ArrayList<>();
            private int inlineLength;
            private boolean broken;
            private boolean containsCase;

            private GroupInfo(@NotNull ASTNode open) {
                this.open = open;
            }

            private void computeInlineLength() {
                if (close == null) {
                    inlineLength = Integer.MAX_VALUE;
                    return;
                }
                int length = open.getTextLength() + close.getTextLength();
                ASTNode current = open.getTreeNext();
                ASTNode previous = null;
                while (current != null && current != close) {
                    if (current.getElementType() != TokenType.WHITE_SPACE) {
                        if (previous != null) {
                            length += 1;
                        }
                        if (current.getElementType() == CypherTokenTypes.KEYWORD
                                && "CASE".equalsIgnoreCase(current.getText())) {
                            containsCase = true;
                        }
                        length += current.getTextLength();
                        previous = current;
                    }
                    current = current.getTreeNext();
                }
                inlineLength = length;
            }

            private int startOffset() {
                return open.getStartOffset();
            }

            private int endOffset() {
                if (close == null) {
                    return open.getStartOffset();
                }
                return close.getStartOffset();
            }

            private int span() {
                return Math.max(0, endOffset() - startOffset());
            }
        }
    }

    private static final int BROKEN_GROUP_INDENT = 2;
    private static final int CASE_BRANCH_INDENT = 2;
    private static final Set<String> CLAUSE_START_KEYWORDS = CypherTokenTypes.CLAUSE_START_KEYWORDS;
    private static final Set<String> CLAUSE_CONTINUATION_KEYWORDS = CypherTokenTypes.CLAUSE_CONTINUATION_KEYWORDS;

    private static final Set<String> INLINE_KEYWORD_PAIRS = Set.of(
            "OPTIONAL MATCH",
            "LOAD CSV",
            "CSV WITH",
            "CALL YIELD",
            "WHERE EXISTS",
            "ORDER BY",
            "ON CREATE",
            "ON MATCH"
    );

    private static final int ON_ACTION_INDENT = 2;
    private static final Spacing SINGLE_SPACE = Spacing.createSpacing(1, 1, 0, false, 0);
    private static final Spacing NO_SPACE = Spacing.createSpacing(0, 0, 0, false, 0);
}
