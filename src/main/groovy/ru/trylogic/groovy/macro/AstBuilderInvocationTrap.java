package ru.trylogic.groovy.macro;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.ReaderSource;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;

import java.util.ArrayList;
import java.util.List;

import static org.codehaus.groovy.ast.expr.VariableExpression.THIS_EXPRESSION;

/**
 * This class traps invocations of AstBuilder.build(CompilePhase, boolean, Closure) and converts
 * the contents of the closure into expressions by reading the source of the Closure and sending
 * that as a String to AstBuilder.build(String, CompilePhase, boolean) at runtime.
 */
public class AstBuilderInvocationTrap {

    private final ReaderSource source;
    private final SourceUnit sourceUnit;

    /**
     * Creates the trap and captures all the ways in which a class may be referenced via imports.
     *
     * @param imports        all the imports from the source
     * @param importPackages all the imported packages from the source
     * @param source         the reader source that contains source for the SourceUnit
     * @param sourceUnit     the source unit being compiled. Used for error messages.
     */
    AstBuilderInvocationTrap(List<ImportNode> imports, List<ImportNode> importPackages, ReaderSource source, SourceUnit sourceUnit) {
        if (source == null) throw new IllegalArgumentException("Null: source");
        if (sourceUnit == null) throw new IllegalArgumentException("Null: sourceUnit");
        this.source = source;
        this.sourceUnit = sourceUnit;
    }

    /**
     * Reports an error back to the source unit.
     *
     * @param msg  the error message
     * @param expr the expression that caused the error message.
     */
    private void addError(String msg, ASTNode expr) {
        sourceUnit.getErrorCollector().addErrorAndContinue(
                new SyntaxErrorMessage(new SyntaxException(msg + '\n', expr.getLineNumber(), expr.getColumnNumber(), expr.getLastLineNumber(), expr.getLastColumnNumber()), sourceUnit)
        );
    }


    /**
     * Attempts to find AstBuilder 'from code' invocations. When found, converts them into calls
     * to the 'from string' approach.
     *
     * @param call the method call expression that may or may not be an AstBuilder 'from code' invocation.
     */
    public void visitMethodCallExpression(MethodCallExpression call) {

        if (!isBuildInvocation(call)) {
            return;
        }

        ClosureExpression closureExpression = getClosureArgument(call);
        
        final MapExpression mapExpression = new MapExpression();
        
        (new CodeVisitorSupport() {
            @Override
            public void visitMethodCallExpression(MethodCallExpression call) {
                super.visitMethodCallExpression(call);
                
                if(call.getMethodAsString().equalsIgnoreCase(MacroTransformation.DOLLAR_VALUE)) {
                    ArgumentListExpression callArguments = (ArgumentListExpression) call.getArguments();
                    ClosureExpression substitutionClosure = (ClosureExpression) callArguments.getExpressions().get(0);
                    String substitutionClosureSource = convertClosureToSource(source, substitutionClosure);

                    ConstantExpression keyExpression = new ConstantExpression(substitutionClosureSource);

                    mapExpression.addMapEntryExpression(keyExpression, substitutionClosure);

                    callArguments.getExpressions().add(keyExpression);
                }
            }
        }).visitClosureExpression(closureExpression);
        
        List<Expression> otherArgs = new ArrayList<Expression>();
        String source = convertClosureToSource(this.source, closureExpression);

        otherArgs.add(new ConstantExpression(source));
        otherArgs.add(mapExpression);
        call.setArguments(new ArgumentListExpression(otherArgs));
        call.setObjectExpression(new PropertyExpression(new ClassExpression(ClassHelper.makeWithoutCaching(MacroBuilder.class, false)), "INSTANCE"));
        call.setSpreadSafe(false);
        call.setSafe(false);
        call.setImplicitThis(false);
    }

    private ClosureExpression getClosureArgument(MethodCallExpression call) {

        if (call.getArguments() instanceof TupleExpression) {
            for (ASTNode node : ((TupleExpression) call.getArguments()).getExpressions()) {
                if (node instanceof ClosureExpression) {
                    return (ClosureExpression) node;
                }
            }
        }
        return null;
    }

    /**
     * Looks for method calls on the AstBuilder class called build that take
     * a Closure as parameter. This is all needed b/c build is overloaded.
     *
     * @param call the method call expression, may not be null
     */
    private boolean isBuildInvocation(MethodCallExpression call) {
        if (call == null) throw new IllegalArgumentException("Null: call");

        // is method name correct?
        if (call.getMethod() instanceof ConstantExpression && MacroTransformation.MACRO_METHOD.equals(((ConstantExpression) call.getMethod()).getValue())) {

            // is method object correct type?
            if (call.getObjectExpression() == THIS_EXPRESSION) {
                // is one of the arguments a closure?
                if (call.getArguments() != null && call.getArguments() instanceof TupleExpression) {
                    if (((TupleExpression) call.getArguments()).getExpressions() != null) {
                        for (ASTNode node : ((TupleExpression) call.getArguments()).getExpressions()) {
                            if (node instanceof ClosureExpression) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Converts a ClosureExpression into the String source.
     *
     * @param expression a closure
     * @return the source the closure was created from
     */
    public static String convertClosureToSource(ReaderSource source, ClosureExpression expression) {
        if (expression == null) throw new IllegalArgumentException("Null: expression");

        StringBuilder result = new StringBuilder();
        for (int x = expression.getLineNumber(); x <= expression.getLastLineNumber(); x++) {
            String line = source.getLine(x, null);
            if (line == null) {
                /*FIXME
                addError(
                        "Error calculating source code for expression. Trying to read line " + x + " from " + source.getClass(),
                        expression
                );
                */
            }
            if (x == expression.getLastLineNumber()) {
                line = line.substring(0, expression.getLastColumnNumber() - 1);
            }
            if (x == expression.getLineNumber()) {
                line = line.substring(expression.getColumnNumber() - 1);
            }
            //restoring line breaks is important b/c of lack of semicolons
            result.append(line).append('\n');
        }


        String resultSource = result.toString().trim();
        if (!resultSource.startsWith("{")) {
            /*FIXME
            addError(
                    "Error converting ClosureExpression into source code. Closures must start with {. Found: " + source,
                    expression
            );
            */
        }

        return resultSource;
    }
}
