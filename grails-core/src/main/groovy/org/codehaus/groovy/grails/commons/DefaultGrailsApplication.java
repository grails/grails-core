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

import grails.util.Environment;
import grails.util.GrailsNameUtils;
import grails.util.GrailsUtil;
import grails.util.Metadata;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObjectSupport;
import groovy.util.ConfigObject;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper;
import org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.codehaus.groovy.grails.documentation.DocumentationContext;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAwareBeanPostProcessor;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Default implementation of the GrailsApplication interface that manages application loading,
 * state, and artefact instances.
 * <p/>
 * Upon loading this GrailsApplication will inspect each class using its registered ArtefactHandler instances. Each
 * ArtefactHandler provides knowledge about the conventions used to establish its artefact type. For example
 * controllers use the ControllerArtefactHandler to establish this knowledge.
 * <p/>
 * New ArtefactHandler instances can be registered with the GrailsApplication thus allowing application extensibility.
 *
 * @author Marc Palmer
 * @author Steven Devijver
 * @author Graeme Rocher
 *
 * @see org.codehaus.groovy.grails.plugins.GrailsPluginManager
 * @see org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
 * @see org.codehaus.groovy.grails.commons.ArtefactHandler
 * @see org.codehaus.groovy.grails.commons.ArtefactInfo
 * @since 0.1
 */
public class DefaultGrailsApplication extends GroovyObjectSupport implements GrailsApplication, BeanClassLoaderAware {

    protected static final Pattern GETCLASSESPROP_PATTERN = Pattern.compile("(\\w+)(Classes)");
    protected static final Pattern GETCLASSESMETH_PATTERN = Pattern.compile("(get)(\\w+)(Classes)");
    protected static final Pattern ISCLASS_PATTERN = Pattern.compile("(is)(\\w+)(Class)");
    protected static final Pattern GETCLASS_PATTERN = Pattern.compile("(get)(\\w+)Class");

    protected ClassLoader cl;

    protected Class<?>[] allClasses = new Class[0];
    protected static Log log = LogFactory.getLog(DefaultGrailsApplication.class);
    protected ApplicationContext parentContext;
    protected ApplicationContext mainContext;

    protected List<Class<?>> loadedClasses = new ArrayList<Class<?>>();
    protected GrailsResourceLoader resourceLoader;
    protected ArtefactHandler[] artefactHandlers;
    protected Map<String, ArtefactHandler> artefactHandlersByName = new HashMap<String, ArtefactHandler>();
    protected List<Class<?>> allArtefactClasses = new ArrayList<Class<?>>();
    protected Map<String, ArtefactInfo> artefactInfo = new HashMap<String, ArtefactInfo>();
    protected Class<?>[] allArtefactClassesArray;
    protected Metadata applicationMeta = Metadata.getCurrent();
    protected Resource[] resources;
    protected boolean initialised = false;
    protected ConfigObject config;
    protected Map flatConfig = Collections.emptyMap();

    /**
     * Creates a new empty Grails application.
     */
    public DefaultGrailsApplication() {
        cl = new GroovyClassLoader();
    }

    /**
     * Creates a new GrailsApplication instance using the given classes and GroovyClassLoader.
     *
     * @param classes     The classes that make up the GrailsApplication
     * @param classLoader The GroovyClassLoader to use
     */
    public DefaultGrailsApplication(final Class<?>[] classes, ClassLoader classLoader) {
        Assert.notNull(classes, "Constructor argument 'classes' cannot be null");

        loadedClasses.addAll(Arrays.asList(classes));
        allClasses = classes;
        cl = classLoader;
    }

    /**
     * Constructs a GrailsApplication with the given set of groovy sources specified as Spring Resource instances.
     *
     * @param resources An array or Groovy sources provides by Spring Resource instances
     */
    public DefaultGrailsApplication(final Resource[] resources) {
        this(new GrailsResourceLoader(resources));
    }

