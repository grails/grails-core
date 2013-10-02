/*
 * Copyright 2012 the original author or authors.
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

package org.codehaus.groovy.grails.transaction.transform

import org.codehaus.groovy.grails.orm.support.GrailsTransactionTemplate
import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils
import org.codehaus.groovy.grails.orm.support.TransactionManagerAware
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute
import org.springframework.transaction.interceptor.RollbackRuleAttribute
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute

import java.lang.reflect.Modifier

/**
 * This AST transform reads the {@link grails.transaction.Transactional} annotation and transforms method calls by
 * wrapping the body of the method in an execution of {@link GrailsTransactionTemplate}.
 *
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class TransactionalTransform implements ASTTransformation{
    public static final ClassNode MY_TYPE = new ClassNode(Transactional).getPlainNodeReference();
    private static final String PROPERTY_TRANSACTION_MANAGER = "transactionManager"
    private static final String METHOD_EXECUTE = "execute"

    @Override
    void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException('Internal error: wrong types: $node.class / $parent.class')
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode annotationNode = (AnnotationNode) astNodes[0];
        if (!MY_TYPE.equals(annotationNode.getClassNode())) {
            return;
        }

        if (parent instanceof MethodNode) {
            MethodNode methodNode = (MethodNode)parent


            final declaringClassNode = methodNode.getDeclaringClass()

            weaveTransactionManagerAware(declaringClassNode)

            weaveTransactionalMethod(annotationNode, methodNode)
        }
        else if (parent instanceof ClassNode) {
            weaveTransactionalBehavior(parent, annotationNode)
        }

    }

    public void weaveTransactionalBehavior(ClassNode classNode, AnnotationNode annotationNode) {
        weaveTransactionManagerAware(classNode)

        ClassNode controllerMethodAnn = getAnnotationClassNode("grails.web.controllers.ControllerMethod")


        for (MethodNode md in classNode.methods) {
            if (Modifier.isPublic(md.modifiers) && !Modifier.isAbstract(md.modifiers)) {
                if (md.getAnnotations(MY_TYPE)) continue

                if (controllerMethodAnn && md.getAnnotations(controllerMethodAnn)) continue
                weaveTransactionalMethod(annotationNode, md)
            }
        }
    }

    ClassNode getAnnotationClassNode(String annotationName) {
        try {
            final classLoader = Thread.currentThread().contextClassLoader
            final clazz = classLoader.loadClass(annotationName)
            return new ClassNode(clazz)
        } catch (e) {
            return null
        }
    }

    protected void weaveTransactionalMethod(AnnotationNode annotationNode, MethodNode methodNode) {

        BlockStatement methodBody = new BlockStatement()

        final transactionAttributeVar = new VariableExpression('$transactionAttribute')
        final transactionAttributeClassNode = ClassHelper.make(RuleBasedTransactionAttribute).getPlainNodeReference()
        methodBody.addStatement(
            new ExpressionStatement(
                new DeclarationExpression(
                    transactionAttributeVar,
                    GrailsASTUtils.ASSIGNMENT_OPERATOR,
                    new ConstructorCallExpression(transactionAttributeClassNode, GrailsASTUtils.ZERO_ARGUMENTS)
                )
            )
        )

        final rollbackRuleAttributeClassNode = ClassHelper.make(RollbackRuleAttribute).getPlainNodeReference()
        final noRollbackRuleAttributeClassNode = ClassHelper.make(NoRollbackRuleAttribute).getPlainNodeReference()
        final members = annotationNode.getMembers()
        members.each { String name, Expression expr ->

            Token operator = Token.newSymbol(Types.EQUAL, 0, 0)

            if (name == 'isolation') {
                name = 'isolationLevel'
                expr = new MethodCallExpression(expr, "value", new ArgumentListExpression())
            } else if (name == 'propagation') {
                name = 'propagationBehavior'
                expr = new MethodCallExpression(expr, "value", new ArgumentListExpression())
            } else if (name == 'rollbackFor' || name == 'rollbackForClassName' || name == 'noRollbackFor' || name == 'noRollbackForClassName') {
                final targetClassNode = (name == 'rollbackFor' || name == 'rollbackForClassName') ? rollbackRuleAttributeClassNode : noRollbackRuleAttributeClassNode
                name = 'rollbackRules'
                if (expr instanceof ListExpression) {
                    final closureExpression = new ClosureExpression(
                        GrailsASTUtils.ZERO_PARAMETERS,
                        new ExpressionStatement(new ConstructorCallExpression(targetClassNode, new VariableExpression("it")))
                    )
                    closureExpression.setVariableScope(new VariableScope())
                    operator = Token.newSymbol(Types.PLUS_EQUAL, 0, 0)
                    expr = new MethodCallExpression(expr, "collect", closureExpression)
                } else {
                    operator = Token.newSymbol(Types.LEFT_SHIFT, 0, 0)
                    expr = new ConstructorCallExpression(targetClassNode, expr)
                }
            }

            methodBody.addStatement(
                new ExpressionStatement(
                    new BinaryExpression(new PropertyExpression(transactionAttributeVar, name),
                        operator,
                        expr)
                )
            )
        }

        final methodArgs = new ArgumentListExpression()
        final executeMethodParameterTypes = [new Parameter(ClassHelper.make(TransactionStatus).getPlainNodeReference(), "transactionStatus")] as Parameter[]
        final callCallExpression = new ClosureExpression(executeMethodParameterTypes, methodNode.code)

        final variableScope = new VariableScope()
        for (Parameter p in methodNode.parameters) {
            p.setClosureSharedVariable(true);
            variableScope.putReferencedLocalVariable(p);
        }
        callCallExpression.setVariableScope(variableScope)
        methodArgs.addExpression(callCallExpression)

        final constructorArgs = new ArgumentListExpression()
        constructorArgs.addExpression(new VariableExpression(PROPERTY_TRANSACTION_MANAGER))
        constructorArgs.addExpression(transactionAttributeVar)
        final transactionTemplateVar = new VariableExpression('$transactionTemplate')
        final transactionTemplateClassNode = ClassHelper.make(GrailsTransactionTemplate).getPlainNodeReference()
        methodBody.addStatement(
            new ExpressionStatement(
                new DeclarationExpression(
                    transactionTemplateVar,
                    GrailsASTUtils.ASSIGNMENT_OPERATOR,
                    new ConstructorCallExpression(transactionTemplateClassNode, constructorArgs)
                )
            )
        )

        final executeMethodCallExpression = new MethodCallExpression(transactionTemplateVar, METHOD_EXECUTE, methodArgs)
        final executeMethodNode = transactionTemplateClassNode.getMethod("execute", executeMethodParameterTypes)
        executeMethodCallExpression.setMethodTarget(executeMethodNode)
        methodBody.addStatement(new ExpressionStatement(executeMethodCallExpression))

        methodNode.setCode(methodBody)
    }

    protected void weaveTransactionManagerAware(ClassNode declaringClassNode) {
        ClassNode transactionManagerAwareInterface = ClassHelper.make(TransactionManagerAware).getPlainNodeReference()

        if (!GrailsASTUtils.findInterface(declaringClassNode, transactionManagerAwareInterface)) {
            declaringClassNode.addInterface(transactionManagerAwareInterface)

            //add the transactionManager property
            final transactionManagerProperty = declaringClassNode.getProperty(PROPERTY_TRANSACTION_MANAGER)
            if (!transactionManagerProperty) {
                declaringClassNode.addProperty(PROPERTY_TRANSACTION_MANAGER, Modifier.PUBLIC, ClassHelper.make(PlatformTransactionManager).getPlainNodeReference(), null, null, null);
            }

        }
    }
}
