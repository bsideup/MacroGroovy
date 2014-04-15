package ru.trylogic.groovy.macro;

import groovy.lang.Closure;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.StringReaderSource;

import java.util.List;
import java.util.Map;

public enum MacroBuilder {
    INSTANCE;

    public <T> T macro(boolean asIs, String source, final Map<SubstitutionKey, Closure<Expression>> context, Class<T> resultClass) {
        final String label = "__synthesized__label__" + System.currentTimeMillis()+ "__:";
        final String labelledSource = label + source;
        final int linesOffset = 1;
        final int columnsOffset = label.length() + 1; // +1 because of {
        
        List<ASTNode> nodes = (new AstBuilder()).buildFromString(CompilePhase.CONVERSION, true, labelledSource);

        for(ASTNode node : nodes) {
            if (node instanceof BlockStatement) {

                BlockStatement closureBlock = (BlockStatement) ((BlockStatement)node).getStatements().get(0);

                (new ClassCodeExpressionTransformer() {
                    public Expression transform(Expression expression) {
                        if(!(expression instanceof MethodCallExpression)) {
                            return super.transform(expression);
                        }

                        MethodCallExpression call = (MethodCallExpression) expression;

                        if(!AstBuilderInvocationTrap.isBuildInvocation(call, MacroTransformation.DOLLAR_VALUE)) {
                            return super.transform(expression);
                        }

                        SubstitutionKey key = new SubstitutionKey(call, linesOffset, columnsOffset);
                        
                        return context.get(key).call();
                    }

                    @Override
                    protected SourceUnit getSourceUnit() {
                        return null; // Could be null if there are no errors
                    }
                }).visitBlockStatement(closureBlock);

                return (T) getMacroValue(closureBlock, asIs);
            }
        }
        return null;
    }

    public static ASTNode getMacroValue(BlockStatement closureBlock, boolean asIs) {
        if(!asIs && closureBlock.getStatements().size() == 1) {
            Statement result = closureBlock.getStatements().get(0);
            if(result instanceof ExpressionStatement) {
                return ((ExpressionStatement) result).getExpression();
            } else {
                return result;
            }
        }
        return closureBlock;
    }
}
