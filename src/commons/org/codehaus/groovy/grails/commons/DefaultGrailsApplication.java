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
package org.codehaus.groovy.grails.commons;

import grails.util.GrailsUtil;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObjectSupport;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.injection.GrailsInjectionOperation;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.security.CodeSource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of the GrailsApplication interface that manages application loading,
 * state, and artifact instances.
 *
 * @todo Review all synching on new artefact cache stuff, what is sync policy for grailsapplication?
 * 
 * @author Marc Palmer
 * @author Steven Devijver
 * @author Graeme Rocher
 * @since 0.1
 *        <p/>
 *        Created: Jul 2, 2005
 */
public class DefaultGrailsApplication extends GroovyObjectSupport implements GrailsApplication {

    private static final Pattern GETCLASSESPROP_PATTERN = Pattern.compile("(\\w+)(Classes)");
    private static final Pattern GETCLASSESMETH_PATTERN = Pattern.compile("(get)(\\w+)(Classes)");
    private static final Pattern ISCLASS_PATTERN = Pattern.compile("(is)(\\w+)(Class)");
    private static final Pattern GETCLASS_PATTERN = Pattern.compile("(get)(\\w+)Class");

    private GroovyClassLoader cl = null;

    private Class[] allClasses = null;

    private static Log log = LogFactory.getLog(DefaultGrailsApplication.class);
    private ApplicationContext parentContext;
    private Set loadedClasses = new HashSet();
    private GrailsResourceLoader resourceLoader;
    private ArtefactHandler[] artefactHandlers;
    private Map artefactHandlersByName = new HashMap();
    private Set allArtefactClasses = new HashSet();
    private Map artefactInfo = new HashMap();
    private boolean suspectArtefactInit;
    private Class[] allArtefactClassesArray;


    public DefaultGrailsApplication(final Class[] classes, GroovyClassLoader classLoader) {
        if (classes == null) {
            throw new IllegalArgumentException("Constructor argument 'classes' cannot be null");
        }

        configureLoadedClasses(classes);
        this.cl = classLoader;
    }

    private void initArtefactHandlers() {
        registerArtefactHandler( new DomainClassArtefactHandler());
        registerArtefactHandler( new ControllerArtefactHandler());
        registerArtefactHandler( new ServiceArtefactHandler());
        registerArtefactHandler( new TagLibArtefactHandler());
        registerArtefactHandler( new TaskArtefactHandler());
        registerArtefactHandler( new BootstrapArtefactHandler());
        registerArtefactHandler( new CodecArtefactHandler());
        registerArtefactHandler( new DataSourceArtefactHandler());

        // Cache the list as an array
        this.artefactHandlers = ((ArtefactHandler[]) this.artefactHandlersByName.values().toArray(
            new ArtefactHandler[artefactHandlersByName.size()]));
    }

    public DefaultGrailsApplication(final Resource[] resources) throws IOException {
        this(resources, null);
    }

    public DefaultGrailsApplication(final Resource[] resources,
        final GrailsInjectionOperation injectionOperation) throws IOException {
        super();

        log.debug("Loading Grails application.");

        this.resourceLoader = new GrailsResourceLoader(resources);
        GrailsResourceHolder resourceHolder = new GrailsResourceHolder();

        this.cl = configureClassLoader(injectionOperation, resourceLoader);

        Collection loadedResources = new ArrayList();
        this.loadedClasses = new HashSet();

        try {
            for (int i = 0; resources != null && i < resources.length; i++) {
                log.debug("Loading groovy file :[" + resources[i].getFile().getAbsolutePath() + "]");
                if (!loadedResources.contains(resources[i])) {
                    try {
                        String className = resourceHolder.getClassName(resources[i]);
                        if (!StringUtils.isBlank(className)) {
                            Class c = cl.loadClass(className, true, false);
                            Assert.notNull(c, "Groovy Bug! GCL loadClass method returned a null class!");

                            loadedClasses.add(c);
                            loadedResources = resourceLoader.getLoadedResources();
                        }
                    }
                    catch (ClassNotFoundException e) {
                        throw new org.codehaus.groovy.grails.exceptions.CompilationFailedException("Compilation error parsing file [" + resources[i].getFilename() + "]: " + e.getMessage(), e);
                    }
                }
                else {
                    Class c;
                    try {
                        c = cl.loadClass(resourceHolder.getClassName(resources[i]));
                    }
                    catch (ClassNotFoundException e) {
                        throw new GrailsConfigurationException("Groovy Bug! Resource [" + resources[i] + "] loaded, but not returned by GCL.");
                    }

                    loadedClasses.add(c);
                }
            }
        }
        catch (CompilationFailedException e) {
            if (GrailsUtil.isDevelopmentEnv()) {
                // if we're in the development environement then there is no point in this exception propagating up the stack as it
                // just clouds the actual error so log it as fatal and kill the server
                log.fatal("Compilation error loading Grails application: " + e.getMessage(), e);
                System.exit(1);
            }
            else {
                throw e;
            }
        }

        // get all the classes that were loaded
        if (log.isDebugEnabled()) {
            log.debug("loaded classes: [" + loadedClasses + "]");
        }

        Class[] classes = populateAllClasses();

        configureLoadedClasses(classes);
    }

