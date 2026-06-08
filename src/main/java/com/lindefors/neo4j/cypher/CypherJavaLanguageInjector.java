package com.lindefors.neo4j.cypher;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Injects Cypher into Java string literals that flow into Neo4j driver's {@code run(...)} methods.
 */
public class CypherJavaLanguageInjector implements MultiHostInjector {

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!(context instanceof PsiLiteralExpression literal)
                || !(literal instanceof PsiLanguageInjectionHost host)
                || !(literal.getValue() instanceof String)
                || !isCypherQuerySource(literal)) {
            return;
        }

        registrar.startInjecting(CypherLanguage.INSTANCE)
                .addPlace(null, null, host, ElementManipulators.getValueTextRange(literal))
                .doneInjecting();
    }

    @Override
    public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return List.of(PsiLiteralExpression.class);
    }

    private static boolean isCypherQuerySource(PsiLiteralExpression literal) {
        if (isNeo4jRunArgument(literal)) {
            return true;
        }

        PsiVariable variable = initializedVariable(literal);
        return variable != null && flowsToNeo4jRun(variable, new HashSet<>());
    }

    private static boolean flowsToNeo4jRun(PsiVariable variable, Set<PsiVariable> visited) {
        if (!visited.add(variable)) {
            return false;
        }

        List<PsiReferenceExpression> references = referencesInContainingFile(variable);
        if (references.stream().anyMatch(PsiUtil::isAccessedForWriting)) {
            return false;
        }

        for (PsiReferenceExpression reference : references) {
            if (isNeo4jRunArgument(reference)) {
                return true;
            }

            PsiVariable alias = initializedVariable(reference);
            if (alias != null && flowsToNeo4jRun(alias, visited)) {
                return true;
            }
        }
        return false;
    }

    private static List<PsiReferenceExpression> referencesInContainingFile(PsiVariable variable) {
        return ReferencesSearch.search(variable, new LocalSearchScope(variable.getContainingFile())).findAll().stream()
                .map(PsiReference::getElement)
                .filter(PsiReferenceExpression.class::isInstance)
                .map(PsiReferenceExpression.class::cast)
                .toList();
    }

    private static PsiVariable initializedVariable(PsiExpression expression) {
        PsiElement unwrapped = PsiUtil.skipParenthesizedExprUp(expression);
        PsiVariable variable = PsiTreeUtil.getParentOfType(unwrapped, PsiVariable.class, true);
        if (variable == null) {
            return null;
        }

        return PsiUtil.skipParenthesizedExprDown(variable.getInitializer()) == expression ? variable : null;
    }

    private static boolean isNeo4jRunArgument(PsiExpression expression) {
        PsiElement unwrapped = PsiUtil.skipParenthesizedExprUp(expression);
        if (!(unwrapped.getParent() instanceof PsiExpressionList arguments)
                || !(arguments.getParent() instanceof PsiMethodCallExpression call)) {
            return false;
        }
        PsiExpression[] argumentExpressions = arguments.getExpressions();
        if (argumentExpressions.length == 0 || argumentExpressions[0] != unwrapped) {
            return false;
        }

        PsiMethod method = call.resolveMethod();
        PsiClass containingClass = method == null ? null : method.getContainingClass();
        String qualifiedName = containingClass == null ? null : containingClass.getQualifiedName();
        if (method != null) {
            return "run".equals(method.getName())
                    && qualifiedName != null
                    && qualifiedName.startsWith("org.neo4j.driver.");
        }
        return isUnresolvedNeo4jRun(call);
    }

    private static boolean isUnresolvedNeo4jRun(PsiMethodCallExpression call) {
        PsiReferenceExpression methodExpression = call.getMethodExpression();
        if (!"run".equals(methodExpression.getReferenceName())) {
            return false;
        }

        PsiExpression qualifier = methodExpression.getQualifierExpression();
        PsiType qualifierType = qualifier == null ? null : qualifier.getType();
        if (qualifierType == null || !(call.getContainingFile() instanceof PsiJavaFile javaFile)) {
            return false;
        }

        String typeName = qualifierType.getCanonicalText();
        if (typeName.startsWith("org.neo4j.driver.")) {
            return true;
        }

        PsiImportList importList = javaFile.getImportList();
        if (importList == null) {
            return false;
        }
        for (PsiImportStatementBase importStatement : importList.getAllImportStatements()) {
            if (importStatement instanceof PsiImportStaticStatement) {
                continue;
            }
            PsiJavaCodeReferenceElement reference = importStatement.getImportReference();
            String importedName = reference == null ? null : reference.getQualifiedName();
            boolean driverPackage = "org.neo4j.driver".equals(importedName)
                    || importedName != null && importedName.startsWith("org.neo4j.driver.");
            if (!driverPackage) {
                continue;
            }
            if (importStatement.isOnDemand() || importedName.endsWith("." + typeName)) {
                return true;
            }
        }
        return false;
    }
}
