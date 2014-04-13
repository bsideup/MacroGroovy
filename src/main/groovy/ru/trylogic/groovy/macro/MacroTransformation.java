package ru.trylogic.groovy.macro;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

@GroovyASTTransformation(phase =  CompilePhase.CONVERSION)
public class MacroTransformation extends ClassCodeVisitorSupport implements ASTTransformation {
    
    public static final String DOLLAR_VALUE = "$v";
    public static final String MACRO_METHOD = "macro";

    SourceUnit source;
    AstBuilderInvocationTrap transformer;
    
    public void visit(ASTNode[] nodes, SourceUnit source) {
        this.source = source;

        transformer = new AstBuilderInvocationTrap(
                source.getAST().getImports(),
                source.getAST().getStarImports(),
                source.getSource(),
                source);
        
        for(ASTNode node : nodes) {
            if(node instanceof ClassNode) {
                visitClass((ClassNode) node);
            } else if(node instanceof ModuleNode) {
                ModuleNode moduleNode = (ModuleNode) node;
                for (ClassNode classNode : moduleNode.getClasses()) {
                    visitClass(classNode);
                }
            }
        }
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression call) {
        transformer.visitMethodCallExpression(call);
        super.visitMethodCallExpression(call);
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return source;
    }

    @Override
    public void visitClass(ClassNode node) {
        super.visitClass(node);

        java.io.StringWriter writer = new java.io.StringWriter();
        groovy.inspect.swingui.AstNodeToScriptVisitor visitor = new groovy.inspect.swingui.AstNodeToScriptVisitor(writer);
        visitor.visitClass(node);
        System.out.println(writer.toString());
    }
}



