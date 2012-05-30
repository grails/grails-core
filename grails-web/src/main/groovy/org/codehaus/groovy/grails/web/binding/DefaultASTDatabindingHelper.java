package org.codehaus.groovy.grails.web.binding;

import grails.util.GrailsNameUtils;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.ClassInjector;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;

public class DefaultASTDatabindingHelper implements ASTDatabindingHelper {
    public static final String CONSTRAINTS_FIELD_NAME = "constraints";
    public static final String BINDABLE_CONSTRAINT_NAME = "bindable";

    public static final String DEFAULT_DATABINDING_WHITELIST = "$defaultDatabindingWhiteList";

    private static Map<ClassNode, Set<String>> CLASS_NAME_TO_WHITE_LIST_PROPERTY_NAMES = new HashMap<ClassNode, Set<String>>();

    @SuppressWarnings("serial")
    private static final List<ClassNode> SIMPLE_TYPES = new ArrayList<ClassNode>() {{
       add(new ClassNode(java.lang.Boolean.class));
       add(new ClassNode(java.lang.Boolean.TYPE));
       add(new ClassNode(java.lang.Byte.class));
       add(new ClassNode(java.lang.Byte.TYPE));
       add(new ClassNode(java.lang.Character.class));
       add(new ClassNode(java.lang.Character.TYPE));
       add(new ClassNode(java.lang.Short.class));
       add(new ClassNode(java.lang.Short.TYPE));
       add(new ClassNode(java.lang.Integer.class));
       add(new ClassNode(java.lang.Integer.TYPE));
       add(new ClassNode(java.lang.Long.class));
       add(new ClassNode(java.lang.Long.TYPE));
       add(new ClassNode(java.lang.Float.class));
       add(new ClassNode(java.lang.Float.TYPE));
       add(new ClassNode(java.lang.Double.class));
       add(new ClassNode(java.lang.Double.TYPE));
       add(new ClassNode(java.math.BigInteger.class));
       add(new ClassNode(java.math.BigDecimal.class));
       add(new ClassNode(java.lang.String.class));
       add(new ClassNode(java.net.URL.class));
    }};

    public void injectDatabindingCode(final SourceUnit source, final GeneratorContext context, final ClassNode classNode) {
        addDefaultDatabindingWhitelistField(source, classNode);
        addDatabindingApi(source, context, classNode);
    }


    private void addDatabindingApi(final SourceUnit source,
            final GeneratorContext context, final ClassNode classNode) {
        final ClassInjector classInjector = new AbstractGrailsArtefactTransformer() {

            public boolean shouldInject(final URL url) {
                return true;
            }

            @Override
            public Class<?> getInstanceImplementation() {
                return DatabindingApi.class;
            }

            @Override
            public Class<?> getStaticImplementation() {
                return null;
            }
            
            @Override
            protected boolean requiresAutowiring() {
                return false;
            }
        };
        classInjector.performInjection(source, context, classNode);
    }

    private void addDefaultDatabindingWhitelistField(final SourceUnit sourceUnit, final ClassNode classNode) {
        final FieldNode defaultWhitelistField = classNode.getDeclaredField(DEFAULT_DATABINDING_WHITELIST);
        if(defaultWhitelistField == null) {
            final Set<String> propertyNamesToIncludeInWhiteList = getPropertyNamesToIncludeInWhiteList(sourceUnit, classNode);
                
            final ListExpression listExpression = new ListExpression();
            for(String propertyName : propertyNamesToIncludeInWhiteList) {
                listExpression.addExpression(new ConstantExpression(propertyName));

                final FieldNode declaredField = getDeclaredFieldInInheritanceHierarchy(classNode, propertyName);
                if(declaredField != null) {
                    final ClassNode type = declaredField.getType();
                    if(type != null && !SIMPLE_TYPES.contains(type)) {
                        listExpression.addExpression(new ConstantExpression(propertyName + "_*"));
                        listExpression.addExpression(new ConstantExpression(propertyName + ".*"));
                    }
                }
            }
            
            classNode.addField(DEFAULT_DATABINDING_WHITELIST,
                    Modifier.STATIC | Modifier.PUBLIC | Modifier.FINAL, new ClassNode(List.class),
                    listExpression);
         }
    }


    private FieldNode getDeclaredFieldInInheritanceHierarchy(
            final ClassNode classNode, String propertyName) {
        FieldNode fieldNode = null;
        fieldNode = classNode.getDeclaredField(propertyName);
        if(fieldNode == null) {
            if(!classNode.getSuperClass().equals(new ClassNode(Object.class))) {
                return getDeclaredFieldInInheritanceHierarchy(classNode.getSuperClass(), propertyName);
            }
        }
        return fieldNode;
    }