    public DefaultGrailsApplication(GrailsResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;

        try {
            loadGrailsApplicationFromResources(resourceLoader.getResources());
        }
        catch (IOException e) {
            throw new GrailsConfigurationException("I/O exception loading Grails: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("rawtypes")
    protected void loadGrailsApplicationFromResources(@SuppressWarnings("hiding") Resource[] resources) throws IOException {
        GrailsResourceHolder resourceHolder = new GrailsResourceHolder();
        cl = configureClassLoader();
        GroovyClassLoader gcl = (GroovyClassLoader)cl;

        Collection loadedResources = new ArrayList();
        loadedClasses = new ArrayList<Class<?>>();

        try {
            for (int i = 0; resources != null && i < resources.length; i++) {

                if (!loadedResources.contains(resources[i])) {
                    try {
                        String className = resourceHolder.getClassName(resources[i]);
                        log.debug("Loading groovy file from resource loader :[" + resources[i].getFile().getAbsolutePath() + "] with name [" + className + "]");
                        if (!StringUtils.isBlank(className)) {

                            Class<?> c = gcl.loadClass(className, true, false);
                            Assert.notNull(c, "Groovy Bug! GCL loadClass method returned a null class!");

                            if (!loadedClasses.contains(c)) {
                                loadedClasses.add(c);
                            }
                            log.debug("Added Groovy class [" + c + "] to loaded classes");
                            loadedResources = resourceLoader.getLoadedResources();
                        }
                    }
                    catch (ClassNotFoundException e) {
                        log.error("The class ["+e.getMessage()+"] was not found when attempting to load Grails application. Skipping.");
                    }
                }
                else {
                    Class<?> c = null;
                    try {
                        log.debug("Loading groovy file from class loader :[" +
                                resources[i].getFile().getAbsolutePath() + "]");
                        c = cl.loadClass(resourceHolder.getClassName(resources[i]));
                    }
                    catch (ClassNotFoundException e) {
                        log.error("Class not found attempting to load class " + e.getMessage(), e);
                    }

                    if (c != null) {
                        loadedClasses.add(c);
                    }
                    log.debug("Added Groovy class [" + c + "] to loaded classes");
                }
            }
        }
        catch (CompilationFailedException e) {
            if (Environment.getCurrent() == Environment.DEVELOPMENT) {
                // if we're in the development environement then there is no point in this exception propagating up the stack as it
                // just clouds the actual error so log it as fatal and kill the server
                log.fatal("Compilation error loading Grails application: " + e.getMessage(), e);
                System.exit(1);
            }
            else {
                throw e;
            }
        }
    }

    /**
     * Initialises the default set of ArtefactHandler instances.
     *
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
    protected void initArtefactHandlers() {

        final DomainClassArtefactHandler domainClassArtefactHandler = new AnnotationDomainClassArtefactHandler();
        if (!hasArtefactHandler(domainClassArtefactHandler.getType())) {
            registerArtefactHandler(domainClassArtefactHandler);
        }

        final ControllerArtefactHandler controllerArtefactHandler = new ControllerArtefactHandler();
        if (!hasArtefactHandler(controllerArtefactHandler.getType())) {
            registerArtefactHandler(controllerArtefactHandler);
        }

        final ServiceArtefactHandler serviceArtefactHandler = new ServiceArtefactHandler();
        if (!hasArtefactHandler(serviceArtefactHandler.getType())) {
            registerArtefactHandler(serviceArtefactHandler);
        }

        final TagLibArtefactHandler tagLibArtefactHandler = new TagLibArtefactHandler();
        if (!hasArtefactHandler(tagLibArtefactHandler.getType())) {
            registerArtefactHandler(tagLibArtefactHandler);
        }

        final BootstrapArtefactHandler bootstrapArtefactHandler = new BootstrapArtefactHandler();
        if (!hasArtefactHandler(bootstrapArtefactHandler.getType())) {
            registerArtefactHandler(bootstrapArtefactHandler);
        }

        final CodecArtefactHandler codecArtefactHandler = new CodecArtefactHandler();
        if (!hasArtefactHandler(codecArtefactHandler.getType())) {
            registerArtefactHandler(codecArtefactHandler);
        }

        final UrlMappingsArtefactHandler urlMappingsArtefactHandler = new UrlMappingsArtefactHandler();
        if (!hasArtefactHandler(urlMappingsArtefactHandler.getType())) {
            registerArtefactHandler(urlMappingsArtefactHandler);
        }

        updateArtefactHandlers();
    }

    private void updateArtefactHandlers() {
        // Cache the list as an array
        artefactHandlers = artefactHandlersByName.values().toArray(
                new ArtefactHandler[artefactHandlersByName.size()]);
    }

    /**
     * Configures a GroovyClassLoader for the given GrailsInjectionOperation and GrailsResourceLoader.
     *
     * @return A GroovyClassLoader
     */
    protected GroovyClassLoader configureClassLoader() {

        final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();

        GrailsAwareClassLoader gcl = new GrailsAwareClassLoader(contextLoader);
        if (resourceLoader != null) {
            gcl.setResourceLoader(resourceLoader);
        }
        cl = gcl;

        try {
            Thread.currentThread().setContextClassLoader(cl);
        }
        catch (AccessControlException e) {
            // container doesn't allow this, in WAR deployment this shouldn't be an issue
        }

        return gcl;
    }

    /**
     * Returns all the classes identified as artefacts by ArtefactHandler instances.
     *
     * @return An array of classes
     */
    public Class<?>[] getAllArtefacts() {
        return allArtefactClassesArray;
    }

    protected Class<?>[] populateAllClasses() {
        allClasses = loadedClasses.toArray(new Class[loadedClasses.size()]);
        return allClasses;
    }

    /**
     * Configures the loaded classes within the GrailsApplication instance using the
     * registered ArtefactHandler instances.
     *
     * @param classes The classes to configure
     */
    protected void configureLoadedClasses(Class<?>[] classes) {

        initArtefactHandlers();

        artefactInfo.clear();
        allArtefactClasses.clear();
        allArtefactClassesArray = null;
        allClasses = classes;

        // first load the domain classes
        log.debug("Going to inspect artefact classes.");
        for (final Class<?> theClass : classes) {
            log.debug("Inspecting [" + theClass.getName() + "]");
            if (allArtefactClasses.contains(theClass)) {
                continue;
            }

            // check what kind of artefact it is and add to corrent data structure
            for (ArtefactHandler artefactHandler : artefactHandlers) {
                if (artefactHandler.isArtefact(theClass)) {
                    log.debug("Adding artefact " + theClass + " of kind " + artefactHandler.getType());
                    GrailsClass gclass = addArtefact(artefactHandler.getType(), theClass);
                    // Also maintain set of all artefacts (!= all classes loaded)
                    allArtefactClasses.add(theClass);

                    // Update per-artefact cache
                    DefaultArtefactInfo info = getArtefactInfo(artefactHandler.getType(), true);
                    info.addGrailsClass(gclass);
                    break;
                }
            }
        }

        refreshArtefactGrailsClassCaches();

        allArtefactClassesArray = allArtefactClasses.toArray(new Class[allArtefactClasses.size()]);

        // Tell all artefact handlers to init now we've worked out which classes are which artefacts
        for (ArtefactHandler artefactHandler : artefactHandlers) {
            initializeArtefacts(artefactHandler);
        }
    }

    /**
     * Tell all our artefact info objects to update their internal state after we've added a bunch of classes.
     */
    protected void refreshArtefactGrailsClassCaches() {
        for (Object o : artefactInfo.values()) {
            ((DefaultArtefactInfo)o).updateComplete();
        }
    }

    protected void addToLoaded(Class<?> clazz) {
        loadedClasses.add(clazz);
        populateAllClasses();
    }

    public GrailsResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public ClassLoader getClassLoader() {
        return cl;
    }

    public ConfigObject getConfig() {
        if (this.config == null) {
            setConfig(ConfigurationHelper.loadConfigFromClasspath(this));
        }
        return config;
    }

    public void setConfig(ConfigObject config) {
        this.config = config;
        if(config != null) {
            this.flatConfig = config.flatten();
        } else {
        	this.flatConfig = Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getFlatConfig() {
        return flatConfig;
    }

    /**
     * Retrieves the number of artefacts registered for the given artefactType as defined by the ArtefactHandler.
     *
     * @param artefactType The type of the artefact as defined by the ArtefactHandler
     * @return The number of registered artefacts
     */
    protected int getArtefactCount(String artefactType) {
        ArtefactInfo info = getArtefactInfo(artefactType);
        return info == null ? 0 : info.getClasses().length;
    }

    /**
     * Retrieves all classes loaded by the GrailsApplication.
     *
     * @return All classes loaded by the GrailsApplication
     */
    public Class<?>[] getAllClasses() {
        return allClasses;
    }

    public ApplicationContext getMainContext() {
        return mainContext;
    }

    public void setMainContext(ApplicationContext context) {
        mainContext = context;
    }

    /**
     * Sets the parent ApplicationContext for the GrailsApplication.
     *
     * @param applicationContext The ApplicationContext
     * @throws BeansException Thrown when an error occurs setting the ApplicationContext
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        parentContext = applicationContext;
    }

    /**
     * Retrieves the parent ApplicationContext for this GrailsApplication.
     *
     * @return The parent ApplicationContext
     */
    public ApplicationContext getParentContext() {
        return parentContext;
    }

    /**
     * Retrieves a class from the GrailsApplication for the given name.
     *
     * @param className The class name
     * @return Either the Class instance or null if it doesn't exist
     */
    public Class<?> getClassForName(String className) {
        if (StringUtils.isBlank(className)) {
            return null;
        }

        for (Class<?> c : allClasses) {
            if (c.getName().equals(className)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Refreshes constraints defined by the DomainClassArtefactHandler.
     *
     * TODO: Move this out of GrailsApplication
     */
    public void refreshConstraints() {
        ArtefactInfo info = getArtefactInfo(DomainClassArtefactHandler.TYPE, true);
        GrailsClass[] domainClasses = info.getGrailsClasses();
        for (GrailsClass domainClass : domainClasses) {
            ((GrailsDomainClass) domainClass).refreshConstraints();
        }
    }

    /**
     * Refreshes this GrailsApplication, rebuilding all of the artefact definitions as
     * defined by the registered ArtefactHandler instances.
     */
    public void refresh() {
        if (cl instanceof GroovyClassLoader) {
            configureLoadedClasses(((GroovyClassLoader)cl).getLoadedClasses());
        }
    }

    public void rebuild() {
        initialised = false;
        loadedClasses.clear();
        initArtefactHandlers();

        if (GrailsUtil.isDevelopmentEnv()) {
            try {
                loadGrailsApplicationFromResources(resources);
                initialise();
            }
            catch (IOException e) {
                throw new GrailsConfigurationException(
                        "I/O error rebuilding GrailsApplication: " + e.getMessage(), e);
            }
        }
        else {
            throw new IllegalStateException("Cannot rebuild GrailsApplication when not in development mode!");
        }
    }

    /**
     * Retrieves the Spring Resource that was used to load the given Class.
     *
     * @param theClazz The class
     * @return Either a Spring Resource or null if no Resource was found for the given class
     */
    public Resource getResourceForClass(@SuppressWarnings("rawtypes") Class theClazz) {
        if (resourceLoader == null) {
            return null;
        }
        return resourceLoader.getResourceForClass(theClazz);
    }

    /**
     * Returns true if the given class is an artefact identified by one of the registered
     * ArtefactHandler instances. Uses class name equality to handle class reloading
     *
     * @param theClazz The class to check
     * @return True if it is an artefact
     */
    public boolean isArtefact(@SuppressWarnings("rawtypes") Class theClazz) {
        String className = theClazz.getName();
        for (Class<?> artefactClass : allArtefactClasses) {
            if (className.equals(artefactClass.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the specified class is of the given artefact type as defined by the ArtefactHandler.
     *
     * @param artefactType The type of the artefact
     * @param theClazz     The class
     * @return True if it is of the specified artefactType
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
    public boolean isArtefactOfType(String artefactType, @SuppressWarnings("rawtypes") Class theClazz) {
        ArtefactHandler handler = artefactHandlersByName.get(artefactType);
        if (handler == null) {
            throw new GrailsConfigurationException(
                    "Unable to locate arefact handler for specified type: " + artefactType);
        }

        return handler.isArtefact(theClazz);
    }

    /**
     * Returns true if the specified class name is of the given artefact type as defined by the ArtefactHandler.
     *
     * @param artefactType The type of the artefact
     * @param className    The class name
     * @return True if it is of the specified artefactType
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
    public boolean isArtefactOfType(String artefactType, String className) {
        return getArtefact(artefactType, className) != null;
    }

    /**
     * Retrieves an artefact for the given type and name.
     *
     * @param artefactType The artefact type as defined by a registered ArtefactHandler
     * @param name         The name of the class
     * @return A GrailsClass instance or null if none could be found for the given artefactType and name
     */
    public GrailsClass getArtefact(String artefactType, String name) {
        ArtefactInfo info = getArtefactInfo(artefactType);
        return info == null ? null : info.getGrailsClass(name);
    }

    public ArtefactHandler getArtefactType(@SuppressWarnings("rawtypes") Class theClass) {
        for (ArtefactHandler artefactHandler : artefactHandlers) {
            if (artefactHandler.isArtefact(theClass)) {
                return artefactHandler;
            }
        }
        return null;
    }

    protected GrailsClass getFirstArtefact(String artefactType) {
        ArtefactInfo info = getArtefactInfo(artefactType);
        // This will throw AIOB if we have none
        return info == null ? null : info.getGrailsClasses()[0];
    }

    /**
     * Returns all of the GrailsClass instances for the given artefactType as defined by the ArtefactHandler
     *
     * @param artefactType The type of the artefact defined by the ArtefactHandler
     * @return An array of classes for the given artefact
     */
    public GrailsClass[] getArtefacts(String artefactType) {
        if (!isWarDeployed()) {
            DocumentationContext.getInstance().setArtefactType(artefactType);
        }
        return getArtefactInfo(artefactType, true).getGrailsClasses();
    }

    // This is next call is equiv to getControllerByURI / getTagLibForTagName
    public GrailsClass getArtefactForFeature(String artefactType, Object featureID) {
        return artefactHandlersByName.get(artefactType).getArtefactForFeature(featureID);
    }

    /**
     * Adds an artefact of the given type for the given Class.
     *
     * @param artefactType  The type of the artefact as defined by a ArtefactHandler instance
     * @param artefactClass A Class instance that matches the type defined by the ArtefactHandler
     * @return The GrailsClass if successful or null if it couldn't be added
     * @throws GrailsConfigurationException If the specified Class is not the same as the type defined by the ArtefactHandler
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
    public GrailsClass addArtefact(String artefactType, @SuppressWarnings("rawtypes") Class artefactClass) {
        return addArtefact(artefactType, artefactClass, false);
    }

    /**
     * Adds an artefact of the given type for the given GrailsClass.
     *
     * @param artefactType        The type of the artefact as defined by a ArtefactHandler instance
     * @param artefactGrailsClass A GrailsClass instance that matches the type defined by the ArtefactHandler
     * @return The GrailsClass if successful or null if it couldn't be added
     * @throws GrailsConfigurationException If the specified GrailsClass is not the same as the type defined by the ArtefactHandler
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
    public GrailsClass addArtefact(String artefactType, GrailsClass artefactGrailsClass) {
        // @todo should we filter abstracts here?
        if (Modifier.isAbstract(artefactGrailsClass.getClazz().getModifiers())) {
            return null;
        }

        ArtefactHandler handler = artefactHandlersByName.get(artefactType);
        if (handler.isArtefactGrailsClass(artefactGrailsClass)) {
            // Store the GrailsClass in cache
            DefaultArtefactInfo info = getArtefactInfo(artefactType, true);
            info.addGrailsClass(artefactGrailsClass);
            info.updateComplete();

            initializeArtefacts(artefactType);

            return artefactGrailsClass;
        }

        throw new GrailsConfigurationException("Cannot add " + artefactType + " class [" +
                artefactGrailsClass + "]. It is not a " + artefactType + "!");
    }

    /**
     * Registers a new ArtefactHandler that is responsible for identifying and managing a
     * particular artefact type that is defined by some convention.
     *
     * @param handler The ArtefactHandler to regster
     */
    public void registerArtefactHandler(ArtefactHandler handler) {
        GrailsApplicationAwareBeanPostProcessor.processAwareInterfaces(this, handler);
        artefactHandlersByName.put(handler.getType(), handler);
        updateArtefactHandlers();
    }

    public boolean hasArtefactHandler(String type) {
        return artefactHandlersByName.containsKey(type);
    }

    public ArtefactHandler[] getArtefactHandlers() {
        return artefactHandlers;
    }

    public ArtefactHandler getArtefactHandler(String type) {
        return artefactHandlersByName.get(type);
    }

    /**
     * Re-initialize the artefacts of the specified type. This gives handlers a chance to update caches etc.
     *
     * @param artefactType The type of artefact to init
     */
    protected void initializeArtefacts(String artefactType) {
        initializeArtefacts(artefactHandlersByName.get(artefactType));
    }

    /**
     * Clears the application returning it to an empty state. Very dangerous method, use with caution.
     */
    public void clear() {
        artefactHandlersByName.clear();
        updateArtefactHandlers();
        artefactInfo.clear();
        initialise();
    }

    /**
     * Re-initialize the artefacts of the specified type. This gives handlers a chance to update caches etc.
     *
     * @param handler The handler to register
     */
    protected void initializeArtefacts(ArtefactHandler handler) {
        if (handler == null) {
            return;
        }

        ArtefactInfo info = getArtefactInfo(handler.getType());
        // Only init those that have data
        if (info != null) {
            //System.out.println("Initialising artefacts of kind " + handler.getType() + " with registered artefacts" + info.getGrailsClassesByName());
            handler.initialize(info);
        }
    }

    /**
     * Get or create the cache of classes for the specified artefact type.
     *
     * @param artefactType The name of an artefact type
     * @param create       Set to true if you want non-existent caches to be created
     * @return The cache of classes for the type, or null if no cache exists and create is false
     */
    protected DefaultArtefactInfo getArtefactInfo(String artefactType, boolean create) {
        DefaultArtefactInfo cache = (DefaultArtefactInfo) artefactInfo.get(artefactType);
        if (cache == null && create) {
            cache = new DefaultArtefactInfo();
            artefactInfo.put(artefactType, cache);
            cache.updateComplete();
        }
        return cache;
    }

    /**
     * Get the cache of classes for the specified artefact type.
     *
     * @param artefactType The name of an artefact type
     * @return The cache of classes for the type, or null if no cache exists
     */
    public ArtefactInfo getArtefactInfo(String artefactType) {
        return getArtefactInfo(artefactType, false);
    }

    /**
     * <p>Overrides method invocation to return dynamic artefact methods.</p>
     * <p>We will support getXXXXClasses() and isXXXXClass(class)</p>
     *
     * @param methodName The name of the method
     * @param args       The arguments to the method
     * @return The return value of the method
     * @todo Need to add matches for add<Artefact>Class(java.lang.Class) and add<Artefact>Class(GrailsClass)
     */
    @Override
    public Object invokeMethod(String methodName, Object args) {

        Object[] argsv = (Object[]) args;

        Matcher match = GETCLASS_PATTERN.matcher(methodName);
        // look for getXXXXClass(y)
        match.find();
        if (match.matches()) {
            if (argsv.length > 0) {
                if (argsv[0] instanceof CharSequence) argsv[0] = argsv[0].toString();
                if ((argsv.length != 1) || !(argsv[0] instanceof String)) {
                    throw new IllegalArgumentException(
                            "Dynamic method get<Artefact>Class(artefactName) requires a single String parameter");
                }
                return getArtefact(match.group(2), argsv[0].toString());
            }

            // It's a no-param getter
            return super.invokeMethod(methodName, args);
        }

        // look for isXXXXClass(y)
        match = ISCLASS_PATTERN.matcher(methodName);
        // find match
        match.find();
        if (match.matches()) {
            if ((argsv.length != 1) || !(argsv[0] instanceof Class<?>)) {
                throw new IllegalArgumentException(
                        "Dynamic method is<Artefact>Class(artefactClass) requires a single Class parameter");
            }

            return isArtefactOfType(match.group(2), (Class<?>) argsv[0]);
        }

        // look for getXXXXClasses
        match = GETCLASSESMETH_PATTERN.matcher(methodName);
        // find match
        match.find();
        if (match.matches()) {
            String artefactName = GrailsNameUtils.getClassNameRepresentation(match.group(2));
            if (artefactHandlersByName.containsKey(artefactName)) {
                return getArtefacts(match.group(2));
            }

            throw new IllegalArgumentException("Dynamic method get<Artefact>Classes() called for " +
                    "unrecognized artefact: " + match.group(2));
        }

        return super.invokeMethod(methodName, args);
    }

    /**
     * Override property access and hit on xxxxClasses to return class arrays of artefacts.
     *
     * @param propertyName The name of the property, if it ends in *Classes then match and invoke internal ArtefactHandler
     * @return All the artifacts or delegate to super.getProperty
     */
    @Override
    public Object getProperty(String propertyName) {
        // look for getXXXXClasses
        final Matcher match = GETCLASSESPROP_PATTERN.matcher(propertyName);
        // find match
        match.find();
        if (match.matches()) {
            String artefactName = GrailsNameUtils.getClassNameRepresentation(match.group(1));
            if (artefactHandlersByName.containsKey(artefactName)) {
                return getArtefacts(artefactName);
            }
        }
        return super.getProperty(propertyName);
    }

    public void initialise() {
        // get all the classes that were loaded
        if (log.isDebugEnabled()) {
            log.debug("loaded classes: [" + loadedClasses + "]");
        }
        Class<?>[] classes = populateAllClasses();
        configureLoadedClasses(classes);
        initialised = true;
    }

    public boolean isInitialised() {
        return initialised;
    }

    public Metadata getMetadata() {
        return applicationMeta;
    }

    public GrailsClass getArtefactByLogicalPropertyName(String type, String logicalName) {
        ArtefactInfo info = getArtefactInfo(type);
        return info == null ? null : info.getGrailsClassByLogicalPropertyName(logicalName);
    }

    public void addArtefact(@SuppressWarnings("rawtypes") Class artefact) {
        for (ArtefactHandler artefactHandler : artefactHandlers) {
            if (artefactHandler.isArtefact(artefact)) {
                addArtefact(artefactHandler.getType(), artefact);
            }
        }
    }

    public boolean isWarDeployed() {
        return getMetadata().isWarDeployed();
    }

    public void setBeanClassLoader(ClassLoader classLoader) {
        // do nothing
    }

    public void addOverridableArtefact(@SuppressWarnings("rawtypes") Class artefact) {
        for (ArtefactHandler artefactHandler : artefactHandlers) {
            if (artefactHandler.isArtefact(artefact)) {
                addOverridableArtefact(artefactHandler.getType(), artefact);
            }
        }
    }

    public void configChanged() {
        ConfigObject co = getConfig();
        // not thread safe
        this.flatConfig = co.flatten();
        final ArtefactHandler[] handlers = getArtefactHandlers();
        for (ArtefactHandler handler : handlers) {
            if (handler instanceof GrailsConfigurationAware) {
                ((GrailsConfigurationAware)handler).setConfiguration(co);
            }
        }
    }

    /**
     * Adds an artefact of the given type for the given Class.
     *
     * @param artefactType  The type of the artefact as defined by a ArtefactHandler instance
     * @param artefactClass A Class instance that matches the type defined by the ArtefactHandler
     * @return The GrailsClass if successful or null if it couldn't be added
     * @throws GrailsConfigurationException If the specified Class is not the same as the type defined by the ArtefactHandler
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
    public GrailsClass addOverridableArtefact(String artefactType, @SuppressWarnings("rawtypes") Class artefactClass) {
        return addArtefact(artefactType, artefactClass, true);
    }

    protected GrailsClass addArtefact(String artefactType, Class<?> artefactClass, boolean overrideable) {
        ArtefactHandler handler = artefactHandlersByName.get(artefactType);
        if (handler.isArtefact(artefactClass)) {
            GrailsClass artefactGrailsClass = handler.newArtefactClass(artefactClass);
            artefactGrailsClass.setGrailsApplication(this);

            // Store the GrailsClass in cache
            DefaultArtefactInfo info = getArtefactInfo(artefactType, true);
            if (overrideable) {
                info.addOverridableGrailsClass(artefactGrailsClass);
            }
            else {
                info.addGrailsClass(artefactGrailsClass);
            }
            info.updateComplete();

            addToLoaded(artefactClass);

            if (isInitialised()) {
                initializeArtefacts(artefactType);
            }

            return artefactGrailsClass;
        }

        throw new GrailsConfigurationException("Cannot add " + artefactType + " class [" +
                artefactClass + "]. It is not a " + artefactType + "!");
    }
}
