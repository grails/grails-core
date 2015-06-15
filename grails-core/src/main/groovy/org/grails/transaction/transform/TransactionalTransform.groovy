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

package org.grails.transaction.transform

import grails.transaction.Rollback
import groovy.transform.TypeChecked
import org.codehaus.groovy.ast.stmt.Statement

import static org.grails.compiler.injection.GrailsASTUtils.*
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import groovy.transform.CompileStatic

import java.lang.reflect.Modifier

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.grails.compiler.injection.GrailsASTUtils
import grails.compiler.ast.GrailsArtefactClassInjector
import grails.transaction.GrailsTransactionTemplate
import grails.transaction.TransactionManagerAware
import org.grails.transaction.GrailsTransactionAttribute
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute
import org.springframework.transaction.interceptor.RollbackRuleAttribute

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
    public static final ClassNode COMPILE_STATIC_TYPE = ClassHelper.make(CompileStatic)
    public static final ClassNode TYPE_CHECKED_TYPE = ClassHelper.make(TypeChecked)
    private static final String PROPERTY_TRANSACTION_MANAGER = "transactionManager"
    private static final String METHOD_EXECUTE = "execute"
    private static final Set<String> METHOD_NAME_EXCLUDES = new HashSet<String>(Arrays.asList("afterPropertiesSet", "destroy"));
    private static final Set<String> ANNOTATION_NAME_EXCLUDES = new HashSet<String>(Arrays.asList(PostConstruct.class.getName(), PreDestroy.class.getName(), Transactional.class.getName(), Rollback.class.getName(), "grails.web.controllers.ControllerMethod", NotTransactional.class.getName()));

    @Override
    void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException('Internal error: wrong types: $node.class / $parent.class')
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode annotationNode = (AnnotationNode) astNodes[0];
        if (!isTransactionAnnotation(annotationNode)) {
            return;
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

    protected boolean isTransactionAnnotation(AnnotationNode annotationNode) {
        MY_TYPE.equals(annotationNode.getClassNode())
    }

    public void weaveTransactionalBehavior(SourceUnit source, ClassNode classNode, AnnotationNode annotationNode) {
        weaveTransactionManagerAware(source, classNode)
        List<MethodNode> methods = new ArrayList<MethodNode>(classNode.getMethods());
        
        for (MethodNode md in methods) {
            String methodName = md.getName()
            int modifiers = md.modifiers
            if (!md.isSynthetic() && Modifier.isPublic(modifiers) && !Modifier.isAbstract(modifiers) && !Modifier.isStatic(modifiers)) {
                if(hasExcludedAnnotation(md)) continue

                if( methodName.contains('$') && !methodName.startsWith('$spock') ) continue

                if(METHOD_NAME_EXCLUDES.contains(methodName)) continue
                
                if(GrailsASTUtils.isSetterOrGetterMethod(md)) continue
                
                weaveTransactionalMethod(source, classNode, annotationNode, md);
            }
        }
    }

    private boolean hasExcludedAnnotation(MethodNode md) {
        boolean excludedAnnotation = false;
        for (AnnotationNode annotation : md.getAnnotations()) {
            if(ANNOTATION_NAME_EXCLUDES.contains(annotation.getClassNode().getName())) {
                excludedAnnotation = true;
                break;
            }
        }
        excludedAnnotation
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

    protected void weaveTransactionalMethod(SourceUnit source, ClassNode classNode, AnnotationNode annotationNode, MethodNode methodNode) {
        if(isApplied(methodNode, this.getClass())) {
            return
        }
        markApplied(methodNode, this.getClass())
        
        MethodCallExpression originalMethodCall = moveOriginalCodeToNewMethod(source, classNode, methodNode)
        
        BlockStatement methodBody = new BlockStatement()

        final transactionAttributeClassNode = ClassHelper.make(GrailsTransactionAttribute)
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
        
        applyTransactionalAttributeSettings(annotationNode, transactionAttributeVar, methodBody)


        def transactionStatusParam = new Parameter(ClassHelper.make(TransactionStatus), "transactionStatus")
        final executeMethodParameterTypes = [transactionStatusParam] as Parameter[]
        final callCallExpression = new ClosureExpression(executeMethodParameterTypes, createTransactionalMethodCallBody(transactionStatusParam, originalMethodCall))

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
        final executeMethodCallExpression = new MethodCallExpression(transactionTemplateVar, getTransactionTemplateMethodName(), methodArgs)
        final executeMethodParameters = [new Parameter(ClassHelper.make(Closure), null)] as Parameter[]
        final executeMethodNode = transactionTemplateClassNode.getMethod(getTransactionTemplateMethodName(), executeMethodParameters)
        executeMethodCallExpression.setMethodTarget(executeMethodNode)
        
        if(methodNode.getReturnType() != ClassHelper.VOID_TYPE) {
            methodBody.addStatement(new ReturnStatement(new CastExpression(methodNode.getReturnType(), executeMethodCallExpression)))
        } else {
            methodBody.addStatement(new ExpressionStatement(executeMethodCallExpression));
        }
        
        methodNode.setCode(methodBody)
        processVariableScopes(source, classNode, methodNode)
    }

    protected String getTransactionTemplateMethodName() {
        "execute"
    }

    protected Statement createTransactionalMethodCallBody(Parameter transactionStatusParam, MethodCallExpression originalMethodCall) {
        new ExpressionStatement(originalMethodCall)
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
                    for(exprItem in ((ListExpression)expr).expressions) {
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
                methodNode.code
                );

        // GrailsCompileStatic and GrailsTypeChecked are not explicitly addressed
        // here but they will be picked up because they are @AnnotationCollector annotations
        // which use CompileStatic and TypeChecked...
        renamedMethodNode.addAnnotations(methodNode.getAnnotations(COMPILE_STATIC_TYPE))
        renamedMethodNode.addAnnotations(methodNode.getAnnotations(TYPE_CHECKED_TYPE))

        methodNode.setCode(null)
        classNode.addMethod(renamedMethodNode)
        
        processVariableScopes(source, classNode, renamedMethodNode)
        
        final originalMethodCall = new MethodCallExpression(new VariableExpression("this"), renamedMethodName, new ArgumentListExpression(renamedMethodNode.parameters))
        originalMethodCall.setImplicitThis(false)
        originalMethodCall.setMethodTarget(renamedMethodNode)
        
        originalMethodCall
    }

    protected void weaveTransactionManagerAware(SourceUnit source, ClassNode declaringClassNode) {
        ClassNode transactionManagerAwareInterface = ClassHelper.make(TransactionManagerAware)

        if (!GrailsASTUtils.findInterface(declaringClassNode, transactionManagerAwareInterface)) {
            declaringClassNode.addInterface(transactionManagerAwareInterface)

            //add the transactionManager property
            if (!GrailsASTUtils.hasProperty(declaringClassNode, PROPERTY_TRANSACTION_MANAGER)) {

                def transactionManagerClassNode = ClassHelper.make(PlatformTransactionManager)

                def fieldName = '$' + PROPERTY_TRANSACTION_MANAGER
                def field = declaringClassNode.addField(fieldName, Modifier.PROTECTED, transactionManagerClassNode, null)


                def body = new BlockStatement()
                def p = new Parameter(transactionManagerClassNode, PROPERTY_TRANSACTION_MANAGER)
                def parameters = [p] as Parameter[]


                def transactionManagerPropertyExpr = new PropertyExpression(new VariableExpression("this"), fieldName)
                def getterBody = new BlockStatement()

                // this is a hacky workaround that ensures the transaction manager is also set on the spock shared instance which seems to differ for
                // some reason
                if(GrailsASTUtils.isSubclassOf(declaringClassNode, "spock.lang.Specification")) {

                    getterBody.addStatement(new ExpressionStatement(
                            new MethodCallExpression(new PropertyExpression(new PropertyExpression(new VariableExpression("this"), "specificationContext"), "sharedInstance" ),
                                    "setTransactionManager",
                                    transactionManagerPropertyExpr)
                    ))
                }

                getterBody.addStatement( new ReturnStatement(transactionManagerPropertyExpr))
                declaringClassNode.addMethod("getTransactionManager", Modifier.PUBLIC, transactionManagerClassNode, GrailsASTUtils.ZERO_PARAMETERS, null, getterBody)

                def assignment = Token.newSymbol("=", -1, -1)
                body.addStatement(new ExpressionStatement(
                        new BinaryExpression(transactionManagerPropertyExpr,
                                assignment,
                                             new VariableExpression(p)
                )))


                declaringClassNode.addMethod("setTransactionManager", Modifier.PUBLIC, ClassHelper.VOID_TYPE, parameters, null, body)

            }

        }
    }
}
