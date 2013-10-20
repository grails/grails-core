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

import static org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils.*
import grails.transaction.Transactional
import groovy.transform.CompileStatic

import java.lang.reflect.Modifier

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector
import org.codehaus.groovy.grails.orm.support.GrailsTransactionTemplate
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
    public static final ClassNode MY_TYPE = new ClassNode(Transactional)
    private static final String PROPERTY_TRANSACTION_MANAGER = "transactionManager"
    private static final String METHOD_EXECUTE = "execute"

    @Override
    void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException('Internal error: wrong types: $node.class / $parent.class')
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1]
        AnnotationNode annotationNode = (AnnotationNode) astNodes[0]
        if (!MY_TYPE.equals(annotationNode.getClassNode())) {
            return
        }

        if (parent instanceof MethodNode) {
            MethodNode methodNode = (MethodNode)parent

            final declaringClassNode = methodNode.getDeclaringClass()

            weaveTransactionManagerAware(source, declaringClassNode)
            weaveTransactionalMethod(source, declaringClassNode, annotationNode, methodNode)
        }
        else if (parent instanceof ClassNode) {
            weaveTransactionalBehavior(source, parent, annotationNode)
        }
    }

    void weaveTransactionalBehavior(SourceUnit source, ClassNode classNode, AnnotationNode annotationNode) {
        weaveTransactionManagerAware(source, classNode)

        ClassNode controllerMethodAnn = getAnnotationClassNode("grails.web.controllers.ControllerMethod")

        for (MethodNode md in new ArrayList<MethodNode>(classNode.getMethods())) {
            if (Modifier.isPublic(md.modifiers) && !Modifier.isAbstract(md.modifiers)) {
                if (md.getAnnotations(MY_TYPE)) continue

                if (controllerMethodAnn && md.getAnnotations(controllerMethodAnn)) continue
                weaveTransactionalMethod(source, classNode, annotationNode, md)
            }
        }
    }

    ClassNode getAnnotationClassNode(String annotationName) {
        try {
            return new ClassNode(Thread.currentThread().contextClassLoader.loadClass(annotationName))
        } catch (e) {
            return null
        }
    }

    protected void weaveTransactionalMethod(SourceUnit source, ClassNode classNode, AnnotationNode annotationNode, MethodNode methodNode) {
        if (isApplied(methodNode, getClass())) {
            return
        }
        markApplied(methodNode, getClass())

        MethodCallExpression originalMethodCall = moveOriginalCodeToNewMethod(source, classNode, methodNode)

        BlockStatement methodBody = new BlockStatement()

        final transactionAttributeClassNode = ClassHelper.make(RuleBasedTransactionAttribute)
        final transactionAttributeVar = new VariableExpression('$transactionAttribute', transactionAttributeClassNode)
        methodBody.addStatement(
            new ExpressionStatement(
                new DeclarationExpression(
                    transactionAttributeVar,
                    GrailsASTUtils.ASSIGNMENT_OPERATOR,
                    new ConstructorCallExpression(transactionAttributeClassNode, GrailsASTUtils.ZERO_ARGUMENTS)
                )
            )
        )

        // remove possible @CS & @TC SKIP annotation from original method node
        removeCompileStaticAnnotations(methodNode)
        // add @CS annotation to original method node
        addCompileStaticAnnotation(methodNode)

        applyTransactionalAttributeSettings(annotationNode, transactionAttributeVar, methodBody)

        final executeMethodParameterTypes = [new Parameter(ClassHelper.make(TransactionStatus), "transactionStatus")] as Parameter[]
        final callCallExpression = new ClosureExpression(executeMethodParameterTypes, new ExpressionStatement(originalMethodCall))

        final constructorArgs = new ArgumentListExpression()
        constructorArgs.addExpression(new PropertyExpression(buildThisExpression(), PROPERTY_TRANSACTION_MANAGER))
        constructorArgs.addExpression(transactionAttributeVar)
        final transactionTemplateClassNode = ClassHelper.make(GrailsTransactionTemplate)
        final transactionTemplateVar = new VariableExpression('$transactionTemplate', transactionTemplateClassNode)
        methodBody.addStatement(
            new ExpressionStatement(
                new DeclarationExpression(
                    transactionTemplateVar,
                    GrailsASTUtils.ASSIGNMENT_OPERATOR,
                    new ConstructorCallExpression(transactionTemplateClassNode, constructorArgs)
                )
            )
        )

        final methodArgs = new ArgumentListExpression()
        methodArgs.addExpression(callCallExpression)
        final executeMethodCallExpression = new MethodCallExpression(transactionTemplateVar, METHOD_EXECUTE, methodArgs)
        final executeMethodNode = transactionTemplateClassNode.getMethod("execute", executeMethodParameterTypes)
        executeMethodCallExpression.setMethodTarget(executeMethodNode)

        if (methodNode.getReturnType() != ClassHelper.VOID_TYPE) {
            methodBody.addStatement(new ReturnStatement(new CastExpression(methodNode.getReturnType(), executeMethodCallExpression)))
        } else {
            methodBody.addStatement(new ExpressionStatement(executeMethodCallExpression))
        }

        methodNode.setCode(methodBody)
        processVariableScopes(source, classNode, methodNode)
    }

    protected applyTransactionalAttributeSettings(AnnotationNode annotationNode, VariableExpression transactionAttributeVar, BlockStatement methodBody) {
        final rollbackRuleAttributeClassNode = ClassHelper.make(RollbackRuleAttribute)
        final noRollbackRuleAttributeClassNode = ClassHelper.make(NoRollbackRuleAttribute)
        final members = annotationNode.getMembers()
        int rollbackRuleId = 0
        members.each { String name, Expression expr ->
            if (name == 'rollbackFor' || name == 'rollbackForClassName' || name == 'noRollbackFor' || name == 'noRollbackForClassName') {
                final targetClassNode = (name == 'rollbackFor' || name == 'rollbackForClassName') ? rollbackRuleAttributeClassNode : noRollbackRuleAttributeClassNode
                name = 'rollbackRules'
                if (expr instanceof ListExpression) {
                    for (exprItem in ((ListExpression)expr).expressions) {
                        appendRuleElement(methodBody, transactionAttributeVar, name, new ConstructorCallExpression(targetClassNode, exprItem))
                    }
                } else {
                    appendRuleElement(methodBody, transactionAttributeVar, name, new ConstructorCallExpression(targetClassNode, expr))
                }
            } else {
                if (name == 'isolation') {
                    name = 'isolationLevel'
                    expr = new MethodCallExpression(expr, "value", new ArgumentListExpression())
                } else if (name == 'propagation') {
                    name = 'propagationBehavior'
                    expr = new MethodCallExpression(expr, "value", new ArgumentListExpression())
                }
                methodBody.addStatement(
                    new ExpressionStatement(
                        new BinaryExpression(new PropertyExpression(transactionAttributeVar, name),
                            Token.newSymbol(Types.EQUAL, 0, 0),
                            expr)
                    )
                )
            }
        }
    }

    private appendRuleElement(BlockStatement methodBody, VariableExpression transactionAttributeVar, String name, Expression expr) {
        final rollbackRuleAttributeClassNode = ClassHelper.make(RollbackRuleAttribute)
        ClassNode rollbackRulesListClassNode = nonGeneric(ClassHelper.make(List), rollbackRuleAttributeClassNode)
        methodBody.addStatement(
            new ExpressionStatement(
                new MethodCallExpression(
                    new CastExpression(rollbackRulesListClassNode, buildGetPropertyExpression(transactionAttributeVar, name, transactionAttributeVar.getType())),
                    'add',
                    expr
                )
            )
        )
    }

    protected MethodCallExpression moveOriginalCodeToNewMethod(SourceUnit source, ClassNode classNode, MethodNode methodNode) {
        String renamedMethodName = '$tt__' + methodNode.getName()
        final transactionStatusParameter = new Parameter(ClassHelper.make(TransactionStatus), "transactionStatus")
        def newParameters = methodNode.getParameters() ? (copyParameters(((methodNode.getParameters() as List) + [transactionStatusParameter]) as Parameter[])) : [transactionStatusParameter] as Parameter[]

        MethodNode renamedMethodNode = new MethodNode(
            renamedMethodName,
            Modifier.PROTECTED, methodNode.getReturnType().getPlainNodeReference(),
            newParameters,
            GrailsArtefactClassInjector.EMPTY_CLASS_ARRAY,
            methodNode.code)
        methodNode.setCode(null)
        copyAnnotations(methodNode, renamedMethodNode, null, ["grails.transaction.Transactional"] as Set)
        classNode.addMethod(renamedMethodNode)

        processVariableScopes(source, classNode, renamedMethodNode)

        final originalMethodCall = new MethodCallExpression(new VariableExpression("this"), renamedMethodName, new ArgumentListExpression(renamedMethodNode.parameters))
        originalMethodCall.setImplicitThis(false)
        originalMethodCall.setMethodTarget(renamedMethodNode)

        originalMethodCall
    }

    protected void weaveTransactionManagerAware(SourceUnit source, ClassNode declaringClassNode) {
        ClassNode transactionManagerAwareInterface = ClassHelper.make(TransactionManagerAware)
        if (GrailsASTUtils.findInterface(declaringClassNode, transactionManagerAwareInterface)) {
            return
        }

        declaringClassNode.addInterface(transactionManagerAwareInterface)

        //add the transactionManager property
        final transactionManagerProperty = declaringClassNode.getProperty(PROPERTY_TRANSACTION_MANAGER)
        if (!transactionManagerProperty) {
            declaringClassNode.addProperty(PROPERTY_TRANSACTION_MANAGER, Modifier.PUBLIC, ClassHelper.make(PlatformTransactionManager), null, null, null)
        }
    }
}
