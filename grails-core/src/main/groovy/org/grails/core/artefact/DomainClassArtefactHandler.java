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
package org.grails.core.artefact;

import grails.artefact.Artefact;
import grails.core.*;
import grails.core.support.GrailsApplicationAware;
import grails.persistence.Entity;
import grails.util.Environment;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.grails.compiler.injection.GrailsASTUtils;
import org.grails.core.DefaultGrailsDomainClass;
import org.grails.core.support.GrailsDomainConfigurationUtil;
import org.grails.io.support.GrailsResourceUtils;
import org.grails.io.support.Resource;
import org.grails.validation.ConstraintEvalUtils;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Map;
import java.util.TreeSet;

/**
 * Evaluates the conventions that define a domain class in Grails.
 *
 * @author Graeme Rocher
 * @author Marc Palmer (marc@anyware.co.uk)
 */
public class DomainClassArtefactHandler extends ArtefactHandlerAdapter implements GrailsApplicationAware, Ordered {

    public static final String TYPE = "Domain";
    public static final String PLUGIN_NAME = "domainClass";
    private  static final String ENTITY_ANN_NAME = "Entity";
    private static final String GRAILS_PACKAGE_PREFIX = "grails.";
    private static final String JAVAX_PERSISTENCE = "javax.persistence";

    private Map<String, Object> defaultConstraints;
    public DomainClassArtefactHandler() {
        super(TYPE, GrailsDomainClass.class, DefaultGrailsDomainClass.class, null, true);
    }
    private static boolean developmentMode = Environment.isDevelopmentMode();

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        if (grailsApplication != null) {
            defaultConstraints = ConstraintEvalUtils.getDefaultConstraints(grailsApplication.getConfig());
        }
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public GrailsClass newArtefactClass(Class artefactClass) {
        return new DefaultGrailsDomainClass(artefactClass, defaultConstraints);
    }

    @Override
    protected boolean isArtefactResource(Resource resource) throws IOException {
        return super.isArtefactResource(resource) && GrailsResourceUtils.isDomainClass(resource.getURL());
    }

    @Override
    protected boolean isValidArtefactClassNode(ClassNode classNode, int modifiers) {
        return !classNode.isEnum() && !(classNode instanceof InnerClassNode);
    }


    @Override
    public boolean isArtefact(ClassNode classNode) {
        if(classNode == null) return false;
        if(!isValidArtefactClassNode(classNode, classNode.getModifiers())) return false;

        URL url = GrailsASTUtils.getSourceUrl(classNode);
        if(url != null) {
            return GrailsResourceUtils.isDomainClass(url);
        }
        else {
            return super.isArtefact(classNode);
        }
    }

    /**
     * Sets up the relationships between the domain classes, this has to be done after
     * the intial creation to avoid looping
     */
    @Override
    public void initialize(ArtefactInfo artefacts) {
        GrailsDomainConfigurationUtil.configureDomainClassRelationships(
                artefacts.getGrailsClasses(),
                artefacts.getGrailsClassesByName());
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isArtefactClass(Class clazz) {
        return isDomainClass(clazz);
    }

    public static boolean isDomainClass(Class<?> clazz, boolean allowProxyClass) {
        boolean retval = isDomainClass(clazz);
        if(!retval && allowProxyClass && clazz != null && clazz.getSimpleName().contains("$")) {
            retval = isDomainClass(clazz.getSuperclass());
        }
        return retval;
    }
    
    public static boolean isDomainClass(Class<?> clazz) {
        return clazz != null && doIsDomainClassCheck(clazz);

    }

    private static boolean doIsDomainClassCheck(Class<?> clazz) {
        // it's not a closure
        if (Closure.class.isAssignableFrom(clazz)) {
            return false;
        }

        if (clazz.isEnum()) return false;
        Artefact artefactAnn = clazz.getAnnotation(Artefact.class);
        if( artefactAnn != null && artefactAnn.value().equals(DomainClassArtefactHandler.TYPE) ) {
            return true;
        }

        for (Annotation annotation : clazz.getAnnotations()) {
            Class<? extends Annotation> annType = annotation.annotationType();
            String annName = annType.getSimpleName();

            String pkgName = annType.getPackage().getName();
            if(ENTITY_ANN_NAME.equals(annName) && pkgName.startsWith(GRAILS_PACKAGE_PREFIX) || pkgName.startsWith(JAVAX_PERSISTENCE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
