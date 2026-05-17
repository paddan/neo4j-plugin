package com.lindefors.neo4j.cypher;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Uppercases all Cypher keyword tokens after the formatter has run.
 */
public class CypherKeywordCasePostFormatProcessor implements PostFormatProcessor {
    @Override
    public @NotNull PsiElement processElement(@NotNull PsiElement source, @NotNull CodeStyleSettings settings) {
        if (source.getContainingFile() instanceof CypherPsiFile) {
            uppercaseKeywordsInRange(source.getContainingFile(), source.getTextRange());
        }
        return source;
    }

    @Override
    public @NotNull TextRange processText(@NotNull PsiFile source, @NotNull TextRange rangeToReformat, @NotNull CodeStyleSettings settings) {
        if (!(source instanceof CypherPsiFile)) {
            return rangeToReformat;
        }
        uppercaseKeywordsInRange(source, rangeToReformat);
        return rangeToReformat;
    }

    private void uppercaseKeywordsInRange(@NotNull PsiFile file, @NotNull TextRange range) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) {
            return;
        }

        CharSequence text = document.getCharsSequence();
        int startOffset = Math.max(0, range.getStartOffset());
        int endOffset = Math.min(text.length(), range.getEndOffset());
        if (startOffset >= endOffset) {
            return;
        }

        // Scan the current document text with the lexer instead of using PSI node ranges,
        // because PSI may be stale after the formatter has changed whitespace in the document.
        CypherLexer lexer = new CypherLexer();
        lexer.start(text, startOffset, endOffset, 0);

        List<TextRange> toReplace = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenType() == CypherTokenTypes.KEYWORD) {
                int tokenStart = lexer.getTokenStart();
                int tokenEnd = lexer.getTokenEnd();
                String tokenText = text.subSequence(tokenStart, tokenEnd).toString();
                if (!tokenText.equals(tokenText.toUpperCase(Locale.ENGLISH))) {
                    toReplace.add(new TextRange(tokenStart, tokenEnd));
                }
            }
            lexer.advance();
        }

        // Replace in reverse order to keep offsets valid
        for (int i = toReplace.size() - 1; i >= 0; i--) {
            TextRange r = toReplace.get(i);
            String current = document.getText(r);
            document.replaceString(r.getStartOffset(), r.getEndOffset(), current.toUpperCase(Locale.ENGLISH));
        }

        if (!toReplace.isEmpty()) {
            PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
        }
    }
}
