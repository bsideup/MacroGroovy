package ru.trylogic.groovy.macro;

import groovy.lang.Closure;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.StringReaderSource;

import java.util.List;
import java.util.Map;

public class MacroExtensionMethods {
    public static Object macro(Object self, Closure cl) {
        return null;
    }

    public static Object macro(Object self, String source, final Map<String, Closure<Expression>> context) {
        String labelledSource = "__synthesized__label__" + System.currentTimeMillis()+ "__:" + source;
        List<ASTNode> nodes = (new AstBuilder()).buildFromString(CompilePhase.CONVERSION, true, labelledSource);

        final StringReaderSource readerSource = new StringReaderSource(labelledSource, new CompilerConfiguration());
        
        for(ASTNode node : nodes) {
            if (node instanceof BlockStatement) {
                
                BlockStatement closureBlock = (BlockStatement) ((BlockStatement)node).getStatements().get(0);

                (new ClassCodeExpressionTransformer() {
                    public Expression transform(Expression expression) {
                        if(!(expression instanceof MethodCallExpression)) {
                            return super.transform(expression);
                        }
                        
                        MethodCallExpression call = (MethodCallExpression) expression;
                        
                        if(!call.getMethodAsString().equals("$")) {
                            return super.transform(expression);
                        }

                        ArgumentListExpression callArguments = (ArgumentListExpression) call.getArguments();
                        ClosureExpression subtitutionClosureExpression = (ClosureExpression) callArguments.getExpressions().get(0);

                        Closure<Expression> subtitutionClosure = context.get(AstBuilderInvocationTrap.convertClosureToSource(readerSource, subtitutionClosureExpression));
                        
                        return subtitutionClosure.call();
                    }

                    @Override
                    protected SourceUnit getSourceUnit() {
                        return null; // Could be null if there is no errors
                    }
                }).visitBlockStatement(closureBlock);
                
                return closureBlock.getStatements().get(0);
            }
        }
        return null;
    }

}
