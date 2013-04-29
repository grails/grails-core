/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.compiler.injection;

import static org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY;
import static org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector.ZERO_PARAMETERS;

import java.lang.reflect.Modifier;

import grails.validation.ValidationErrors;
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
import org.springframework.validation.Errors;

public class ASTValidationErrorsHelper implements ASTErrorsHelper {
    private static final ConstantExpression NULL_EXPRESSION = new ConstantExpression(null);
    private static final String SET_ERRORS_METHOD_NAME = "setErrors";
    private static final String GET_ERRORS_METHOD_NAME = "getErrors";
    private static final String HAS_ERRORS_METHOD_NAME = "hasErrors";
    private static final String CLEAR_ERRORS_METHOD_NAME = "clearErrors";
    private static final String INIT_ERRORS_METHOD_NAME = "initErrors";
    private static final String ERRORS_PROPERTY_NAME = "errors";
    private static final Token EQUALS_SYMBOL = Token.newSymbol(Types.EQUALS, 0, 0);
    private static final ClassNode ERRORS_CLASS_NODE = new ClassNode(Errors.class);
    private static final VariableExpression ERRORS_EXPRESSION = new VariableExpression(ERRORS_PROPERTY_NAME);
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    private static final TupleExpression EMPTY_TUPLE = new TupleExpression();
    private static final MethodCallExpression INIT_ERRORS_METHOD_CALL_EXPRESSION = new MethodCallExpression(THIS_EXPRESSION, INIT_ERRORS_METHOD_NAME, EMPTY_TUPLE);

    public void injectErrorsCode(ClassNode classNode) {
        addErrorsField(classNode);
        addInitErrorsMethod(classNode);
        addGetErrorsMethod(classNode);
        addHasErrorsMethod(classNode);
        addSetErrorsMethod(classNode);
        addClearErrorsMethod(classNode);
    }

    protected void addErrorsField(final ClassNode paramTypeClassNode) {
        final ASTNode errorsField = paramTypeClassNode.getField(ERRORS_PROPERTY_NAME);
        if (errorsField == null) {
            paramTypeClassNode.addField(new FieldNode(ERRORS_PROPERTY_NAME, Modifier.PUBLIC,
                    ERRORS_CLASS_NODE, paramTypeClassNode, NULL_EXPRESSION));
        }
    }

    protected void addInitErrorsMethod(final ClassNode paramTypeClassNode) {
        final ASTNode initErrorsMethod = paramTypeClassNode.getMethod(INIT_ERRORS_METHOD_NAME, ZERO_PARAMETERS);
        if (initErrorsMethod == null) {
            final BlockStatement initErrorsMethodCode = new BlockStatement();

            final BinaryExpression errorsIsNullExpression = new BinaryExpression(ERRORS_EXPRESSION, Token.newSymbol(
                    Types.COMPARE_EQUAL, 0, 0), NULL_EXPRESSION);

            Expression beanPropertyBindingResultConstructorArgs = new ArgumentListExpression(
                    THIS_EXPRESSION, new ConstantExpression(paramTypeClassNode.getName()));
            final Statement newEvaluatorExpression = new ExpressionStatement(
                    new BinaryExpression(ERRORS_EXPRESSION,
                            EQUALS_SYMBOL,
                            new ConstructorCallExpression(new ClassNode(
                                    ValidationErrors.class),
                                    beanPropertyBindingResultConstructorArgs)));
            final Statement initErrorsIfNullStatement = new IfStatement(
                    new BooleanExpression(errorsIsNullExpression), newEvaluatorExpression,
                    new ExpressionStatement(new EmptyExpression()));
            initErrorsMethodCode.addStatement(initErrorsIfNullStatement);
            paramTypeClassNode.addMethod(new MethodNode(INIT_ERRORS_METHOD_NAME,
                    Modifier.PRIVATE, ClassHelper.VOID_TYPE,
                    ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, initErrorsMethodCode));
        }
    }