    private GroovyClassLoader configureClassLoader(final GrailsInjectionOperation injectionOperation,
        final GrailsResourceLoader resourceLoader) throws IOException {
        GroovyClassLoader cl;


        if (injectionOperation == null) {
            cl = new GroovyClassLoader();
        }
        else {
            if (log.isDebugEnabled()) {
                log.debug("Creating new Groovy class loader without compiler config");
            }

            cl = new GroovyClassLoader() {

                /* (non-Javadoc)
                * @see groovy.lang.GroovyClassLoader#createCompilationUnit(org.codehaus.groovy.control.CompilerConfiguration, java.security.CodeSource)
                */
                protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
                    CompilationUnit cu = super.createCompilationUnit(config, source);
                    injectionOperation.setResourceLoader(resourceLoader);
                    cu.addPhaseOperation(injectionOperation, Phases.CONVERSION);
                    return cu;
                }

            };
        }

        cl.setShouldRecompile(Boolean.TRUE);
        cl.setResourceLoader(resourceLoader);
        Thread.currentThread().setContextClassLoader(cl);

        return cl;

    }


    public Class[] getAllArtefacts() {
        return allArtefactClassesArray;
    }

    private Class[] populateAllClasses() {
        this.allClasses = (Class[]) loadedClasses.toArray(new Class[loadedClasses.size()]);
        return allClasses;
    }

    private void configureLoadedClasses(Class[] classes) {
        initArtefactHandlers();

        artefactInfo.clear();
        allArtefactClasses.clear();
        allArtefactClassesArray = null;
        this.allClasses = classes;

        suspectArtefactInit = true;

        try {

            // first load the domain classes
            log.debug("Going to inspect artefact classes.");
            for (int i = 0; i < classes.length; i++) {
                log.debug("Inspecting [" + classes[i].getName() + "]");
                if (Modifier.isAbstract(classes[i].getModifiers())) {
                    log.debug("[" + classes[i].getName() + "] is abstract.");
                    continue;
                }

                // check what kind of artefact it is and add to corrent data structure
                for (int j = 0; j < artefactHandlers.length; j++) {
                    if (artefactHandlers[j].isArtefact(classes[i])) {
                        GrailsClass gclass = addArtefact(artefactHandlers[j].getType(), classes[i]);
                        // Also maintain set of all artefacts (!= all classes loaded)
                        allArtefactClasses.add(classes[i]);

                        // Update per-artefact cache
                        DefaultArtefactInfo info = getArtefactInfo(artefactHandlers[j].getType(), true);
                        info.addGrailsClass(gclass);
                    }
                }
            }

            refreshArtefactGrailsClassCaches();

        } finally {
            suspectArtefactInit = false;
        }

        allArtefactClassesArray = (Class[]) allArtefactClasses.toArray(new Class[allArtefactClasses.size()]);

        // Tell all artefact handlers to init now we've worked out which classes are which artefacts
        for (int j = 0; j < artefactHandlers.length; j++) {
            initializeArtefacts(artefactHandlers[j]);
        }
    }

    /**
     * <p>Tell all our artefact info objects to update their internal state after we've added a bunch of classes</p>
     */
    private void refreshArtefactGrailsClassCaches() {
        for (Iterator it = artefactInfo.values().iterator(); it.hasNext(); ) {
            DefaultArtefactInfo info = (DefaultArtefactInfo) it.next();
            info.updateComplete();
        }
    }

    private void addToLoaded(Class clazz) {
        this.loadedClasses.add(clazz);
        populateAllClasses();
    }

    public GrailsControllerClass getScaffoldingController(GrailsDomainClass domainClass) {
        if (domainClass == null) {
            return null;
        }

        ArtefactInfo info = getArtefactInfo(ControllerArtefactHandler.TYPE, true);
        GrailsControllerClass[] controllerClasses = (GrailsControllerClass[]) info.getGrailsClasses();

        for (int i = 0; i < controllerClasses.length; i++) {
            GrailsControllerClass controllerClass = controllerClasses[i];
            if (controllerClass.isScaffolding()) {
                Class scaffoldedClass = controllerClass.getScaffoldedClass();
                if (scaffoldedClass != null) {
                    if (domainClass.getClazz().getName().equals(scaffoldedClass.getName())) {
                        return controllerClass;
                    }
                }
                else if (domainClass.getName().equals(controllerClass.getName())) {
                    return controllerClass;
                }
            }
        }
        return null;
    }

    public GroovyClassLoader getClassLoader() {
        return this.cl;
    }

    public GrailsDataSource getGrailsDataSource() {
        String environment = System.getProperty(GrailsApplication.ENVIRONMENT);
        if (log.isDebugEnabled()) {
            log.debug("[GrailsApplication] Retrieving data source for environment: " + environment);
        }
        if (StringUtils.isBlank(environment)) {
            environment = GrailsApplication.ENV_PRODUCTION;
            String envPropName = GrailsClassUtils.getClassNameRepresentation(environment) + DataSourceArtefactHandler.TYPE;
            System.out.println("envPropName = " + envPropName);
            GrailsDataSource devDataSource = (GrailsDataSource) getArtefact(DataSourceArtefactHandler.TYPE, envPropName);
            if (devDataSource == null) {
                devDataSource = (GrailsDataSource) getArtefact(DataSourceArtefactHandler.TYPE,
                    GrailsClassUtils.getClassNameRepresentation(GrailsApplication.ENV_APPLICATION));

            }
            if (getArtefactCount(DataSourceArtefactHandler.TYPE) == 1 && devDataSource == null) {
                devDataSource = (GrailsDataSource) getFirstArtefact(DataSourceArtefactHandler.TYPE);
            }
            return devDataSource;
        }
        else {
            String envPropName = GrailsClassUtils.getClassNameRepresentation(environment) + DataSourceArtefactHandler.TYPE;
            GrailsDataSource dataSource = (GrailsDataSource) getArtefact(DataSourceArtefactHandler.TYPE, envPropName);
            if (dataSource == null && GrailsApplication.ENV_DEVELOPMENT.equalsIgnoreCase(environment)) {
                dataSource = (GrailsDataSource) getArtefact(DataSourceArtefactHandler.TYPE,
                    GrailsClassUtils.getClassNameRepresentation(GrailsApplication.ENV_APPLICATION));
            }
            if (dataSource == null) {
                log.warn("No data source found for environment [" + environment + "]. Please specify alternative via -Dgrails.env=myenvironment");
            }

            return dataSource;

        }
    }

    private int getArtefactCount(String type) {
        ArtefactInfo info = getArtefactInfo(type);
        return info == null ? 0 : info.getClasses().length;
    }
    public Class[] getAllClasses() {
        return this.allClasses;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.parentContext = applicationContext;
    }

    public ApplicationContext getParentContext() {
        return this.parentContext;
    }

    public Class getClassForName(String className) {
        if (StringUtils.isBlank(className)) {
            return null;
        }
        for (int i = 0; i < allClasses.length; i++) {
            Class c = allClasses[i];
            if (c.getName().equals(className)) {
                return c;
            }
        }
        return null;
    }

    public void refreshConstraints() {
        ArtefactInfo info = getArtefactInfo(DomainClassArtefactHandler.TYPE, true);
        GrailsClass[] domainClasses = info.getGrailsClasses();
        for (int i = 0; i < domainClasses.length; i++) {
            ((GrailsDomainClass)domainClasses[i]).refreshConstraints();
        }
    }

    public void refresh() {
        configureLoadedClasses(this.cl.getLoadedClasses());
    }

    public Resource getResourceForClass(Class theClazz) {
        if (this.resourceLoader == null) {
            return null;
        }
        return this.resourceLoader.getResourceForClass(theClazz);
    }

    public boolean isArtefact(Class theClazz) {
        return this.allArtefactClasses.contains(theClazz);
    }

    public boolean isArtefactOfType(String artefactType, Class theClazz) {
        return isArtefactOfType(artefactType, theClazz.getName());
    }

    public boolean isArtefactOfType(String artefactType, String className) {
        ArtefactHandler handler = (ArtefactHandler) artefactHandlersByName.get(artefactType);
        return getArtefact(artefactType, className) != null;
    }

    public GrailsClass getArtefact(String artefactType, String name) {
        ArtefactInfo info = getArtefactInfo(artefactType);
        return info == null ? null : info.getGrailsClass(name);
    }

    private GrailsClass getFirstArtefact(String artefactType) {
        ArtefactInfo info = getArtefactInfo(artefactType);
        // This will throw AIOB if we have none
        return info == null ? null : info.getGrailsClasses()[0];
    }

    public GrailsClass[] getArtefacts(String artefactType) {
        ArtefactInfo info = getArtefactInfo(artefactType, true);
        return info.getGrailsClasses();
    }

    // This is next call is equiv to getControllerByURI / getTagLibForTagName
    public GrailsClass getArtefactForFeature(String artefactType, Object featureID) {
        ArtefactHandler handler = (ArtefactHandler) artefactHandlersByName.get(artefactType);
        return handler.getArtefactForFeature(featureID);
    }

    public GrailsClass addArtefact(String artefactType, Class artefactClass) {
        System.out.println("addArtefact: "+artefactType+" - "+artefactClass);
        
        // @todo should we filter abstracts here?
        if (Modifier.isAbstract(artefactClass.getModifiers())) {
            return null;
        }

        ArtefactHandler handler = (ArtefactHandler) artefactHandlersByName.get(artefactType);
        if (handler.isArtefact(artefactClass)) {
            GrailsClass artefactGrailsClass = handler.newArtefactClass(artefactClass);

            // Store the GrailsClass in cache
            DefaultArtefactInfo info = getArtefactInfo(artefactType, true);
            info.addGrailsClass( artefactGrailsClass);
            info.updateComplete();

            addToLoaded(artefactClass);

            if (!suspectArtefactInit) {
                initializeArtefacts(artefactType);
            }

            return artefactGrailsClass;
        }
        else {
            throw new GrailsConfigurationException("Cannot add "+artefactType+" class ["
                + artefactClass + "]. It is not a "+ artefactType+"!");
        }
    }

    public GrailsClass addArtefact(String artefactType, GrailsClass artefactGrailsClass) {
        // @todo should we filter abstracts here?
        if (Modifier.isAbstract(artefactGrailsClass.getClazz().getModifiers())) {
            return null;
        }

        ArtefactHandler handler = (ArtefactHandler) artefactHandlersByName.get(artefactType);
        if (handler.isArtefactGrailsClass(artefactGrailsClass)) {
            // Store the GrailsClass in cache
            DefaultArtefactInfo info = getArtefactInfo(artefactType, true);
            info.addGrailsClass( artefactGrailsClass);
            info.updateComplete();

            return artefactGrailsClass;
        }
        else {
            throw new GrailsConfigurationException("Cannot add "+artefactType+" class ["
                + artefactGrailsClass + "]. It is not a "+ artefactType+"!");
        }
    }

    public void registerArtefactHandler(ArtefactHandler handler) {
        artefactHandlersByName.put(handler.getType(), handler);
    }

    /**
     * <p>Re-initialize the artefacts of the specified type. This gives handlers a chance to update caches etc</p>
     * @param artefactType The type of artefact to init
     */
    private void initializeArtefacts(String artefactType) {
        ArtefactHandler handler = (ArtefactHandler) artefactHandlersByName.get(artefactType);
        initializeArtefacts(handler);
    }

    /**
     * <p>Re-initialize the artefacts of the specified type. This gives handlers a chance to update caches etc</p>
     * @param handler The handler to register
     */
    private void initializeArtefacts(ArtefactHandler handler) {
        if (handler != null) {
            ArtefactInfo info = getArtefactInfo(handler.getType());
            // Only init those that have data
            if (info != null) {
                handler.initialize(info);
            }
        }
    }

    /**
     * <p>Get or create the cache of classes for the specified artefact type</p>
     * @param artefactType The name of an artefact type
     * @param create Set to true if you want non-existent caches to be created
     * @return The cache of classes for the type, or null if no cache exists and create is false
     */
    private DefaultArtefactInfo getArtefactInfo(String artefactType, boolean create) {
        DefaultArtefactInfo cache = (DefaultArtefactInfo) artefactInfo.get(artefactType);
        if ((cache == null) && create) {
            cache = new DefaultArtefactInfo();
            artefactInfo.put( artefactType, cache);
            cache.updateComplete();
        }
        return cache;
    }

    /**
     * <p>Get the cache of classes for the specified artefact type</p>
     * @param artefactType The name of an artefact type
     * @return The cache of classes for the type, or null if no cache exists
     */
    public ArtefactInfo getArtefactInfo(String artefactType) {
        return getArtefactInfo(artefactType, false);
    }


    /**
     * <p>Overrides method invocation to return dynamic artefact methods</p>
     * <p>We will support getXXXXClasses() and isXXXXClass(class)</p>
     * @param methodName
     * @param args
     * @return
     * @todo this is REALLY ugly
     */
    public Object invokeMethod(String methodName, Object args) {

        System.out.println("methodName = " + methodName);
        System.out.println("args = " + args);

        Object[] argsv = (Object[]) args;

        Matcher match = GETCLASS_PATTERN.matcher( methodName );
        // look for getXXXXClass(y)
        match.find();
        if (match.matches()) {
            System.out.println("get<"+match.group(2)+">Class");
            if (argsv.length > 0) {
                if ((argsv.length != 1) || !(argsv[0] instanceof String)) {
                    throw new IllegalArgumentException("Dynamic method get<Artefact>Class(artefactName) requires a " +
                        "single String parameter");
                } else {
                    return getArtefact(match.group(2), argsv[0].toString());
                }
            } else {
                // It's a no-param getter
                return super.invokeMethod(methodName, args);
            }
        } else {
            // look for isXXXXClass(y)
            match = ISCLASS_PATTERN.matcher( methodName );
            // find match
            match.find();
            if (match.matches()) {
                if ((argsv.length != 1) || !(argsv[0] instanceof Class)) {
                    throw new IllegalArgumentException("Dynamic method is<Artefact>Class(artefactClass) requires a " +
                        "single Class parameter");
                } else {
                    return Boolean.valueOf(isArtefactOfType(match.group(2), (Class)argsv[0]));
                }
            } else {
                // look for getXXXXClasses
                match = GETCLASSESMETH_PATTERN.matcher( methodName);
                // find match
                match.find();
                if (match.matches()) {
                    String artefactName = GrailsClassUtils.getClassNameRepresentation(match.group(2));
                    if (artefactHandlersByName.containsKey(artefactName)) {
                        return getArtefacts(match.group(2));
                    } else {
                        throw new IllegalArgumentException("Dynamic method get<Artefact>Classes() called for " +
                            "unrecognized artefact: "+match.group(2));
                    }
                } else {
                    return super.invokeMethod(methodName, args);
                }
            }
        }
    }

    /**
     * <p>Override property access and hit on xxxxClasses to return class arrays of artefacts</p>
     * @param propertyName
     * @return
     */
    public Object getProperty(String propertyName) {
        System.out.println("in getprop: "+propertyName);

        // look for getXXXXClasses
        final Matcher match = GETCLASSESPROP_PATTERN.matcher( propertyName );
        // find match
        match.find();
        if (match.matches()) {
            String artefactName = GrailsClassUtils.getClassNameRepresentation(match.group(1));
            System.out.println("matches getclasses: "+match.group(1)+ " / " + artefactName);
            if (artefactHandlersByName.containsKey(artefactName)) {
                return getArtefacts(artefactName);
            }
        }
        return super.getProperty(propertyName);    
    }
}
