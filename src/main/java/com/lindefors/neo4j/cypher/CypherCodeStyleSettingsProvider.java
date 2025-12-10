package com.lindefors.neo4j.cypher;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleConfigurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CypherCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
    @Override
    public @NotNull String getConfigurableDisplayName() {
        return CypherLanguage.INSTANCE.getDisplayName();
    }

    @Override
    public @NotNull CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings,
                                                             @NotNull CodeStyleSettings originalSettings) {
        return new CodeStyleAbstractConfigurable(settings, originalSettings, getConfigurableDisplayName()) {
            @Override
            protected @NotNull CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings settings) {
                return new CypherCodeStyleMainPanel(getCurrentSettings(), settings);
            }

            @Override
            public @Nullable String getHelpTopic() {
                return null;
            }
        };
    }

    @Override
    public @Nullable Language getLanguage() {
        return CypherLanguage.INSTANCE;
    }

    private static class CypherCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
        CypherCodeStyleMainPanel(@NotNull CodeStyleSettings currentSettings, @NotNull CodeStyleSettings settings) {
            super(CypherLanguage.INSTANCE, currentSettings, settings);
        }

        @Override
        protected void initTabs(@NotNull CodeStyleSettings settings) {
            addIndentOptionsTab(settings);
            addSpacesTab(settings);
            addBlankLinesTab(settings);
            addWrappingAndBracesTab(settings);
        }
    }
}
