package ru.trylogic.groovy.macro.examples.basic

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*


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
    
    protected void assertSyntaxTree(Object expected, Object result) {
        AstAssert.assertSyntaxTree([expected], [result])
    }
}
