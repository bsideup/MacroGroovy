package ru.trylogic.groovy.macro;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;

public class SubstitutionKey {

    private int startLine;
    private int startColumn;
    private int endLine;
    private int endColumn;

    public SubstitutionKey(int startLine, int startColumn, int endLine, int endColumn) {
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }
    
    public SubstitutionKey(Expression expression, int linesOffset, int columnsOffset) {
        this(
                expression.getLineNumber() - linesOffset,
                expression.getColumnNumber() - (expression.getLineNumber() == linesOffset ? columnsOffset : 0),
                expression.getLastLineNumber() - linesOffset,
                expression.getLastColumnNumber() - (expression.getLastLineNumber() == linesOffset ? columnsOffset : 0)
        );
    }
    
    public ConstructorCallExpression toConstructorCallExpression() {
        return new ConstructorCallExpression(
                ClassHelper.make(this.getClass()),
                new ArgumentListExpression(new Expression[] {
                        new ConstantExpression(startLine),
                        new ConstantExpression(startColumn),
                        new ConstantExpression(endLine),
                        new ConstantExpression(endColumn)
                })
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubstitutionKey that = (SubstitutionKey) o;

        if (endColumn != that.endColumn) return false;
        if (endLine != that.endLine) return false;
        if (startColumn != that.startColumn) return false;
        if (startLine != that.startLine) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startLine;
        result = 31 * result + startColumn;
        result = 31 * result + endLine;
        result = 31 * result + endColumn;
        return result;
    }

    @Override
    public String toString() {
        return "SubstitutionKey{" +
                "startLine=" + startLine +
                ", startColumn=" + startColumn +
                ", endLine=" + endLine +
                ", endColumn=" + endColumn +
                '}';
    }
}
