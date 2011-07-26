package org.codehaus.groovy.grails.compiler.web;

import static org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY;
import static org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector.ZERO_PARAMETERS;

import java.lang.reflect.Modifier;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

public class ASTBeanPropertyBindingResultHelper implements ASTErrorsHelper {
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    private static final TupleExpression EMPTY_TUPLE = new TupleExpression();

    public void injectErrorsCode(ClassNode classNode) {
        addErrorsField(classNode);
        addInitErrorsMethod(classNode);
        addGetErrorsMethod(classNode);
        addHasErrorsMethod(classNode);
        addSetErrorsMethod(classNode);
        addClearErrorsMethod(classNode);
    }

    protected void addErrorsField(final ClassNode paramTypeClassNode) {
        final ASTNode errorsField = paramTypeClassNode.getField("errors");
        if(errorsField == null) {
            paramTypeClassNode.addField(new FieldNode("errors", Modifier.PUBLIC, new ClassNode(Errors.class), paramTypeClassNode, new ConstantExpression(null)));
        }
    }

    protected void addInitErrorsMethod(final ClassNode paramTypeClassNode) {
        final ASTNode initErrorsMethod = paramTypeClassNode.getMethod("initErrors", ZERO_PARAMETERS);
        if (initErrorsMethod == null) {
            final BlockStatement initErrorsMethodCode = new BlockStatement();

            final BinaryExpression errorsIsNullExpression = new BinaryExpression(new VariableExpression(
                    "errors"), Token.newSymbol(
                    Types.COMPARE_EQUAL, 0, 0), new ConstantExpression(null));

            Expression beanPropertyBindingResultConstructorArgs = new ArgumentListExpression(THIS_EXPRESSION,new ConstantExpression(paramTypeClassNode.getName()));
            final Statement newEvaluatorExpression = new ExpressionStatement(
                    new BinaryExpression(new VariableExpression("errors"),
                            Token.newSymbol(Types.EQUALS, 0, 0),
                            new ConstructorCallExpression(new ClassNode(
                                    BeanPropertyBindingResult.class),
                                    beanPropertyBindingResultConstructorArgs)));
            final Statement initErrorsIfNullStatement = new IfStatement(
                    new BooleanExpression(errorsIsNullExpression), newEvaluatorExpression,
                    new ExpressionStatement(new EmptyExpression()));
            initErrorsMethodCode.addStatement(initErrorsIfNullStatement);
            paramTypeClassNode.addMethod(new MethodNode("initErrors",
                    Modifier.PRIVATE, ClassHelper.VOID_TYPE,
                    ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, initErrorsMethodCode));
        }
    }

    protected void addClearErrorsMethod(final ClassNode paramTypeClassNode) {
        final ASTNode clearErrorsMethod = paramTypeClassNode.getMethod("clearErrors", ZERO_PARAMETERS);
        if (clearErrorsMethod == null) {
            final BlockStatement clearErrorsMethodCode = new BlockStatement();
            Expression nullOutErrorsFieldExpression = new BinaryExpression(new VariableExpression("errors"),
                    Token.newSymbol(Types.EQUALS, 0, 0), new ConstantExpression(null));
            clearErrorsMethodCode.addStatement(new ExpressionStatement(nullOutErrorsFieldExpression));
            paramTypeClassNode.addMethod(new MethodNode("clearErrors",
                    Modifier.PUBLIC, ClassHelper.VOID_TYPE,
                    ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, clearErrorsMethodCode));
        }
    }

    protected void addHasErrorsMethod(final ClassNode paramTypeClassNode) {
        final ASTNode getErrorsMethod = paramTypeClassNode.getMethod("hasErrors", ZERO_PARAMETERS);
        if (getErrorsMethod == null) {
            final BlockStatement hasErrorsMethodCode = new BlockStatement();
            hasErrorsMethodCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "initErrors", EMPTY_TUPLE)));
            final Statement returnStatement = new ReturnStatement(new BooleanExpression(new MethodCallExpression(new VariableExpression("errors"), "hasErrors", EMPTY_TUPLE)));
            hasErrorsMethodCode.addStatement(returnStatement);
            paramTypeClassNode.addMethod(new MethodNode("hasErrors",
                    Modifier.PUBLIC, new ClassNode(Boolean.class),
                    ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, hasErrorsMethodCode));
        }
    }

    protected void addGetErrorsMethod(final ClassNode paramTypeClassNode) {
        final ASTNode getErrorsMethod = paramTypeClassNode.getMethod("getErrors", ZERO_PARAMETERS);
        if (getErrorsMethod == null) {
            final BlockStatement getErrorsMethodCode = new BlockStatement();
            getErrorsMethodCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "initErrors", EMPTY_TUPLE)));
            final Statement returnStatement = new ReturnStatement(new VariableExpression("errors"));
            getErrorsMethodCode.addStatement(returnStatement);
            paramTypeClassNode.addMethod(new MethodNode("getErrors",
                    Modifier.PUBLIC, new ClassNode(Errors.class),
                    ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, getErrorsMethodCode));
        }
    }

    protected void addSetErrorsMethod(final ClassNode paramTypeClassNode) {
        MethodNode setErrorsMethod = paramTypeClassNode.getMethod("setErrors", new Parameter[]{ new Parameter(new ClassNode(Errors.class), "errorsArg")});
        if(setErrorsMethod == null) {
            final Expression assignErrorsExpression = new BinaryExpression(new VariableExpression("errors"),
                    Token.newSymbol(Types.EQUALS, 0, 0), new VariableExpression("errorsArg"));
            setErrorsMethod = new MethodNode("setErrors",
                    Modifier.PUBLIC, ClassHelper.VOID_TYPE,
                    new Parameter[]{new Parameter(new ClassNode(Errors.class), "errorsArg")}, EMPTY_CLASS_ARRAY, new ExpressionStatement(assignErrorsExpression));
            paramTypeClassNode.addMethod(setErrorsMethod);
        }
    }
}
