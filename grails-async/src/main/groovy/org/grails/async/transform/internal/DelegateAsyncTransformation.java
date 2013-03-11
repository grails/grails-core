/*
 * Copyright 2013 SpringSource
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
package org.grails.async.transform.internal;


import grails.async.Promise;
import grails.async.Promises;
import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of {@link grails.async.transform.DelegateAsync} transformation
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class DelegateAsyncTransformation implements ASTTransformation {
    private static final ArgumentListExpression NO_ARGS = new ArgumentListExpression();

    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (nodes.length != 2 || !(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new GroovyBugError("Internal error: expecting [AnnotationNode, AnnotatedNode] but got: " + Arrays.asList(nodes));
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode annotationNode = (AnnotationNode) nodes[0];

        if (parent instanceof ClassNode) {
            Expression value = annotationNode.getMember("value");
            if (value instanceof ClassExpression) {
                ClassNode targetApi = value.getType().getPlainNodeReference();
                ClassNode classNode = (ClassNode)parent;

                final String fieldName = '$' + Introspector.decapitalize(targetApi.getNameWithoutPackage());
                FieldNode fieldNode = classNode.getField(fieldName);
                if (fieldNode == null) {
                    fieldNode = new FieldNode(fieldName, Modifier.PRIVATE, targetApi, classNode, new ConstructorCallExpression(targetApi, NO_ARGS));
                    classNode.addField(fieldNode);
                }

                applyDelegateAsyncTransform(classNode, targetApi, fieldName);
            }

        }
        else if(parent instanceof FieldNode) {
            FieldNode fieldNode = (FieldNode)parent;
            ClassNode targetApi = fieldNode.getType().getPlainNodeReference();
            ClassNode classNode = fieldNode.getOwner();
            applyDelegateAsyncTransform(classNode, targetApi, fieldNode.getName());
        }
    }

    private void applyDelegateAsyncTransform(ClassNode classNode, ClassNode targetApi, String fieldName) {
        List<MethodNode> methods = targetApi.getAllDeclaredMethods();
        for(MethodNode m : methods) {
            if (isCandidateMethod(m)) {
                MethodNode existingMethod = classNode.getMethod(m.getName(), m.getParameters());
                if (existingMethod == null) {
                    ClassNode promiseNode = ClassHelper.make(Promise.class).getPlainNodeReference();
                    promiseNode.setGenericsTypes( new GenericsType[]{ new GenericsType(m.getReturnType().getPlainNodeReference()) });
                    final BlockStatement methodBody = new BlockStatement();
                    final BlockStatement promiseBody = new BlockStatement();


                    ClassNode promisesClass = ClassHelper.make(Promises.class).getPlainNodeReference();
                    final ClosureExpression closureExpression = new ClosureExpression(new Parameter[0], promiseBody);
                    VariableScope variableScope = new VariableScope();
                    closureExpression.setVariableScope(variableScope);
                    MethodCallExpression createPromise = new MethodCallExpression(new ClassExpression(promisesClass), "createPromise",new ArgumentListExpression( closureExpression ));
                    methodBody.addStatement(new ExpressionStatement(createPromise));
                    final ArgumentListExpression arguments = new ArgumentListExpression();
                    Parameter[] parameters = copyParameters(m.getParameters());
                    for(Parameter p : parameters) {
                        p.setClosureSharedVariable(true);
                        variableScope.putReferencedLocalVariable(p);
                        VariableExpression ve = new VariableExpression(p);
                        ve.setClosureSharedVariable(true);
                        arguments.addExpression(ve);
                    }
                    MethodCallExpression delegateMethodCall = new MethodCallExpression(new VariableExpression(fieldName), m.getName(), arguments);
                    promiseBody.addStatement(new ExpressionStatement(delegateMethodCall));
                    MethodNode newMethodNode = new MethodNode(m.getName(), Modifier.PUBLIC,promiseNode, parameters,null, methodBody);
                    classNode.addMethod(newMethodNode);
                }
            }
        }
    }

    private static boolean isCandidateMethod(MethodNode declaredMethod) {
        return !declaredMethod.isSynthetic() &&
                !declaredMethod.getName().contains("$")&&
                Modifier.isPublic(declaredMethod.getModifiers()) &&
                !Modifier.isAbstract(declaredMethod.getModifiers());
    }

    private static Parameter[] copyParameters(Parameter[] parameterTypes) {
        Parameter[] newParameterTypes = new Parameter[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Parameter parameterType = parameterTypes[i];
            Parameter newParameter = new Parameter(parameterType.getType().getPlainNodeReference(), parameterType.getName(), parameterType.getInitialExpression());
            newParameter.addAnnotations(parameterType.getAnnotations());
            newParameterTypes[i] = newParameter;
        }
        return newParameterTypes;
    }


}
