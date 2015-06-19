package org.grails.compiler.injection.test
import grails.boot.config.GrailsApplicationContextLoader
import grails.boot.config.GrailsAutoConfiguration
import grails.test.mixin.integration.Integration
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.compiler.injection.GrailsASTUtils
import org.grails.io.support.MainClassFinder
import org.grails.test.context.junit4.GrailsJunit4ClassRunner
import org.grails.test.context.junit4.GrailsTestConfiguration
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.boot.test.IntegrationTest
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.util.ClassUtils

import java.lang.reflect.Modifier
/*
 * Copyright 2014 original authors
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

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class IntegrationTestMixinTransformation implements ASTTransformation {

    static final ClassNode MY_TYPE = new ClassNode(Integration.class);
    public static final ClassNode CONTEXT_CONFIG_ANNOTATION = ClassHelper.make(ContextConfiguration)
    public static final ClassNode GRAILS_APPLICATION_CONTEXT_LOADER = ClassHelper.make(GrailsApplicationContextLoader)
    public static final ClassNode WEB_APP_CONFIGURATION = ClassHelper.make(WebAppConfiguration)
    public static final ClassNode INTEGRATION_TEST_CLASS_NODE = ClassHelper.make(IntegrationTest)
    public static final ClassNode WEB_INTEGRATION_TEST_CLASS_NODE = ClassHelper.make(WebIntegrationTest)
    public static final ClassNode SPRING_APPLICATION_CONFIGURATION_CLASS_NODE = ClassHelper.make(GrailsTestConfiguration)
    public static final ClassNode RUN_WITH_ANNOTATION_NODE = ClassHelper.make(RunWith)
    public static final ClassNode SPRING_JUNIT4_CLASS_RUNNER = ClassHelper.make(GrailsJunit4ClassRunner)

    @Override
    void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: ${astNodes[0].getClass()} / ${astNodes[1].getClass()}")
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1]
        AnnotationNode annotationNode = (AnnotationNode) astNodes[0]
        if (!MY_TYPE.equals(annotationNode.classNode) || !(parent instanceof ClassNode)) {
            return
        }

        ClassExpression applicationClassExpression = (ClassExpression)annotationNode.getMember('applicationClass')

        final applicationClassNode
        if(applicationClassExpression) {
            applicationClassNode = applicationClassExpression.getType()
            if(!applicationClassNode.isDerivedFrom(ClassHelper.make(GrailsAutoConfiguration))) {
                GrailsASTUtils.error(source, applicationClassExpression, "Invalid applicationClass attribute value [${applicationClassNode.getName()}].  The applicationClass attribute must specify a class which extends grails.boot.config.GrailsAutoConfiguration.", true)
            }
        } else {
            String mainClass = MainClassFinder.searchMainClass(source.source.URI)
            if(mainClass) {
                applicationClassNode = ClassHelper.make(mainClass)
            }
        }

        if(applicationClassNode) {
            ClassNode classNode = (ClassNode) parent

            weaveIntegrationTestMixin(classNode, applicationClassNode)

        }

    }

    public void weaveIntegrationTestMixin(ClassNode classNode, ClassNode applicationClassNode) {
        if(applicationClassNode == null) return


        enableAutowireByName(classNode)

        if (TestMixinTransformation.isSpockTest(classNode)) {
            // first add context configuration
            // Example: @ContextConfiguration(loader = GrailsApplicationContextLoader, classes = Application)
            def contextConfigAnn = new AnnotationNode(CONTEXT_CONFIG_ANNOTATION)
            contextConfigAnn.addMember("loader", new ClassExpression(GRAILS_APPLICATION_CONTEXT_LOADER))
            contextConfigAnn.addMember("classes", new ClassExpression(applicationClassNode))
            classNode.addAnnotation(contextConfigAnn)

            enhanceGebSpecWithPort(classNode)

        } else {
            // Must be a JUnit 4 test so add JUnit spring annotations
            // @RunWith(SpringJUnit4ClassRunner)
            def runWithAnnotation = new AnnotationNode(RUN_WITH_ANNOTATION_NODE)
            runWithAnnotation.addMember("value", new ClassExpression(SPRING_JUNIT4_CLASS_RUNNER))
            classNode.addAnnotation(runWithAnnotation)

            // @SpringApplicationConfiguration(classes = Application)
            def contextConfigAnn = new AnnotationNode(SPRING_APPLICATION_CONFIGURATION_CLASS_NODE)
            contextConfigAnn.addMember("classes", new ClassExpression(applicationClassNode))
            classNode.addAnnotation(contextConfigAnn)
        }

        // now add integration test annotations
        // @WebAppConfiguration
        // @IntegrationTest
        if (ClassUtils.isPresent("javax.servlet.ServletContext", Thread.currentThread().contextClassLoader)) {
            classNode.addAnnotation(new AnnotationNode(WEB_INTEGRATION_TEST_CLASS_NODE))
        } else {
            classNode.addAnnotation(new AnnotationNode(INTEGRATION_TEST_CLASS_NODE))
        }
    }

    protected void enableAutowireByName(ClassNode classNode) {
        classNode.addInterface(ClassHelper.make(ApplicationContextAware))

        def body = new BlockStatement()

        def ctxClass = ClassHelper.make(ApplicationContext)
        def p = new Parameter(ctxClass, "ctx")

        def getBeanFactoryMethodCall = new MethodCallExpression(new VariableExpression(p), "getAutowireCapableBeanFactory", GrailsASTUtils.ZERO_ARGUMENTS)

        def args = new ArgumentListExpression(new VariableExpression("this"), new ConstantExpression(AutowireCapableBeanFactory.AUTOWIRE_BY_NAME), new ConstantExpression(Boolean.FALSE))
        def autoMethodCall = new MethodCallExpression(getBeanFactoryMethodCall, "autowireBeanProperties", args)
        body.addStatement(new ExpressionStatement(autoMethodCall))
        classNode.addMethod("setApplicationContext", Modifier.PUBLIC, ClassHelper.VOID_TYPE, [p] as Parameter[], null, body)
    }

    protected void enhanceGebSpecWithPort(ClassNode classNode) {
        if (GrailsASTUtils.isSubclassOf(classNode, "geb.spock.GebSpec")) {
            def integerClassNode = ClassHelper.make(Integer)
            def param = new Parameter(integerClassNode, "port")
            def setterBody = new BlockStatement()
            def systemClassExpression = new ClassExpression(ClassHelper.make(System))
            def args = new ArgumentListExpression()
            args.addExpression(new ConstantExpression("geb.build.baseUrl"))
            args.addExpression(new GStringExpression('http://localhost:${port}', [new ConstantExpression("http://localhost:"), new ConstantExpression("")], [new VariableExpression(param)] as List<Expression>))
            setterBody.addStatement(new ExpressionStatement(new MethodCallExpression(systemClassExpression, "setProperty", args)))
            def method = new MethodNode("setPort", Modifier.PUBLIC, ClassHelper.VOID_TYPE, [param] as Parameter[], null, setterBody)
            def valueAnnotation = new AnnotationNode(ClassHelper.make(Value))
            valueAnnotation.setMember("value", new ConstantExpression('${local.server.port}'))
            method.addAnnotation(valueAnnotation)
            classNode.addMethod(method)
        }
    }

}
