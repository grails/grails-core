/*
 * Copyright 2014 the original author or authors.
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
package grails.compiler

import static org.codehaus.groovy.ast.ClassHelper.CLASS_Type
import static org.codehaus.groovy.ast.ClassHelper.Integer_TYPE
import static org.codehaus.groovy.ast.ClassHelper.LIST_TYPE

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.MethodCall
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils
import org.codehaus.groovy.transform.stc.GroovyTypeCheckingExtensionSupport.TypeCheckingDSL


/**
 *
 * @since 2.4
 */
class TypeCheckedExtensions extends TypeCheckingDSL {

    @Override
    public Object run() {
        setup { newScope() }

        finish { scopeExit() }

        handleValidateableConstraints()
        
        handleCriteriaQueries()
        
        handleDynamicFinders()
        
        null
    }

    protected handleDynamicFinders() {
        methodNotFound { ClassNode receiver, String name, ArgumentListExpression argList, ClassNode[] argTypes, MethodCall call ->
            def dynamicCall
            if(receiver == CLASS_Type) {
                def genericsTypes = receiver.genericsTypes
                if(genericsTypes) {
                    def staticMethodCallTargetType = genericsTypes[0].type
                    if(staticMethodCallTargetType) {
                        def sourceUnit = staticMethodCallTargetType?.module?.context
                        if(GrailsASTUtils.isDomainClass(staticMethodCallTargetType, sourceUnit)) {
                            switch(name) {
                                case ~/countBy[A-Z].*/:
                                    dynamicCall = makeDynamicGormCall(call, Integer_TYPE, staticMethodCallTargetType)
                                    break
                                case ~/findAllBy[A-Z].*/:
                                case ~/listOrderBy[A-Z].*/:
                                    def returnType = parameterizedType(LIST_TYPE, staticMethodCallTargetType)
                                    dynamicCall = makeDynamicGormCall(call, returnType, staticMethodCallTargetType)
                                    break
                                case ~/findBy[A-Z].*/:
                                case ~/findOrCreateBy[A-Z].*/:
                                case ~/findOrSaveBy[A-Z].*/:
                                    dynamicCall = makeDynamicGormCall(call, staticMethodCallTargetType, staticMethodCallTargetType)
                                    break
                            }
                        }
                    }
                }
            }
            return dynamicCall
        }
    }

    protected makeDynamicGormCall(MethodCall call, ClassNode returnTypeNode, ClassNode domainClassTypeNode) {
        def dynamicCall = makeDynamic(call, returnTypeNode)
        dynamicCall.declaringClass = domainClassTypeNode
        dynamicCall
    }

    protected handleValidateableConstraints() {
        beforeVisitClass { ClassNode classNode ->
            def constraintsProperty = classNode.getField('constraints')
            if(constraintsProperty && constraintsProperty.isStatic() && constraintsProperty.initialExpression instanceof ClosureExpression) {
                newScope {
                    constraintsClosureCode = constraintsProperty.initialExpression.code
                }
                constraintsProperty.initialExpression.code = new EmptyStatement()
            }
        }

        afterVisitClass { ClassNode classNode ->
            if(currentScope.constraintsClosureCode) {
                def constraintsProperty = classNode.getField('constraints')
                constraintsProperty.initialExpression.code = currentScope.constraintsClosureCode
                currentScope.checkingConstraintsClosure = true
                withTypeChecker { visitClosureExpression constraintsProperty.initialExpression }
                scopeExit()
            }
        }

        methodNotFound { ClassNode receiver, String name, ArgumentListExpression argList, ClassNode[] argTypes, MethodCall call ->
            def dynamicCall
            if(currentScope.constraintsClosureCode && currentScope.checkingConstraintsClosure) {
                dynamicCall = makeDynamic (call)
            }
            dynamicCall
        }
    }

    protected handleCriteriaQueries() {
        methodNotFound { ClassNode receiver, String name, ArgumentListExpression argList, ClassNode[] argTypes, MethodCall call ->
            def dynamicCall
            if(currentScope.processingCriteriaClosure) {
                dynamicCall = makeDynamic (call)
            }
            dynamicCall
        }
        
        afterMethodCall { MethodCall call ->
            if(isCriteriaCall(call)) {
                scopeExit()
            }
        }
        
        beforeMethodCall { MethodCall call ->
            if(isCriteriaCall(call)) {
                newScope {
                    processingCriteriaClosure = true
                }
            }
        }
    }
    
    protected boolean isCriteriaCall(MethodCall call) {
        call instanceof MethodCallExpression && 
            call.objectExpression instanceof ClassExpression && 
            GrailsASTUtils.isDomainClass(call.objectExpression.type, null) && 
            (call.method.value == 'withCriteria' || call.method.value == 'createCriteria')
    }
}
