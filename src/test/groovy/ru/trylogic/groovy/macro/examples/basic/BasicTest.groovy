package ru.trylogic.groovy.macro.examples.basic

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*

import static org.codehaus.groovy.ast.expr.VariableExpression.*;

class BasicTest extends GroovyShellTestCase {
    public void testMethod() {

        def someVariable = new VariableExpression("someVariable");

        def result = macro {
            return new NonExistingClass($v{someVariable});
        }

        def expected = new ReturnStatement(new ConstructorCallExpression(ClassHelper.make("NonExistingClass"),new ArgumentListExpression(someVariable)));

        assertSyntaxTree(expected, result);
    }

    public void testInception() {

        def result = macro {
            new NonExistingClass($v{macro {someVariable}});
        }

        def expected = new ConstructorCallExpression(ClassHelper.make("NonExistingClass"),new ArgumentListExpression(new VariableExpression("someVariable")));

        assertSyntaxTree(expected, result);
    }

    public void testBlock() {

        def result = macro {
            println "foo"
            println "bar"
        }

        def expected = new BlockStatement(
                [
                        new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "println", new ArgumentListExpression(new ConstantExpression("foo")))),
                        new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "println", new ArgumentListExpression(new ConstantExpression("bar")))),
                ],
                new VariableScope()
        )

        assertSyntaxTree(expected, result);
    }
    
    protected void assertSyntaxTree(Object expected, Object result) {
        AstAssert.assertSyntaxTree([expected], [result])
    }
}