    protected void addClearErrorsMethod(final ClassNode paramTypeClassNode) {
        final ASTNode clearErrorsMethod = paramTypeClassNode.getMethod(CLEAR_ERRORS_METHOD_NAME, ZERO_PARAMETERS);
        if (clearErrorsMethod == null) {
            final BlockStatement clearErrorsMethodCode = new BlockStatement();
            Expression nullOutErrorsFieldExpression = new BinaryExpression(ERRORS_EXPRESSION,
                    EQUALS_SYMBOL, NULL_EXPRESSION);
            clearErrorsMethodCode.addStatement(new ExpressionStatement(nullOutErrorsFieldExpression));
            paramTypeClassNode.addMethod(new MethodNode(CLEAR_ERRORS_METHOD_NAME,
                    Modifier.PUBLIC, ClassHelper.VOID_TYPE,
                    ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, clearErrorsMethodCode));
        }
    }

    protected void addHasErrorsMethod(final ClassNode paramTypeClassNode) {
        final ASTNode getErrorsMethod = paramTypeClassNode.getMethod(HAS_ERRORS_METHOD_NAME, ZERO_PARAMETERS);
        if (getErrorsMethod == null) {
            final BlockStatement hasErrorsMethodCode = new BlockStatement();
            hasErrorsMethodCode.addStatement(new ExpressionStatement(INIT_ERRORS_METHOD_CALL_EXPRESSION));
            final Statement returnStatement = new ReturnStatement(new BooleanExpression(new MethodCallExpression(ERRORS_EXPRESSION, HAS_ERRORS_METHOD_NAME, EMPTY_TUPLE)));
            hasErrorsMethodCode.addStatement(returnStatement);
            paramTypeClassNode.addMethod(new MethodNode(HAS_ERRORS_METHOD_NAME,
                    Modifier.PUBLIC, new ClassNode(Boolean.class),
                    ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, hasErrorsMethodCode));
        }
    }

    protected void addGetErrorsMethod(final ClassNode paramTypeClassNode) {
        final ASTNode getErrorsMethod = paramTypeClassNode.getMethod(GET_ERRORS_METHOD_NAME, ZERO_PARAMETERS);
        if (getErrorsMethod == null) {
            final BlockStatement getErrorsMethodCode = new BlockStatement();
            getErrorsMethodCode.addStatement(new ExpressionStatement(INIT_ERRORS_METHOD_CALL_EXPRESSION));
            final Statement returnStatement = new ReturnStatement(ERRORS_EXPRESSION);
            getErrorsMethodCode.addStatement(returnStatement);
            paramTypeClassNode.addMethod(new MethodNode(GET_ERRORS_METHOD_NAME,
                    Modifier.PUBLIC, ERRORS_CLASS_NODE,
                    ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, getErrorsMethodCode));
        }
    }

    protected void addSetErrorsMethod(final ClassNode paramTypeClassNode) {
        final String errorsArgumentName = "$errorsArg";
        MethodNode setErrorsMethod = paramTypeClassNode.getMethod(SET_ERRORS_METHOD_NAME,
             new Parameter[] { new Parameter(ERRORS_CLASS_NODE, errorsArgumentName)});
        if (setErrorsMethod == null) {
            final Expression assignErrorsExpression = new BinaryExpression(ERRORS_EXPRESSION,
                    EQUALS_SYMBOL, new VariableExpression(errorsArgumentName));
            setErrorsMethod = new MethodNode(SET_ERRORS_METHOD_NAME,
                    Modifier.PUBLIC, ClassHelper.VOID_TYPE,
                    new Parameter[]{new Parameter(ERRORS_CLASS_NODE, errorsArgumentName)}, EMPTY_CLASS_ARRAY, new ExpressionStatement(assignErrorsExpression));
            paramTypeClassNode.addMethod(setErrorsMethod);
        }
    }
}
