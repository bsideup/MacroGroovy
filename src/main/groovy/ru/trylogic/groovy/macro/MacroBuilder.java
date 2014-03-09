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

    public <T> T macro(String source, final Map<SubstitutionKey, Closure<Expression>> context, Class<T> resultClass) {
        final String label = "__synthesized__label__" + System.currentTimeMillis()+ "__:";
        final String labelledSource = label + source;
        final int additionalLines = 1;
        
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

                        if(!call.getMethodAsString().equals(MacroTransformation.DOLLAR_VALUE)) {
                            return super.transform(expression);
                        }

                        SubstitutionKey key = new SubstitutionKey(
                                call.getLineNumber() - additionalLines,
                                call.getColumnNumber() - (call.getLineNumber() == additionalLines ? label.length() + 1 : 0),
                                call.getLastLineNumber() - additionalLines,
                                call.getLastColumnNumber() - (call.getLastLineNumber() == additionalLines ? label.length() + 1 : 0)
                        );
                        
                        Closure<Expression> subtitutionClosure = context.get(key);

                        return subtitutionClosure.call();
                    }

                    @Override
                    protected SourceUnit getSourceUnit() {
                        return null; // Could be null if there is no errors
                    }
                }).visitBlockStatement(closureBlock);

                return (T) getMacroValue(closureBlock);
            }
        }
        return null;
    }

    public static Object getMacroValue(BlockStatement closureBlock) {
        if(closureBlock.getStatements().size() == 1) {
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
