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
package org.grails.plugins.web.rest.transform

import static java.lang.reflect.Modifier.*
import static org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils.*
import static org.codehaus.groovy.grails.web.mapping.ControllerActionConventions.*
import static org.springframework.http.HttpMethod.*
import grails.artefact.Artefact
import grails.rest.Resource
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.GrailsNameUtils
import grails.web.controllers.ControllerMethod
import groovy.transform.CompileStatic

import javax.annotation.PostConstruct

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.compiler.injection.ArtefactTypeAstTransformation
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareInjectionOperation
import org.codehaus.groovy.grails.compiler.web.ControllerActionTransformer
import org.codehaus.groovy.grails.core.io.DefaultResourceLocator
import org.codehaus.groovy.grails.core.io.ResourceLocator
import org.codehaus.groovy.grails.transaction.transform.TransactionalTransform
import org.codehaus.groovy.grails.web.mapping.UrlMappings
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus

/**
 * The Resource transform automatically exposes a domain class as a RESTful resource. In effect the transform adds a controller to a Grails application
 * that performs CRUD operations on the domain. See the {@link Resource} annotation for more details
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class ResourceTransform implements ASTTransformation{
    private static final ClassNode MY_TYPE = new ClassNode(Resource)
    public static final String ATTR_READY_ONLY = "readOnly"
    public static final String RESPOND_METHOD = "respond"
    public static final String ATTR_RESPONSE_FORMATS = "formats"
    public static final String ATTR_URI = "uri"
    public static final String PARAMS_VARIABLE = "params"
    public static final ConstantExpression CONSTANT_STATUS = new ConstantExpression(ARGUMENT_STATUS)
    public static final String RENDER_METHOD = "render"
    public static final String ARGUMENT_STATUS = "status"
    public static final String REDIRECT_METHOD = "redirect"
    public static final ClassNode AUTOWIRED_CLASS_NODE = new ClassNode(Autowired).getPlainNodeReference()

    private ResourceLocator resourceLocator

    ResourceLocator getResourceLocator() {
        if (resourceLocator == null) {
            resourceLocator = new DefaultResourceLocator()
            BuildSettings buildSettings = BuildSettingsHolder.getSettings()
            String basedir
            if (buildSettings != null) {
                basedir = buildSettings.getBaseDir().getAbsolutePath()
            } else {
                basedir = "."
            }

            resourceLocator.setSearchLocation(basedir)
        }
        return resourceLocator
    }

    @Override
    void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof ClassNode)) {
            throw new RuntimeException('Internal error: wrong types: $node.class / $parent.class')
        }

        ClassNode parent = (ClassNode) astNodes[1]
        AnnotationNode annotationNode = (AnnotationNode) astNodes[0]
        if (!MY_TYPE.equals(annotationNode.getClassNode())) {
            return
        }

        final resourceLocator = getResourceLocator()
        final className = "${parent.name}${ControllerArtefactHandler.TYPE}"
        final resource = resourceLocator.findResourceForClassName(className)

        LinkableTransform.addLinkingMethods(parent)

        if (resource == null) {
            final ast = source.getAST()
            final newControllerClassNode = new ClassNode(className, PUBLIC, GrailsASTUtils.OBJECT_CLASS_NODE)
            final transactionalAnn = new AnnotationNode(TransactionalTransform.MY_TYPE)
            transactionalAnn.addMember(ATTR_READY_ONLY,ConstantExpression.PRIM_TRUE)
            newControllerClassNode.addAnnotation(transactionalAnn)
            
            List<ClassInjector> injectors = ArtefactTypeAstTransformation.findInjectors(ControllerArtefactHandler.TYPE, GrailsAwareInjectionOperation.getClassInjectors());
                        
            ArtefactTypeAstTransformation.performInjection(source, newControllerClassNode, injectors.findAll { !(it instanceof ControllerActionTransformer) })
            
            final readOnlyAttr = annotationNode.getMember(ATTR_READY_ONLY)
            final responseFormatsAttr = annotationNode.getMember(ATTR_RESPONSE_FORMATS)
            final uriAttr = annotationNode.getMember(ATTR_URI)
            final domainPropertyName = GrailsNameUtils.getPropertyName(parent.getName())

            ListExpression responseFormatsExpression = new ListExpression()
            boolean hasHtml = false
            if (responseFormatsAttr != null) {
                if (responseFormatsAttr instanceof ConstantExpression) {
                    if (responseFormatsExpression.text.equalsIgnoreCase('html')) {
                        hasHtml = true
                    }
                    responseFormatsExpression.addExpression(responseFormatsAttr)
                }
                else if (responseFormatsAttr instanceof ListExpression) {
                    responseFormatsExpression = (ListExpression)responseFormatsAttr
                    for(Expression expr in responseFormatsExpression.expressions) {
                        if (expr.text.equalsIgnoreCase('html')) hasHtml = true; break
                    }
                }
            } else {
                responseFormatsExpression.addExpression(new ConstantExpression("xml"))
                responseFormatsExpression.addExpression(new ConstantExpression("json"))
            }

            if (uriAttr != null) {
                final uri = uriAttr.getText()
                if(uri) {
                    final urlMappingsClassNode = new ClassNode(UrlMappings).getPlainNodeReference()
                    final urlMappingsField = new FieldNode('$urlMappings', PRIVATE, urlMappingsClassNode,newControllerClassNode, null)
                    newControllerClassNode.addField(urlMappingsField)
                    final urlMappingsSetterParam = new Parameter(urlMappingsClassNode, "um")
                    final controllerMethodAnnotation = new AnnotationNode(new ClassNode(ControllerMethod).getPlainNodeReference())
                    MethodNode urlMappingsSetter = new MethodNode("setUrlMappings", PUBLIC, VOID_CLASS_NODE, [urlMappingsSetterParam] as Parameter[], null, new ExpressionStatement(new BinaryExpression(new VariableExpression(urlMappingsField.name),Token.newSymbol(Types.EQUAL, 0, 0), new VariableExpression(urlMappingsSetterParam))))
                    final autowiredAnnotation = new AnnotationNode(AUTOWIRED_CLASS_NODE)
                    autowiredAnnotation.addMember("required", ConstantExpression.FALSE)

                    final qualifierAnnotation = new AnnotationNode(new ClassNode(Qualifier).getPlainNodeReference())
                    qualifierAnnotation.addMember("value", new ConstantExpression("grailsUrlMappingsHolder"))
                    urlMappingsSetter.addAnnotation(autowiredAnnotation)
                    urlMappingsSetter.addAnnotation(qualifierAnnotation)
                    urlMappingsSetter.addAnnotation(controllerMethodAnnotation)
                    newControllerClassNode.addMethod(urlMappingsSetter)
                    processVariableScopes(source, newControllerClassNode, urlMappingsSetter)


                    final methodBody = new BlockStatement()

                    final urlMappingsVar = new VariableExpression(urlMappingsField.name)
                    final resourcesUrlMapping = new MethodCallExpression(buildThisExpression(), uri, new MapExpression([ new MapEntryExpression(new ConstantExpression("resources"), new ConstantExpression(domainPropertyName))]))
                    final urlMappingsClosure = new ClosureExpression(null, new ExpressionStatement(resourcesUrlMapping))

                    def addMappingsMethodCall = applyDefaultMethodTarget(new MethodCallExpression(urlMappingsVar, "addMappings", urlMappingsClosure), urlMappingsClassNode)
                    methodBody.addStatement(new IfStatement(new BooleanExpression(urlMappingsVar), new ExpressionStatement(addMappingsMethodCall),new EmptyStatement()))
                    
                    def initialiseUrlMappingsMethod = new MethodNode("initializeUrlMappings", PUBLIC, VOID_CLASS_NODE, ZERO_PARAMETERS, null, methodBody)
                    initialiseUrlMappingsMethod.addAnnotation(new AnnotationNode(new ClassNode(PostConstruct).getPlainNodeReference()))
                    initialiseUrlMappingsMethod.addAnnotation(controllerMethodAnnotation)
                    newControllerClassNode.addMethod(initialiseUrlMappingsMethod)
                    processVariableScopes(source, newControllerClassNode, initialiseUrlMappingsMethod)
                }
            }

            final publicStaticFinal = PUBLIC | STATIC | FINAL

            newControllerClassNode.addProperty("scope", publicStaticFinal, ClassHelper.STRING_TYPE, new ConstantExpression("singleton"), null, null)
            newControllerClassNode.addProperty("responseFormats", publicStaticFinal, new ClassNode(List).getPlainNodeReference(), responseFormatsExpression, null, null)

            boolean isReadOnly = readOnlyAttr != null && ((ConstantExpression)readOnlyAttr).trueExpression

            List<MethodNode> weavedMethods = []
            weaveReadActions(parent, domainPropertyName,newControllerClassNode, annotationNode.lineNumber, weavedMethods)
            if (!isReadOnly) {
                final mapExpression = new MapExpression()
                mapExpression.addMapEntryExpression(new ConstantExpression(ACTION_SAVE),new ConstantExpression(POST.toString()))
                mapExpression.addMapEntryExpression(new ConstantExpression(ACTION_UPDATE),new ConstantExpression(PUT.toString()))
                mapExpression.addMapEntryExpression(new ConstantExpression(ACTION_DELETE),new ConstantExpression(DELETE.toString()))
                newControllerClassNode.addField("allowedMethods", publicStaticFinal,new ClassNode(Map.class).getPlainNodeReference(), mapExpression)
                weaveWriteActions(parent,domainPropertyName, newControllerClassNode, hasHtml, annotationNode.lineNumber,weavedMethods)
            }

            ArtefactTypeAstTransformation.performInjection(source, newControllerClassNode, injectors.findAll { it instanceof ControllerActionTransformer })
            
            for(MethodNode mn in weavedMethods) {
                if(!mn.getAnnotations(ControllerActionTransformer.ACTION_ANNOTATION_NODE.classNode)) {
                    mn.addAnnotation(ControllerActionTransformer.ACTION_ANNOTATION_NODE)
                }
                processVariableScopes(source, newControllerClassNode, mn)
            }
            
            
            new TransactionalTransform().weaveTransactionalBehavior(source, newControllerClassNode, transactionalAnn)
            newControllerClassNode.setModule(ast)

            final artefactAnnotation = new AnnotationNode(new ClassNode(Artefact))
            artefactAnnotation.addMember("value", new ConstantExpression(ControllerArtefactHandler.TYPE))
            newControllerClassNode.addAnnotation(artefactAnnotation)

            ast.classes.add(newControllerClassNode)
        }
    }

    void weaveWriteActions(ClassNode domainClass, String domainPropertyName, ClassNode controllerClass, boolean hasHtml, int annotationLineNumber,List<MethodNode> weavedMethods) {
        if (hasHtml) {
            // the edit action
            createReadObjectAction(domainClass, domainPropertyName, controllerClass, ACTION_EDIT, annotationLineNumber, weavedMethods)
            // the create action
            weaveCreateAction(domainClass, controllerClass, weavedMethods)
        }

        weaveSaveAction(domainClass, domainPropertyName, controllerClass,hasHtml, annotationLineNumber, weavedMethods)
        weaveDeleteAction(domainClass, domainPropertyName, controllerClass,hasHtml, annotationLineNumber, weavedMethods)
        weaveUpdateAction(domainClass, domainPropertyName, controllerClass,hasHtml, annotationLineNumber, weavedMethods)
    }
    
    private ExpressionStatement createWithFormatStatement(Expression expression, Expression withFormatClosure, ClassNode controllerClass) {
        new ExpressionStatement(applyDefaultMethodTarget(new MethodCallExpression(expression, "withFormat", withFormatClosure), controllerClass))
    }

    void weaveUpdateAction(ClassNode domainClass, String domainPropertyName, ClassNode controllerClass, boolean hasHtml,int annotationLineNumber,List<MethodNode> weavedMethods) {
        def domainParameter = new Parameter(domainClass.getPlainNodeReference(),domainPropertyName)
        def domainVar = new VariableExpression(domainParameter)
        final args = new MapExpression()
        args.addMapEntryExpression(new ConstantExpression(ARGUMENT_STATUS), new ConstantExpression(HttpStatus.NOT_FOUND.value()))
        final ifBlock = new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), RENDER_METHOD, args), controllerClass, [Map] as Class[]))

        def elseBlock = new BlockStatement()
        def ifElseBlock = new BlockStatement()
        final respondArgs = new ArgumentListExpression()
        respondArgs.addExpression(buildGetPropertyExpression(domainVar, "errors", domainClass))
        final viewArgs = new MapExpression()
        viewArgs.addMapEntryExpression(new ConstantExpression("view"), new ConstantExpression("edit"))
        respondArgs.addExpression(viewArgs)

        final respondStatement = new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), RESPOND_METHOD, respondArgs), controllerClass, [Object, Map] as Class[]))
        ifElseBlock.addStatement(new IfStatement(new BooleanExpression(applyDefaultMethodTarget(new MethodCallExpression(domainVar, "hasErrors", ZERO_ARGUMENTS), domainClass)), respondStatement,elseBlock))

        def ifStatement = new IfStatement(new BooleanExpression(new NotExpression(domainVar)),ifBlock,ifElseBlock)

        final updateArgs = new ArgumentListExpression()
        final namedArgs = new MapExpression()
        namedArgs.addMapEntryExpression(new ConstantExpression("flush"), ConstantExpression.TRUE)
        updateArgs.addExpression(namedArgs)
        elseBlock.addStatement(new ExpressionStatement(applyMethodTarget(new MethodCallExpression(domainVar, ACTION_SAVE, updateArgs), domainClass, [Map] as Class[])))
        final withFormatBody = new BlockStatement()
        final withFormatClosure = new ClosureExpression(null, withFormatBody)
        elseBlock.addStatement(createWithFormatStatement(buildThisExpression(), withFormatClosure, controllerClass))

        if (hasHtml) {
            // add html specific method call
            final messageKey = "default.updated.message"
            final message = getFlashMessage(messageKey, domainPropertyName, domainClass, domainVar)
            final redirect = new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), REDIRECT_METHOD, domainVar), controllerClass, [Object] as Class[]))
            final closureBody = new BlockStatement()
            closureBody.addStatement(message)
            closureBody.addStatement(redirect)
            final htmlFormatClosure = new ClosureExpression(null, closureBody)
            withFormatBody.addStatement(new ExpressionStatement(new MethodCallExpression(buildThisExpression(), 'form', htmlFormatClosure)))

        }
        final renderArgs = new ArgumentListExpression()
        final renderNamedArgs = new MapExpression()
        renderNamedArgs.addMapEntryExpression(CONSTANT_STATUS, new ConstantExpression(HttpStatus.OK.value()))
        renderArgs.addExpression(domainVar)
        renderArgs.addExpression(renderNamedArgs)
        final allFormatsClosure = new ClosureExpression(null, new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), RESPOND_METHOD, renderArgs), controllerClass, [Object, Map] as Class[])))
        withFormatBody.addStatement(new ExpressionStatement(new MethodCallExpression(buildThisExpression(), '*', allFormatsClosure)))

        final updateMethod = new MethodNode(ACTION_UPDATE, PUBLIC, OBJECT_CLASS_NODE, [domainParameter] as Parameter[],null, ifStatement)
        updateMethod.addAnnotation(new AnnotationNode(TransactionalTransform.MY_TYPE))
        updateMethod.lineNumber = annotationLineNumber
        weavedMethods << updateMethod
        controllerClass.addMethod(updateMethod)
    }

    protected ExpressionStatement getFlashMessage(String messageKey, String domainPropertyName, ClassNode domainClass, VariableExpression domainVar) {
        final flashArgs = new MapExpression()
        flashArgs.addMapEntryExpression(new ConstantExpression("code"), new ConstantExpression(messageKey))
        final messageArgs = new MapExpression()
        messageArgs.addMapEntryExpression(new ConstantExpression("code"), new ConstantExpression("${domainPropertyName}.label".toString()))
        messageArgs.addMapEntryExpression(new ConstantExpression("default"), new ConstantExpression(domainClass.getNameWithoutPackage()))
        final defaultMessageList = new ListExpression()
        defaultMessageList.addExpression( new MethodCallExpression(buildThisExpression(), "message", messageArgs) )
        defaultMessageList.addExpression( new PropertyExpression(domainVar, "id") )
        flashArgs.addMapEntryExpression(new ConstantExpression("args"), defaultMessageList)
        new ExpressionStatement(
            new BinaryExpression(new PropertyExpression(new VariableExpression("flash"), "message"),
                Token.newSymbol(Types.EQUAL, 0, 0),
                new MethodCallExpression(buildThisExpression(), "message", flashArgs))
        )
    }

    void weaveDeleteAction(ClassNode domainClass, String domainPropertyName, ClassNode controllerClass, boolean hasHtml,int annotationLineNumber,List<MethodNode> weavedMethods) {
        def domainParameter = new Parameter(domainClass.getPlainNodeReference(),domainPropertyName)
        def domainVar = new VariableExpression(domainParameter)
        final args = new MapExpression()
        args.addMapEntryExpression(new ConstantExpression(ARGUMENT_STATUS), new ConstantExpression(HttpStatus.NOT_FOUND.value()))
        final ifBlock = new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), RENDER_METHOD, args), controllerClass, [Map] as Class[]))
        def elseBlock = new BlockStatement()

        def ifStatement = new IfStatement(new BooleanExpression(new NotExpression(domainVar)),ifBlock,elseBlock)

        final deleteArgs = new ArgumentListExpression()
        final namedArgs = new MapExpression()
        namedArgs.addMapEntryExpression(new ConstantExpression("flush"), ConstantExpression.TRUE)
        deleteArgs.addExpression(namedArgs)
        elseBlock.addStatement(new ExpressionStatement(applyDefaultMethodTarget(new MethodCallExpression(domainVar,ACTION_DELETE, deleteArgs), domainClass)))
        final withFormatBody = new BlockStatement()
        final withFormatClosure = new ClosureExpression(null, withFormatBody)
        elseBlock.addStatement(createWithFormatStatement(buildThisExpression(), withFormatClosure, controllerClass))

        if (hasHtml) {
            // add html specific method call
            final message = getFlashMessage('default.deleted.message', domainPropertyName, domainClass, domainVar)
            final redirectArgs = new MapExpression()
            redirectArgs.addMapEntryExpression(new ConstantExpression("action"), new ConstantExpression("index"))
            redirectArgs.addMapEntryExpression(new ConstantExpression("method"), new ConstantExpression(GET.toString()))
            final redirect = new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), REDIRECT_METHOD, redirectArgs), controllerClass, [Map] as Class[]))
            final closureBody = new BlockStatement()
            closureBody.addStatement(message)
            closureBody.addStatement(redirect)
            final htmlFormatClosure = new ClosureExpression(null, closureBody)
            withFormatBody.addStatement(new ExpressionStatement(new MethodCallExpression(buildThisExpression(), 'form', htmlFormatClosure)))

        }
        final renderArgs = new ArgumentListExpression()
        final renderNamedArgs = new MapExpression()
        renderNamedArgs.addMapEntryExpression(CONSTANT_STATUS, new ConstantExpression(HttpStatus.NO_CONTENT.value()))
        renderArgs.addExpression(renderNamedArgs)
        final allFormatsClosure = new ClosureExpression(null, new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), RENDER_METHOD, renderArgs), controllerClass, [Map] as Class[])))
        withFormatBody.addStatement(new ExpressionStatement(new MethodCallExpression(buildThisExpression(), '*', allFormatsClosure)))

        final deleteMethod = new MethodNode(ACTION_DELETE, PUBLIC, OBJECT_CLASS_NODE, [domainParameter] as Parameter[],null, ifStatement)
        deleteMethod.addAnnotation(new AnnotationNode(TransactionalTransform.MY_TYPE))
        deleteMethod.lineNumber = annotationLineNumber
        weavedMethods << deleteMethod
        controllerClass.addMethod(deleteMethod)
    }

    void weaveSaveAction(ClassNode domainClass, String domainPropertyName, ClassNode controllerClass, boolean hasHtml,int annotationLineNumber,List<MethodNode> weavedMethods) {
        def domainParameter = new Parameter(domainClass.getPlainNodeReference(),domainPropertyName)
        def domainVar = new VariableExpression(domainParameter)
        def respondArgs = new ArgumentListExpression()
        respondArgs.addExpression(buildGetPropertyExpression(domainVar, "errors", domainClass))
        final args = new MapExpression()
        args.addMapEntryExpression(new ConstantExpression("view"), new ConstantExpression(ACTION_CREATE))
        respondArgs.addExpression(args)

        final ifBlock = new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), RESPOND_METHOD, respondArgs), controllerClass, [Object, Map] as Class[]))
        def elseBlock = new BlockStatement()

        def ifStatement = new IfStatement(new BooleanExpression(applyDefaultMethodTarget(new MethodCallExpression(domainVar, "hasErrors", ZERO_ARGUMENTS), domainClass)),ifBlock,elseBlock)

        final saveArgs = new ArgumentListExpression()
        final namedArgs = new MapExpression()
        namedArgs.addMapEntryExpression(new ConstantExpression("flush"), ConstantExpression.TRUE)
        saveArgs.addExpression(namedArgs)
        elseBlock.addStatement(new ExpressionStatement(applyMethodTarget(new MethodCallExpression(domainVar,"save", saveArgs), domainClass, [Map] as Class[])))
        final withFormatBody = new BlockStatement()
        final withFormatClosure = new ClosureExpression(null, withFormatBody)
        elseBlock.addStatement(createWithFormatStatement(buildThisExpression(), withFormatClosure, controllerClass))


        if (hasHtml) {
            // add html specific method call
            final message = getFlashMessage('default.created.message', domainPropertyName, domainClass, domainVar)
            final redirect = new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), REDIRECT_METHOD, domainVar), controllerClass, [Object] as Class[]))
            final closureBody = new BlockStatement()
            closureBody.addStatement(message)
            closureBody.addStatement(redirect)
            final htmlFormatClosure = new ClosureExpression(null, closureBody)
            withFormatBody.addStatement(new ExpressionStatement(new MethodCallExpression(buildThisExpression(), 'form', htmlFormatClosure)))

        }
        final renderArgs = new ArgumentListExpression()
        final renderNamedArgs = new MapExpression()
        renderNamedArgs.addMapEntryExpression(CONSTANT_STATUS, new ConstantExpression(HttpStatus.CREATED.value()))
        renderArgs.addExpression(domainVar)
        renderArgs.addExpression(renderNamedArgs)
        final allFormatsClosure = new ClosureExpression(null, new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), RESPOND_METHOD, renderArgs), controllerClass, [Object, Map] as Class[])))
        withFormatBody.addStatement(new ExpressionStatement(new MethodCallExpression(buildThisExpression(), '*', allFormatsClosure)))

        final saveMethod = new MethodNode("save", PUBLIC, OBJECT_CLASS_NODE, [domainParameter] as Parameter[],null, ifStatement)
        saveMethod.addAnnotation(new AnnotationNode(TransactionalTransform.MY_TYPE))
        saveMethod.lineNumber = annotationLineNumber
        weavedMethods << saveMethod
        controllerClass.addMethod(saveMethod)
    }

    void weaveCreateAction(ClassNode domainClass, ClassNode controllerClass,List<MethodNode> weavedMethods) {
        BlockStatement methodBody = new BlockStatement()
        final args = new ArgumentListExpression()
        args.addExpression(new VariableExpression(PARAMS_VARIABLE))
        final constructorCall = new ConstructorCallExpression(domainClass.getPlainNodeReference(), args)
        methodBody.addStatement(new ReturnStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), RESPOND_METHOD, constructorCall), controllerClass, [Object, Map] as Class[])))
        final method = new MethodNode(ACTION_CREATE, PUBLIC, OBJECT_CLASS_NODE, ZERO_PARAMETERS, null, methodBody)
        weavedMethods << method
        controllerClass.addMethod(method)
    }

    void weaveReadActions(ClassNode domainClass, String domainPropertyName, ClassNode controllerClass, int annotationLineNumber,List<MethodNode> weavedMethods) {
        weaveIndexAction(domainClass,domainPropertyName,controllerClass, annotationLineNumber,weavedMethods)
        weaveShowAction(domainClass,domainPropertyName,controllerClass,annotationLineNumber,weavedMethods)
    }

    void weaveShowAction(ClassNode domainClass, String domainPropertyName,ClassNode controllerClass, int annotationLineNumber,List<MethodNode> weavedMethods) {
        createReadObjectAction(domainClass, domainPropertyName, controllerClass, ACTION_SHOW, annotationLineNumber,weavedMethods)
    }

    protected void createReadObjectAction(ClassNode domainClass, String domainPropertyName, ClassNode controllerClass, String actionName, int annotationLineNumber,List<MethodNode> weavedMethods) {
        Parameter[] params = [new Parameter(domainClass.getPlainNodeReference(), domainPropertyName)] as Parameter[]

        BlockStatement methodBody = new BlockStatement()
        methodBody.addStatement(new ExpressionStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), RESPOND_METHOD, new VariableExpression(domainPropertyName)), controllerClass, [Object, Map] as Class[])))
        final method = new MethodNode(actionName, PUBLIC, OBJECT_CLASS_NODE, params, null, methodBody)
        weavedMethods << method
        method.lineNumber = annotationLineNumber
        controllerClass.addMethod(method)
    }

    void weaveIndexAction(ClassNode domainClass, String domainPropertyName, ClassNode controllerClass, int annotationLineNumber,List<MethodNode> weavedMethods) {
        final maxParam = new Parameter(INTEGER_CLASS_NODE, "max")
        Parameter[] params = [maxParam] as Parameter[]
        BlockStatement  methodBody = new BlockStatement()

        final domainExpr = new ClassExpression(domainClass)
        final listArgs = new ArgumentListExpression()

        final paginationArgs = new ArgumentListExpression()
        paginationArgs.addExpression(new ElvisOperatorExpression(new VariableExpression(maxParam), new ConstantExpression(10)))
        paginationArgs.addExpression(new ConstantExpression(100))
        Expression getParamsExpression = buildGetPropertyExpression(buildThisExpression(), "params", controllerClass);
        methodBody.addStatement(
                new ExpressionStatement(
                buildPutMapExpression(getParamsExpression, "max",
                applyMethodTarget(new MethodCallExpression(new ClassExpression(new ClassNode(Math)), "max", paginationArgs), Math.class, int.class, int.class)))
                )

        listArgs.addExpression(new VariableExpression(PARAMS_VARIABLE))
        def listCall = applyDefaultMethodTarget(new MethodCallExpression(domainExpr, new ConstantExpression("list"), listArgs), domainClass)
        def countCall = applyDefaultMethodTarget(new MethodCallExpression(domainExpr, new ConstantExpression("count"), MethodCallExpression.NO_ARGUMENTS), domainClass)
        def respondArgs = new ArgumentListExpression()
        respondArgs.addExpression(listCall)
        final args = new MapExpression()
        final model = new MapExpression()
        model.addMapEntryExpression(new ConstantExpression("${domainPropertyName}Count".toString()), countCall)
        args.addMapEntryExpression(new ConstantExpression("model"), model)
        respondArgs.addExpression(args)

        methodBody.addStatement(new ReturnStatement(applyMethodTarget(new MethodCallExpression(buildThisExpression(), RESPOND_METHOD, respondArgs), controllerClass, [Object, Map] as Class[])))
        final method = new MethodNode(ACTION_INDEX, PUBLIC, OBJECT_CLASS_NODE, params, null, methodBody)
//        method.addAnnotation(ControllerActionTransformer.ACTION_ANNOTATION_NODE)
        weavedMethods << method
        method.lineNumber = annotationLineNumber
        controllerClass.addMethod(method)
    }
}
