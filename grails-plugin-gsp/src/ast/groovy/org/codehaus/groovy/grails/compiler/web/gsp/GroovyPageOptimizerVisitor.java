package org.codehaus.groovy.grails.compiler.web.gsp;

import groovy.lang.Closure;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.util.GrailsPrintWriter;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

import java.util.List;
import java.util.Stack;

/**
 * Scan MethodCallExpression in GSP to convert callsite calls into static calls (printHtmlPart ...)
 *
 * @author Stephane Maldini
 * @since 1.4
 */
class GroovyPageOptimizerVisitor extends CodeVisitorSupport {

    private static final ClassNode groovyPageClassNode = new ClassNode(GroovyPage.class);
    private static final MethodNode writerMethodNode = new ClassNode(GrailsPrintWriter.class).getMethod("print",
            new Parameter[]{new Parameter(new ClassNode(Object.class), "obj")});

    private static final String THIS_RECEIVER = "this";
    private static final String THISOBJECT = "thisObject";
    private static final String OUT_RECEIVER = "out";
    private static final String PRINT_METHOD = "print";
    private static final String CODECOUT_RECEIVER = "codecOut";

    private Stack<ClosureExpression> innerClosures = new Stack<ClosureExpression>();
    private ClassNode targetGroovyPageNode;

    private DeclarationExpression thisObjectDeclaration;
    private VariableExpression thisObjectVariable;

    public GroovyPageOptimizerVisitor(ClassNode targetGroovyPage) {
        this.targetGroovyPageNode = targetGroovyPage;

        MethodCallExpression thisObjectMethodCall = new MethodCallExpression(new VariableExpression(THIS_RECEIVER), "getThisObject", MethodCallExpression.NO_ARGUMENTS);
        thisObjectMethodCall.setMethodTarget(new ClassNode(Closure.class).getMethods("getThisObject").get(0));

        thisObjectVariable = new VariableExpression(THISOBJECT, targetGroovyPageNode);

        thisObjectDeclaration = new DeclarationExpression(
                    thisObjectVariable
                    ,Token.newSymbol(Types.EQUALS, 0, 0)
                    ,thisObjectMethodCall);
    }

    @Override
    public void visitClosureExpression(ClosureExpression expression) {
        innerClosures.push(expression);
        introduceThisObjectVariable(expression);
        super.visitClosureExpression(expression);
        innerClosures.pop();
    }

    private void introduceThisObjectVariable(ClosureExpression closureExpression) {
        if (closureExpression.getCode() instanceof BlockStatement) {
            List<Statement> oldBlock = ((BlockStatement)closureExpression.getCode()).getStatements();
            BlockStatement newBlock = new BlockStatement();

            newBlock.addStatement(new ExpressionStatement(thisObjectDeclaration));
            newBlock.addStatements(oldBlock);

            closureExpression.setCode(newBlock);
        }
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression call) {

        if (isCallFromGroovyPageClass(call)) {
            proceedCallFromGroovyPageClass(call);
        } else if (isCallFromOutOrCodecOut(call)) {
            proceedCallFromOutOrCodecOut(call);
        }

        super.visitMethodCallExpression(call);
    }

    private void proceedCallFromOutOrCodecOut(MethodCallExpression call) {
        call.setMethodTarget(writerMethodNode);
    }

    private boolean isCallFromOutOrCodecOut(MethodCallExpression expression) {
        return (expression.getObjectExpression().getText().equals(OUT_RECEIVER)
                || expression.getObjectExpression().getText().equals(CODECOUT_RECEIVER))
                && expression.getMethodAsString().equals(PRINT_METHOD);
    }

    private void proceedCallFromGroovyPageClass(MethodCallExpression call) {
        List<MethodNode> methodNodeList = groovyPageClassNode.getMethods(call.getMethodAsString());

        if (methodNodeList.size() == 1) {
            call.setMethodTarget(methodNodeList.get(0));
            changeThisObjectExpressionIfInnerClosure(call);
        } else if (methodNodeList.size() > 1 && call.getArguments() instanceof ArgumentListExpression) {
            //Special case for invokeTag
            ArgumentListExpression argsExpr = ((ArgumentListExpression) call.getArguments());

            for (MethodNode methodNode : methodNodeList) {
                //No need for deep analysis as GroovyPage doesn't have multiple signatures for same args number
                if (methodNode.getParameters().length == argsExpr.getExpressions().size()) {
                    call.setMethodTarget(methodNode);
                    changeThisObjectExpressionIfInnerClosure(call);
                    break;
                }
            }
        }
    }

    private void changeThisObjectExpressionIfInnerClosure(MethodCallExpression call) {
        if (!innerClosures.isEmpty()) {
            call.setObjectExpression(thisObjectVariable);
        }
    }

    private boolean isCallFromGroovyPageClass(MethodCallExpression expression) {
        return expression.getObjectExpression().getText().equals(THIS_RECEIVER)
                && !groovyPageClassNode.getMethods(expression.getMethodAsString()).isEmpty();
    }
}
