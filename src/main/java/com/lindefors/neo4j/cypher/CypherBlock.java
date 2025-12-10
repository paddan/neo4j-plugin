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
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CypherBlock extends AbstractBlock {
    private final @Nullable SpacingBuilder spacingBuilder;
    private final Indent indent;

    protected CypherBlock(@NotNull ASTNode node,
                          @Nullable Wrap wrap,
                          @Nullable Alignment alignment,
                          @NotNull Indent indent,
                          @Nullable SpacingBuilder spacingBuilder) {
        super(node, wrap, alignment);
        this.spacingBuilder = spacingBuilder;
        this.indent = indent;
    }

    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = new ArrayList<>();
        ASTNode child = myNode.getFirstChildNode();
        int braceBalance = 0;
        while (child != null) {
            if (child.getElementType() == TokenType.WHITE_SPACE) {
                child = child.getTreeNext();
                continue;
            }
            if (child.getElementType() == CypherTokenTypes.BRACE_CLOSE && braceBalance > 0) {
                braceBalance--;
            }
            Indent childIndent = braceBalance > 0 ? CypherIndents.normal() : CypherIndents.none();
            Wrap childWrap = spacingBuilder == null ? null : Wrap.createWrap(WrapType.NONE, false);
            blocks.add(new CypherBlock(child, childWrap, null, childIndent, spacingBuilder));
            if (child.getElementType() == CypherTokenTypes.BRACE_OPEN) {
                braceBalance++;
            }
            child = child.getTreeNext();
        }
        return blocks;
    }

    @Override
    public @Nullable Spacing getSpacing(Block child1, @NotNull Block child2) {
        Spacing keywordSpacing = keywordSpacing(child1, child2);
        if (keywordSpacing != null) {
            return keywordSpacing;
        }
        Spacing relationshipSpacing = relationshipSpacing(child1, child2);
        if (relationshipSpacing != null) {
            return relationshipSpacing;
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
        return new ChildAttributes(CypherIndents.continuationWithoutFirst(), null);
    }

    @Override
    public boolean isLeaf() {
        return myNode.getFirstChildNode() == null;
    }

    @Override
    public Indent getIndent() {
        return indent;
    }

    @Override
    public ASTNode getNode() {
        return myNode;
    }

    private @Nullable Spacing keywordSpacing(Block left, Block right) {
        ASTNode rightNode = extractNode(right);
        if (rightNode == null || rightNode.getElementType() != CypherTokenTypes.KEYWORD) {
            return null;
        }

        String keyword = rightNode.getText().toUpperCase(Locale.ENGLISH);
        ASTNode leftNode = extractNode(left);
        String leftKeyword = leftNode != null && leftNode.getElementType() == CypherTokenTypes.KEYWORD
                ? leftNode.getText().toUpperCase(Locale.ENGLISH)
                : null;

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
        if (leftBraceOpen || rightBraceClose) {
            return SINGLE_SPACE;
        }

        return null;
    }

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
        return "-".equals(text) || "->".equals(text) || "<-".equals(text) || "--".equals(text);
    }

    private @Nullable ASTNode extractNode(Block block) {
        if (block instanceof CypherBlock) {
            return ((CypherBlock) block).getNode();
        }
        return null;
    }

    private static final Set<String> CLAUSE_START_KEYWORDS = Set.of(
            "CALL",
            "CREATE",
            "DELETE",
            "DETACH",
            "FOREACH",
            "LOAD",
            "MATCH",
            "MERGE",
            "OPTIONAL",
            "RETURN",
            "REMOVE",
            "SET",
            "UNION",
            "UNWIND",
            "USE",
            "WITH"
    );

    private static final Set<String> CLAUSE_CONTINUATION_KEYWORDS = Set.of(
            "WHERE",
            "ORDER",
            "BY",
            "SKIP",
            "LIMIT",
            "ON"
    );

    private static final Set<String> INLINE_KEYWORD_PAIRS = Set.of(
            "OPTIONAL MATCH",
            "LOAD CSV",
            "CSV WITH",
            "ORDER BY",
            "ON CREATE",
            "ON MATCH"
    );

    private static final Spacing SINGLE_SPACE = Spacing.createSpacing(1, 1, 0, false, 0);
}
