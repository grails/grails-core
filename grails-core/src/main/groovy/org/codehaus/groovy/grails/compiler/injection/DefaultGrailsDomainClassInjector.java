/*
 * Copyright 2004-2005 the original author or authors.
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

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of domain class injector interface that adds the 'id'
 * and 'version' properties and other previously boilerplate code.
 *
 * @author Graeme Rocher
 * @since 0.2
 */
@AstTransformer
public class DefaultGrailsDomainClassInjector implements GrailsDomainClassInjector {

    private static final String DOMAIN_DIR = "domain";

    private static final String GRAILS_APP_DIR = "grails-app";

    private List<ClassNode> classesWithInjectedToString = new ArrayList<ClassNode>();

    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        if (isDomainClass(classNode, source) && shouldInjectClass(classNode)) {
            performInjectionOnAnnotatedEntity(classNode);
        }
    }

    public void performInjectionOnAnnotatedEntity(ClassNode classNode) {
        injectIdProperty(classNode);
        injectVersionProperty(classNode);
        injectToStringMethod(classNode);
        injectAssociations(classNode);
    }

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }

    protected boolean isDomainClass(@SuppressWarnings("unused") ClassNode classNode, SourceUnit sourceNode) {
        String sourcePath = sourceNode.getName();
        File sourceFile = new File(sourcePath);
        File parent = sourceFile.getParentFile();
        while (parent != null) {
            File parentParent = parent.getParentFile();
            if (parent.getName().equals(DOMAIN_DIR) && parentParent != null &&
                    parentParent.getName().equals(GRAILS_APP_DIR)) {
                return true;
            }
            parent = parentParent;
        }

        return false;
    }

    protected boolean shouldInjectClass(ClassNode classNode) {
        String fullName = GrailsASTUtils.getFullName(classNode);
        String mappingFile = getMappingFileName(fullName);

        if (getClass().getResource(mappingFile) != null) {
            return false;
        }

        return !isEnum(classNode);
    }

    /**
     * Returns the ORM framework's mapping file name for the specified class name.
     *
     * @param className The class name of the mapped file
     * @return The mapping file name
     */
    private String getMappingFileName(String className) {
        return className.replaceAll("\\.", "/") + ".hbm.xml";
    }

    private void injectAssociations(ClassNode classNode) {

        List<PropertyNode> propertiesToAdd = new ArrayList<PropertyNode>();
        for (PropertyNode pn : classNode.getProperties()) {
            final String name = pn.getName();
            final boolean isHasManyProperty = name.equals(GrailsDomainClassProperty.RELATES_TO_MANY) ||
                    name.equals(GrailsDomainClassProperty.HAS_MANY);
            if (isHasManyProperty) {
                Expression e = pn.getInitialExpression();
                propertiesToAdd.addAll(createPropertiesForHasManyExpression(e, classNode));
            }
            final boolean isBelongsTo = name.equals(GrailsDomainClassProperty.BELONGS_TO) || name.equals(GrailsDomainClassProperty.HAS_ONE);
            if (isBelongsTo) {
                Expression e = pn.getInitialExpression();
                propertiesToAdd.addAll(createPropertiesForBelongsToExpression(e, classNode));
            }
        }
        injectAssociationProperties(classNode, propertiesToAdd);
    }

    private Collection<PropertyNode> createPropertiesForBelongsToExpression(Expression e, ClassNode classNode) {
        List<PropertyNode> properties = new ArrayList<PropertyNode>();
        if (e instanceof MapExpression) {
            MapExpression me = (MapExpression) e;
            for (MapEntryExpression mme : me.getMapEntryExpressions()) {
                String key = mme.getKeyExpression().getText();
                final Expression expression = mme.getValueExpression();
                ClassNode type;
                if (expression instanceof ClassExpression) {
                    type = expression.getType();
                }
                else {
                    type = ClassHelper.make(expression.getText());
                }

                properties.add(new PropertyNode(key, Modifier.PUBLIC, type, classNode, null, null, null));
            }
        }

        return properties;
    }

    private void injectAssociationProperties(ClassNode classNode, List<PropertyNode> propertiesToAdd) {
        for (PropertyNode pn : propertiesToAdd) {
            if (!GrailsASTUtils.hasProperty(classNode, pn.getName())) {
                classNode.addProperty(pn);
            }
        }
    }

    private List<PropertyNode> createPropertiesForHasManyExpression(Expression e, ClassNode classNode) {
        List<PropertyNode> properties = new ArrayList<PropertyNode>();
        if (e instanceof MapExpression) {
            MapExpression me = (MapExpression) e;
            for (MapEntryExpression mee : me.getMapEntryExpressions()) {
                String key = mee.getKeyExpression().getText();
                addAssociationForKey(key, properties, classNode, findPropertyType(mee.getKeyExpression()));
            }
        }
        return properties;
    }

    /**
     * Finds the type of the generated property.  The type will be a {@link Set} that is parameterized
     * by the type of the expression passed in.
     * @param expression the expression used to parameterize the {@link Set}.  Only used if a {@link ClassExpression}.  Otherwise ignored.
     * @return A {@link ClassNode} of type {@link Set} that is possibly parameterized by the expression that is passed in.
     */
    private ClassNode findPropertyType(Expression expression) {
        ClassNode set = new ClassNode(Set.class);
        if (expression instanceof ClassExpression) {
            ClassNode newSet = ClassHelper.makeWithoutCaching(set.getName());
            newSet.setRedirect(set);
            GenericsType[] genericsTypes = new GenericsType[1];
            genericsTypes[0] = new GenericsType(expression.getType());
            newSet.setGenericsTypes(genericsTypes);
            set = newSet;
        }
        return set;
    }

    private void addAssociationForKey(String key, List<PropertyNode> properties, ClassNode declaringType, ClassNode propertyType) {
        properties.add(new PropertyNode(key, Modifier.PUBLIC, propertyType, declaringType, null, null, null));
    }

    private void injectToStringMethod(ClassNode classNode) {
        final boolean hasToString = GrailsASTUtils.implementsOrInheritsZeroArgMethod(
                classNode, "toString", classesWithInjectedToString);

        if (!hasToString && !isEnum(classNode)) {
            GStringExpression ge = new GStringExpression(classNode.getName() + " : ${id}");
            ge.addString(new ConstantExpression(classNode.getName() + " : "));
            ge.addValue(new VariableExpression("id"));
            Statement s = new ReturnStatement(ge);
            MethodNode mn = new MethodNode("toString", Modifier.PUBLIC, new ClassNode(String.class), new Parameter[0], new ClassNode[0], s);
            classNode.addMethod(mn);
            classesWithInjectedToString.add(classNode);
        }
    }

    private boolean isEnum(ClassNode classNode) {
        ClassNode parent = classNode.getSuperClass();
        while (parent != null) {
            if (parent.getName().equals("java.lang.Enum")) return true;
            parent = parent.getSuperClass();
        }
        return false;
    }

    private void injectVersionProperty(ClassNode classNode) {
        final boolean hasVersion = GrailsASTUtils.hasOrInheritsProperty(classNode, GrailsDomainClassProperty.VERSION);

        if (!hasVersion) {
            ClassNode parent = GrailsASTUtils.getFurthestUnresolvedParent(classNode);
            parent.addProperty(GrailsDomainClassProperty.VERSION, Modifier.PUBLIC, new ClassNode(Long.class), null, null, null);
        }
    }

    private void injectIdProperty(ClassNode classNode) {
        final boolean hasId = GrailsASTUtils.hasOrInheritsProperty(classNode, GrailsDomainClassProperty.IDENTITY);

        if (!hasId) {
            // inject into furthest relative
            ClassNode parent = GrailsASTUtils.getFurthestUnresolvedParent(classNode);

            parent.addProperty(GrailsDomainClassProperty.IDENTITY, Modifier.PUBLIC, new ClassNode(Long.class), null, null, null);
        }
    }

    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }
}
