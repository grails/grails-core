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
import static org.grails.compiler.injection.GrailsASTUtils.*
import grails.artefact.Artefact
import grails.compiler.ast.ClassInjector
import grails.rest.Resource
import grails.rest.RestfulController
import grails.util.BuildSettings
import grails.util.GrailsNameUtils
import grails.web.controllers.ControllerMethod
import grails.web.mapping.UrlMappings
import groovy.transform.CompilationUnitAware
import groovy.transform.CompileStatic

import java.lang.reflect.Modifier

import javax.annotation.PostConstruct

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.compiler.injection.ArtefactTypeAstTransformation
import org.grails.compiler.injection.GrailsAwareInjectionOperation
import org.grails.compiler.injection.TraitInjectionUtils
import org.grails.compiler.web.ControllerActionTransformer
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.core.io.DefaultResourceLocator
import org.grails.core.io.ResourceLocator
import org.grails.transaction.transform.TransactionalTransform
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

/**
 * The Resource transform automatically exposes a domain class as a RESTful resource. In effect the transform adds a controller to a Grails application
 * that performs CRUD operations on the domain. See the {@link Resource} annotation for more details
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class ResourceTransform implements ASTTransformation, CompilationUnitAware {
    private static final ClassNode MY_TYPE = new ClassNode(Resource)
    public static final String ATTR_READY_ONLY = "readOnly"
    public static final String ATTR_SUPER_CLASS = "superClass"
    public static final String RESPOND_METHOD = "respond"
    public static final String ATTR_RESPONSE_FORMATS = "formats"
    public static final String ATTR_URI = "uri"
    public static final String PARAMS_VARIABLE = "params"
    public static final ConstantExpression CONSTANT_STATUS = new ConstantExpression(ARGUMENT_STATUS)
    public static final String ATTR_NAMESPACE ="namespace"
    public static final String RENDER_METHOD = "render"
    public static final String ARGUMENT_STATUS = "status"
    public static final String REDIRECT_METHOD = "redirect"
    public static final ClassNode AUTOWIRED_CLASS_NODE = new ClassNode(Autowired).getPlainNodeReference()

    private ResourceLocator resourceLocator
    private CompilationUnit unit
    
    ResourceLocator getResourceLocator() {
        if (resourceLocator == null) {
            resourceLocator = new DefaultResourceLocator()
            String basedir = BuildSettings.BASE_DIR.absolutePath

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
            ClassNode<?> superClassNode
            Expression superClassAttribute = annotationNode.getMember(ATTR_SUPER_CLASS)
            if(superClassAttribute instanceof ClassExpression) {
                superClassNode = ((ClassExpression)superClassAttribute).getType()
            } else {
                superClassNode = ClassHelper.make(RestfulController)
            }
            
            final ast = source.getAST()
            final newControllerClassNode = new ClassNode(className, PUBLIC, nonGeneric(superClassNode, parent))
            
            final transactionalAnn = new AnnotationNode(TransactionalTransform.MY_TYPE)
            transactionalAnn.addMember(ATTR_READY_ONLY,ConstantExpression.PRIM_TRUE)
            newControllerClassNode.addAnnotation(transactionalAnn)
            
            final readOnlyAttr = annotationNode.getMember(ATTR_READY_ONLY)
            boolean isReadOnly = readOnlyAttr != null && ((ConstantExpression)readOnlyAttr).trueExpression
            addConstructor(newControllerClassNode, parent, isReadOnly)
            
            List<ClassInjector> injectors = ArtefactTypeAstTransformation.findInjectors(ControllerArtefactHandler.TYPE, GrailsAwareInjectionOperation.getClassInjectors());
                        
            ArtefactTypeAstTransformation.performInjection(source, newControllerClassNode, injectors.findAll { !(it instanceof ControllerActionTransformer) })
            
            if(unit) {
                TraitInjectionUtils.processTraitsForNode(source, newControllerClassNode, 'Controller', unit)
            }
            
            final responseFormatsAttr = annotationNode.getMember(ATTR_RESPONSE_FORMATS)
            final uriAttr = annotationNode.getMember(ATTR_URI)
            final namespaceAttr = annotationNode.getMember(ATTR_NAMESPACE)
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
                responseFormatsExpression.addExpression(new ConstantExpression("json"))
                responseFormatsExpression.addExpression(new ConstantExpression("xml"))
            }

            if (uriAttr != null || namespaceAttr != null) {

                String uri = uriAttr?.getText()
                final namespace=namespaceAttr?.getText()
                if(uri || namespace) {
                    final urlMappingsClassNode = new ClassNode(UrlMappings).getPlainNodeReference()

                    final lazyInitField = new FieldNode('lazyInit', PUBLIC | STATIC | FINAL, ClassHelper.Boolean_TYPE,newControllerClassNode, new ConstantExpression(Boolean.FALSE))
                    newControllerClassNode.addField(lazyInitField)

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

                    Expression map=new MapExpression()
                    if(uri){
                        map.addMapEntryExpression(new MapEntryExpression(new ConstantExpression("resources"), new ConstantExpression(domainPropertyName)))
                    }
                    if(namespace){
                        final namespaceField = new FieldNode('namespace', STATIC, ClassHelper.STRING_TYPE,newControllerClassNode, new ConstantExpression(namespace))
                        newControllerClassNode.addField(namespaceField)
                        if(map.getMapEntryExpressions().size()==0){
                            uri="/${namespace}/${domainPropertyName}"
                            map.addMapEntryExpression(new MapEntryExpression(new ConstantExpression("resources"), new ConstantExpression(domainPropertyName)))
                        }
                        map.addMapEntryExpression(new MapEntryExpression(new ConstantExpression("namespace"), new ConstantExpression(namespace)))
                    }

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

            ArtefactTypeAstTransformation.performInjection(source, newControllerClassNode, injectors.findAll { it instanceof ControllerActionTransformer })
            new TransactionalTransform().weaveTransactionalBehavior(source, newControllerClassNode, transactionalAnn)
            newControllerClassNode.setModule(ast)

            final artefactAnnotation = new AnnotationNode(new ClassNode(Artefact))
            artefactAnnotation.addMember("value", new ConstantExpression(ControllerArtefactHandler.TYPE))
            newControllerClassNode.addAnnotation(artefactAnnotation)

            ast.classes.add(newControllerClassNode)
        }
    }
    
    ConstructorNode addConstructor(ClassNode controllerClassNode, ClassNode domainClassNode, boolean readOnly) {
        BlockStatement constructorBody = new BlockStatement()
        constructorBody.addStatement(new ExpressionStatement(new ConstructorCallExpression(ClassNode.SUPER, new TupleExpression(new ClassExpression(domainClassNode),new ConstantExpression(readOnly, true)))))
        controllerClassNode.addConstructor(Modifier.PUBLIC, ZERO_PARAMETERS, ClassNode.EMPTY_ARRAY, constructorBody)
    }
    
    void setCompilationUnit(CompilationUnit unit) {
        this.unit = unit
    }
}
