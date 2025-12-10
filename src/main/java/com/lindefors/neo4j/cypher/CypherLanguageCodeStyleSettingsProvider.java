package com.lindefors.neo4j.cypher;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;

public class CypherLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
    private static final String CODE_SAMPLE = """
            // Sample Cypher query
            MATCH (u:User {id: $userId})-[:FRIEND]->(friend)
            WHERE friend.active = true
            RETURN friend.name, friend.age ORDER BY friend.age DESC LIMIT 10
            """;

    @Override
    public @NotNull Language getLanguage() {
        return CypherLanguage.INSTANCE;
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        consumer.showAllStandardOptions();
    }

    @Override
    public @NotNull String getCodeSample(@NotNull SettingsType settingsType) {
        return CODE_SAMPLE;
    }

    @Override
    public @NotNull CommonCodeStyleSettings getDefaultCommonSettings() {
        CommonCodeStyleSettings settings = new CommonCodeStyleSettings(getLanguage());
        CommonCodeStyleSettings.IndentOptions indentOptions = settings.initIndentOptions();
        indentOptions.INDENT_SIZE = 4;
        indentOptions.CONTINUATION_INDENT_SIZE = 4;
        indentOptions.TAB_SIZE = 4;
        return settings;
    }
}
