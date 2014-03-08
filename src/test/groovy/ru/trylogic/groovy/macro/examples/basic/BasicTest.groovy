package ru.trylogic.groovy.macro.examples.basic

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*

import static org.codehaus.groovy.ast.expr.VariableExpression.*;

@CompileStatic // yep, it's ok with CompileStatic ;)
class BasicTest extends GroovyShellTestCase {
    public void testMethod() {

        def someVariable = new VariableExpression("someVariable");

        ReturnStatement result = macro {
            return new NonExistingClass($v{someVariable});
        }

        def expected = new ReturnStatement(new ConstructorCallExpression(ClassHelper.make("NonExistingClass"),new ArgumentListExpression(someVariable)));

        assertSyntaxTree(expected, result);
    }

    public void testInception() {

        ConstructorCallExpression result = macro {
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
                ] as List<Statement>,
                new VariableScope()
        )

        assertSyntaxTree(expected, result);
    }
    
    protected void assertSyntaxTree(Object expected, Object result) {
        AstAssert.assertSyntaxTree([expected], [result])
    }
}
