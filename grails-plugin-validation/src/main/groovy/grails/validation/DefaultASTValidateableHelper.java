/*
 * Copyright 2011 the original author or authors.
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
package grails.validation;

import static org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY;
import static org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector.ZERO_PARAMETERS;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.grails.compiler.injection.ASTErrorsHelper;
import org.codehaus.groovy.grails.compiler.injection.ASTValidationErrorsHelper;
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator;
import org.codehaus.groovy.grails.web.context.ServletContextHolder;
import org.codehaus.groovy.grails.web.plugins.support.ValidationSupport;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class DefaultASTValidateableHelper implements ASTValidateableHelper{

    private static final String CONSTRAINED_PROPERTIES_PROPERTY_NAME = "$constraints";
    private static final String VALIDATE_METHOD_NAME = "validate";
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");

    public void injectValidateableCode(ClassNode classNode) {
        ASTErrorsHelper errorsHelper = new ASTValidationErrorsHelper();
        errorsHelper.injectErrorsCode(classNode);
        addConstraintsField(classNode);
        addStaticInitializer(classNode);
        addGetConstraintsMethod(classNode);
        addValidateMethod(classNode);
    }

    protected void addConstraintsField(final ClassNode classNode) {
        FieldNode field = classNode.getField(CONSTRAINED_PROPERTIES_PROPERTY_NAME);
        if (field == null || !field.getDeclaringClass().equals(classNode)) {
            classNode.addField(CONSTRAINED_PROPERTIES_PROPERTY_NAME,
                Modifier.STATIC | Modifier.PRIVATE, new ClassNode(Map.class),
                new ConstantExpression(null));
        }
    }

    private void addStaticInitializer(final ClassNode classNode) {
        final Expression nullOutConstrainedPropertiesExpression = new BinaryExpression(
                new VariableExpression(CONSTRAINED_PROPERTIES_PROPERTY_NAME),
                Token.newSymbol(Types.EQUALS, 0, 0), new ConstantExpression(null));
        List<Statement> statements = new ArrayList<Statement>();
        statements.add(new ExpressionStatement(nullOutConstrainedPropertiesExpression));
        classNode.addStaticInitializerStatements(statements, true);
    }

    protected void addGetConstraintsMethod(final ClassNode classNode) {
        final String getConstraintsMethodName = "getConstraints";
        MethodNode getConstraintsMethod = classNode.getMethod(getConstraintsMethodName, ZERO_PARAMETERS);
        if (getConstraintsMethod == null || !getConstraintsMethod.getDeclaringClass().equals(classNode)) {
            final BooleanExpression isConstraintsPropertyNull = new BooleanExpression(new BinaryExpression(new VariableExpression(CONSTRAINED_PROPERTIES_PROPERTY_NAME), Token.newSymbol(
                        Types.COMPARE_EQUAL, 0, 0), new ConstantExpression(null)));

            final String servletContextHolderVariableName = "$sch";
            final String applicationContextVariableName = "$ctx";
            final String constraintsEvaluatorVariableName = "$evaluator";
            final String evaluateMethodName = "evaluate";

            final BlockStatement ifConstraintsPropertyIsNullBlockStatement = new BlockStatement();
            final Expression declareServletContextExpression = new DeclarationExpression(new VariableExpression(servletContextHolderVariableName, ClassHelper.OBJECT_TYPE), Token.newSymbol(Types.EQUALS, 0, 0), new StaticMethodCallExpression(new ClassNode(ServletContextHolder.class), "getServletContext", new ArgumentListExpression()));
            final Expression declareApplicationContextExpression = new DeclarationExpression(new VariableExpression(applicationContextVariableName, ClassHelper.OBJECT_TYPE), Token.newSymbol(Types.EQUALS, 0, 0), new StaticMethodCallExpression(new ClassNode(WebApplicationContextUtils.class), "getWebApplicationContext", new ArgumentListExpression(new VariableExpression(servletContextHolderVariableName))));
            final Expression declareConstraintsEvaluatorExpression = new DeclarationExpression(new VariableExpression(constraintsEvaluatorVariableName, ClassHelper.OBJECT_TYPE), Token.newSymbol(Types.EQUALS, 0, 0), new MethodCallExpression(new VariableExpression(applicationContextVariableName), "getBean", new ArgumentListExpression(new ConstantExpression(ConstraintsEvaluator.BEAN_NAME))));
            final Expression initializeConstraintsFieldExpression = new BinaryExpression(new VariableExpression(CONSTRAINED_PROPERTIES_PROPERTY_NAME), Token.newSymbol(Types.EQUALS, 0, 0), new MethodCallExpression(new VariableExpression(constraintsEvaluatorVariableName), evaluateMethodName, new ArgumentListExpression(THIS_EXPRESSION)));
            final Statement ifConstraintsPropertyIsNullStatement = new IfStatement(isConstraintsPropertyNull, ifConstraintsPropertyIsNullBlockStatement, new ExpressionStatement(new EmptyExpression()));

            ifConstraintsPropertyIsNullBlockStatement.addStatement(new ExpressionStatement(declareServletContextExpression));
            ifConstraintsPropertyIsNullBlockStatement.addStatement(new ExpressionStatement(declareApplicationContextExpression));
            ifConstraintsPropertyIsNullBlockStatement.addStatement(new ExpressionStatement(declareConstraintsEvaluatorExpression));
            ifConstraintsPropertyIsNullBlockStatement.addStatement(new ExpressionStatement(initializeConstraintsFieldExpression));

            final BlockStatement methodBlockStatement = new BlockStatement();
            methodBlockStatement.addStatement(ifConstraintsPropertyIsNullStatement);

            final Statement returnStatement = new ReturnStatement(new VariableExpression(CONSTRAINED_PROPERTIES_PROPERTY_NAME));
            methodBlockStatement.addStatement(returnStatement);

            final MethodNode methodNode = new MethodNode(getConstraintsMethodName, Modifier.STATIC | Modifier.PUBLIC, new ClassNode(Map.class), ZERO_PARAMETERS, null, methodBlockStatement);
            if (classNode.redirect() == null) {
                classNode.addMethod(methodNode);
            } else {
                classNode.redirect().addMethod(methodNode);
            }
        }
    }

    protected void addValidateMethod(final ClassNode classNode) {
        String fieldsToValidateParameterName = "$fieldsToValidate";
        final MethodNode listArgValidateMethod = classNode.getMethod(VALIDATE_METHOD_NAME, new Parameter[]{new Parameter(new ClassNode(List.class), fieldsToValidateParameterName)});
        if (listArgValidateMethod == null) {
            final BlockStatement validateMethodCode = new BlockStatement();
            final ArgumentListExpression validateInstanceArguments = new ArgumentListExpression();
            validateInstanceArguments.addExpression(THIS_EXPRESSION);
            validateInstanceArguments.addExpression(new VariableExpression(fieldsToValidateParameterName));
            final ClassNode validationSupportClassNode = new ClassNode(ValidationSupport.class);
            final StaticMethodCallExpression invokeValidateInstanceExpression = new StaticMethodCallExpression(validationSupportClassNode, "validateInstance", validateInstanceArguments);
            validateMethodCode.addStatement(new ExpressionStatement(invokeValidateInstanceExpression));
            final Parameter fieldsToValidateParameter = new Parameter(new ClassNode(List.class), fieldsToValidateParameterName);
            classNode.addMethod(new MethodNode(
                  VALIDATE_METHOD_NAME, Modifier.PUBLIC, ClassHelper.boolean_TYPE,
                  new Parameter[]{fieldsToValidateParameter}, EMPTY_CLASS_ARRAY, validateMethodCode));
        }
        final MethodNode noArgValidateMethod = classNode.getMethod(VALIDATE_METHOD_NAME,ZERO_PARAMETERS);
        if (noArgValidateMethod == null) {
            final BlockStatement validateMethodCode = new BlockStatement();

            final ArgumentListExpression validateInstanceArguments = new ArgumentListExpression();
            validateInstanceArguments.addExpression(new CastExpression(new ClassNode(List.class), new ConstantExpression(null)));
            final Expression callListArgValidateMethod = new MethodCallExpression(THIS_EXPRESSION, VALIDATE_METHOD_NAME, validateInstanceArguments);
            validateMethodCode.addStatement(new ReturnStatement(callListArgValidateMethod));
            classNode.addMethod(new MethodNode(
                  VALIDATE_METHOD_NAME, Modifier.PUBLIC, ClassHelper.boolean_TYPE,
                  ZERO_PARAMETERS, EMPTY_CLASS_ARRAY, validateMethodCode));
        }
    }
}
