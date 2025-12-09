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
        Spacing relationshipSpacing = relationshipSpacing(child1, child2);
        if (relationshipSpacing != null) {
            return relationshipSpacing;
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
}