    private Set<String> getPropertyNamesToIncludeInWhiteList(final SourceUnit sourceUnit, final ClassNode classNode) {
        if(CLASS_NAME_TO_WHITE_LIST_PROPERTY_NAMES.containsKey(classNode)) {
            return CLASS_NAME_TO_WHITE_LIST_PROPERTY_NAMES.get(classNode);
        }
        final Set<String> propertyNamesToIncludeInWhiteList = new HashSet<String>();
        final Set<String> unbindablePropertyNames = new HashSet<String>();
        final Set<String> bindablePropertyNames = new HashSet<String>();
        if(!classNode.getSuperClass().equals(new ClassNode(Object.class))) {
            final Set<String> parentClassPropertyNames = getPropertyNamesToIncludeInWhiteList(sourceUnit, classNode.getSuperClass());
            bindablePropertyNames.addAll(parentClassPropertyNames);
        }
        
        final FieldNode constraintsFieldNode = classNode.getDeclaredField(CONSTRAINTS_FIELD_NAME);
        if(constraintsFieldNode != null && constraintsFieldNode.hasInitialExpression()) {
            final Expression constraintsInitialExpression = constraintsFieldNode.getInitialExpression();
            if(constraintsInitialExpression instanceof ClosureExpression) {
              
                final Map<String, Map<String, Expression>> constraintsInfo = GrailsASTUtils.getConstraintMetadata((ClosureExpression)constraintsInitialExpression);
                
                for (Entry<String, Map<String, Expression>> constraintConfig : constraintsInfo.entrySet()) {
                    final String propertyName = constraintConfig.getKey();
                    final Map<String, Expression> mapEntryExpressions = constraintConfig.getValue();
                    for (Entry<String, Expression> entry : mapEntryExpressions.entrySet()) {
                        final String constraintName = entry.getKey();
                        if(BINDABLE_CONSTRAINT_NAME.equals(constraintName)) {
                            final Expression valueExpression = entry.getValue();
                            Boolean bindableValue = null;
                            if(valueExpression instanceof ConstantExpression) {
                                final Object constantValue = ((ConstantExpression) valueExpression).getValue();
                                if(constantValue instanceof Boolean) {
                                    bindableValue = (Boolean) constantValue;
                                }
                            }
                            if(bindableValue != null) {
                                if(Boolean.TRUE.equals(bindableValue)) {
                                    unbindablePropertyNames.remove(propertyName);
                                    bindablePropertyNames.add(propertyName);
                                } else {
                                    bindablePropertyNames.remove(propertyName);
                                    unbindablePropertyNames.add(propertyName);
                                }
                            } else {
                                final String message = "The bindable constraint for property [" + propertyName + "] in class [" + classNode.getName() + "] has a value which is not a boolean literal and will be ignored.";
                                GrailsASTUtils.warning(sourceUnit, valueExpression, message);
                            }
                        }
                    }
                }
            }
        }
        
        final Set<String> fieldsInTransientsList = getPropertyNamesExpressedInTransientsList(classNode);

        propertyNamesToIncludeInWhiteList.addAll(bindablePropertyNames);
        final List<FieldNode> fields = classNode.getFields();
        for(FieldNode fieldNode : fields) {
            final String fieldName = fieldNode.getName();
            if((!unbindablePropertyNames.contains(fieldName)) && 
                    (bindablePropertyNames.contains(fieldName) || shouldFieldBeInWhiteList(fieldNode, fieldsInTransientsList))) {
                propertyNamesToIncludeInWhiteList.add(fieldName);
            }
        }
        
        final Map<String, MethodNode> declaredMethodsMap = classNode.getDeclaredMethodsMap();
        for (Entry<String, MethodNode> methodEntry : declaredMethodsMap.entrySet()) {
            final MethodNode value = methodEntry.getValue();
            if(value.getDeclaringClass() == classNode) {
                Parameter[] parameters = value.getParameters();
                if(parameters != null && parameters.length == 1) {
                    final String methodName = value.getName();
                    if(methodName.startsWith("set")) {
                        final Parameter parameter = parameters[0];
                        final ClassNode paramType = parameter.getType();
                        if(!paramType.equals(new ClassNode(Object.class))) {
                            final String restOfMethodName = methodName.substring(3);
                            final String propertyName = GrailsNameUtils.getPropertyName(restOfMethodName);
                            if(!unbindablePropertyNames.contains(propertyName)) {
                                propertyNamesToIncludeInWhiteList.add(propertyName);
                            }
                        }
                    }
                }
            }
        }
        CLASS_NAME_TO_WHITE_LIST_PROPERTY_NAMES.put(classNode, propertyNamesToIncludeInWhiteList);
        Map<String, ClassNode> allAssociationMap = GrailsASTUtils.getAllAssociationMap(classNode);
        for (String associationName : allAssociationMap.keySet()) {
            if(!propertyNamesToIncludeInWhiteList.contains(associationName)) {
                propertyNamesToIncludeInWhiteList.add(associationName);
            }
        }
        return propertyNamesToIncludeInWhiteList;
    }

    private boolean shouldFieldBeInWhiteList(final FieldNode fieldNode, final Set<String> fieldsInTransientsList) {
        boolean shouldInclude = true;
        final int modifiers = fieldNode.getModifiers();
        if((modifiers & Modifier.STATIC) != 0 ||
                (modifiers & Modifier.TRANSIENT) != 0 ||
                fieldsInTransientsList.contains(fieldNode.getName()) ||
                fieldNode.getType().equals(new ClassNode(Object.class))) {
            shouldInclude = false;
        }
        return shouldInclude;
    }
    
    private Set<String> getPropertyNamesExpressedInTransientsList(final ClassNode classNode) {
        final Set<String> transientFields = new HashSet<String>();
        final FieldNode transientsField = classNode.getField("transients");
        if(transientsField != null && transientsField.isStatic()) {
            final Expression initialValueExpression = transientsField.getInitialValueExpression();
            if(initialValueExpression instanceof ListExpression) {
                final ListExpression le = (ListExpression) initialValueExpression;
                final List<Expression> expressions = le.getExpressions();
                for(Expression expr : expressions) {
                    if(expr instanceof ConstantExpression) {
                        final ConstantExpression ce = (ConstantExpression) expr;
                        final Object contantValue = ce.getValue();
                        if(contantValue instanceof String) {
                            transientFields.add((String) contantValue);
                        }
                    }
                }
            }
        }
        return transientFields;
    }
}

