package ru.trylogic.groovy.macro;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.ReaderSource;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;

import java.util.ArrayList;
import java.util.List;

import static org.codehaus.groovy.ast.expr.VariableExpression.THIS_EXPRESSION;

/*
    This class was copy-pasted from Groovy lang sources because it's private.
    
    TODO send pull request to groovy-core to make it public
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
     * @param macroCall the method call expression that may or may not be an AstBuilder 'from code' invocation.
     */
    public void visitMethodCallExpression(final MethodCallExpression macroCall) {

        if (!isBuildInvocation(macroCall, MacroTransformation.MACRO_METHOD)) {
            return;
        }

        // is one of the arguments a closure?
        Expression macroCallArguments = macroCall.getArguments();
        if (macroCallArguments == null) {
            addError("Macro closure should have arguments", macroCall);
            return;
        }
        
        if(!(macroCallArguments instanceof TupleExpression)) {
            addError("Macro closure should have TupleExpression as arguments", macroCallArguments);
            return;
        }
        
        TupleExpression tupleArguments = (TupleExpression) macroCallArguments;

        if (tupleArguments.getExpressions() == null) {
            addError("Macro closure arguments should have expressions", tupleArguments);
            return;
        }
        
        if(tupleArguments.getExpressions().size() != 1) {
            addError("Macro closure arguments should have exactly one argument", tupleArguments);
            return;
        }

        if (!(tupleArguments.getExpression(0) instanceof ClosureExpression)) {
            addError("Macro closure argument should be a closure", tupleArguments.getExpression(0));
            return;
        }

        final ClosureExpression closureExpression = (ClosureExpression) tupleArguments.getExpression(0);
        
        if(closureExpression.getParameters() != null && closureExpression.getParameters().length > 0) {
            addError("Macro closure arguments are not allowed", closureExpression);
        }
        
        final MapExpression mapExpression = new MapExpression();
        
        (new CodeVisitorSupport() {
            @Override
            public void visitMethodCallExpression(MethodCallExpression call) {
                super.visitMethodCallExpression(call);
                
                if(call.getMethodAsString().equalsIgnoreCase(MacroTransformation.DOLLAR_VALUE)) {
                    ArgumentListExpression callArguments = (ArgumentListExpression) call.getArguments();
                    ClosureExpression substitutionClosure = (ClosureExpression) callArguments.getExpressions().get(0);

                    SubstitutionKey key = new SubstitutionKey(call, closureExpression.getLineNumber(), closureExpression.getColumnNumber());
                    
                    ConstructorCallExpression keyExpression = key.toConstructorCallExpression();

                    mapExpression.addMapEntryExpression(keyExpression, substitutionClosure);

                    callArguments.getExpressions().add(keyExpression);
                }
            }
        }).visitClosureExpression(closureExpression);
        
        List<Expression> otherArgs = new ArrayList<Expression>();
        String source = convertClosureToSource(this.source, closureExpression);

        BlockStatement closureBlock = (BlockStatement) closureExpression.getCode();

        otherArgs.add(new ConstantExpression(source));
        otherArgs.add(mapExpression);
        otherArgs.add(new ClassExpression(ClassHelper.makeWithoutCaching(MacroBuilder.getMacroValue(closureBlock).getClass(), false)));
        macroCall.setArguments(new ArgumentListExpression(otherArgs));
        macroCall.setObjectExpression(new PropertyExpression(new ClassExpression(ClassHelper.makeWithoutCaching(MacroBuilder.class, false)), "INSTANCE"));
        macroCall.setSpreadSafe(false);
        macroCall.setSafe(false);
        macroCall.setImplicitThis(false);
    }

    /**
     * Looks for method calls on the AstBuilder class called build that take
     * a Closure as parameter. This is all needed b/c build is overloaded.
     *
     * @param call the method call expression, may not be null
     */
    public static boolean isBuildInvocation(MethodCallExpression call, String methodName) {
        if (call == null) throw new IllegalArgumentException("Null: call");
        if(methodName == null) throw new IllegalArgumentException("Null: methodName");
        
        if(!(call.getMethod() instanceof ConstantExpression)) {
            return false;
        }
        
        if(!(methodName.equals(call.getMethodAsString()))) {
            return false;
        }

        // is method object correct type?
        return call.getObjectExpression() == THIS_EXPRESSION;
    }

    /**
     * Converts a ClosureExpression into the String source.
     *
     * @param expression a closure
     * @return the source the closure was created from
     */
    private String convertClosureToSource(ReaderSource source, ClosureExpression expression) {
        if (expression == null) throw new IllegalArgumentException("Null: expression");

        StringBuilder result = new StringBuilder();
        for (int x = expression.getLineNumber(); x <= expression.getLastLineNumber(); x++) {
            String line = source.getLine(x, null);
            if (line == null) {
                addError(
                        "Error calculating source code for expression. Trying to read line " + x + " from " + source.getClass(),
                        expression
                );
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


        String resultSource = result.toString();//.trim();
        if (!resultSource.startsWith("{")) {
            addError(
                    "Error converting ClosureExpression into source code. Closures must start with {. Found: " + source,
                    expression
            );
        }

        return resultSource;
    }
}
