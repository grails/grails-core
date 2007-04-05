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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Modifier;
import java.security.CodeSource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of the GrailsApplication interface that manages application loading,
 * state, and artifact instances.
 *
 * Upon loading this GrailsApplication will insepct each class using its registered ArtefactHandler instances. Each
 * ArtefactHandler provides knownledge about the conventions used to establish its artefact type. For example
 * controllers use the ControllerArtefactHandler to establish this knowledge.
 *
 * New ArtefactHandler instances can be registed with the GrailsApplication thus allowing application extensibility.
 *
 * @todo Review all synching on new artefact cache stuff, what is sync policy for grailsapplication?
 *
 * @see org.codehaus.groovy.grails.plugins.GrailsPluginManager
 * @see org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
 * @see org.codehaus.groovy.grails.commons.ArtefactHandler
 * @see org.codehaus.groovy.grails.commons.ArtefactInfo
 * 
 * @author Marc Palmer
 * @author Steven Devijver
 * @author Graeme Rocher
 *
 * @since 0.1
 *
 *        Created: Jul 2, 2005
 */
public class DefaultGrailsApplication extends GroovyObjectSupport implements GrailsApplication {

    private static final Pattern GETCLASSESPROP_PATTERN = Pattern.compile("(\\w+)(Classes)");
    private static final Pattern GETCLASSESMETH_PATTERN = Pattern.compile("(get)(\\w+)(Classes)");
    private static final Pattern ISCLASS_PATTERN = Pattern.compile("(is)(\\w+)(Class)");
    private static final Pattern GETCLASS_PATTERN = Pattern.compile("(get)(\\w+)Class");
    private static final String PROJECT_META_FILE = "application.properties";

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
    private Map applicationMeta;

    /**
     * Creates a new empty Grails application
     */
    public DefaultGrailsApplication() {
        this.cl = new GroovyClassLoader();
    }

    /**
     * Creates a new GrailsApplication instance using the given classes and GroovyClassLoader
     *
     * @param classes The classes that make up the GrailsApplication
     * @param classLoader The GroovyClassLoader to use
     */
    public DefaultGrailsApplication(final Class[] classes, GroovyClassLoader classLoader) {
        if (classes == null) {
            throw new IllegalArgumentException("Constructor argument 'classes' cannot be null");
        }

        configureLoadedClasses(classes);
        this.cl = classLoader;
    }


    /**
     * Constructs a GrailsApplication with the given set of groovy sources specified as Spring Resource instances
     *
     * @param resources An array or Groovy sources provides by Spring Resource instances
     * @throws IOException Thrown when an error occurs reading a Groovy source
     */
    public DefaultGrailsApplication(final Resource[] resources) throws IOException {
        this(resources, null);
    }

    /**
     * <p>Constructs a GrailsApplication with the given set of groovy sources specified as Spring Resource instances</p>
     *
     * <p>An optional GrailsInjectionOperation can be provided which is registered with the GroovyClassLoader. A GrailsInjectionOperation
     * is a class that interacts with the Groovy compiler to inject code into the Groovy AST before compilation</p>
     *
     * <p>This is how Grails injects the id and version into Grails domain classes before compilation for example</p>
     *
     * @param resources An array or Groovy sources provides by Spring Resource instances
     * @param injectionOperation The GrailsInjectionOperation to use
     *
     * @throws IOException Thrown when an error occurs reading a Groovy source
     */
    public DefaultGrailsApplication(final Resource[] resources,
        final GrailsInjectionOperation injectionOperation) throws IOException {
        super();

        log.debug("Loading Grails application.");

        loadMetadata();

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

    }

    private void loadMetadata()
    {
        final Properties meta = new Properties();
        Resource r = new ClassPathResource(PROJECT_META_FILE);
        try
        {
            meta.load( r.getInputStream());
        }
        catch (IOException e)
        {
            log.warn("No application metadata file found at "+r);
        }
        applicationMeta = Collections.unmodifiableMap(meta);
    }

    /**
     * Initialises the default set of ArtefactHandler instances
     *
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
    private void initArtefactHandlers() {
        registerArtefactHandler( new DomainClassArtefactHandler());
        registerArtefactHandler( new ControllerArtefactHandler());
        registerArtefactHandler( new ServiceArtefactHandler());
        registerArtefactHandler( new TagLibArtefactHandler());
        registerArtefactHandler( new TaskArtefactHandler());
        registerArtefactHandler( new BootstrapArtefactHandler());
        registerArtefactHandler( new CodecArtefactHandler());
        registerArtefactHandler( new DataSourceArtefactHandler());
        registerArtefactHandler( new UrlMappingsArtefactHandler());

        // Cache the list as an array
        this.artefactHandlers = ((ArtefactHandler[]) this.artefactHandlersByName.values().toArray(
            new ArtefactHandler[artefactHandlersByName.size()]));
    }

    /**
     * Configures a GroovyClassLoader for the given GrailsInjectionOperation and GrailsResourceLoader
     *
     * @param injectionOperation The GrailsInjectionOperation
     * @param resourceLoader The GrailsResourceLoader
     * @return A GroovyClassLoader
     */
    private GroovyClassLoader configureClassLoader(final GrailsInjectionOperation injectionOperation,
        final GrailsResourceLoader resourceLoader) {
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

    /**
     * Returns all the classes identified as artefacts by ArtefactHandler instances
     *
     * @return An array of classes
     */
    public Class[] getAllArtefacts() {
        return allArtefactClassesArray;
    }

    private Class[] populateAllClasses() {
        this.allClasses = (Class[]) loadedClasses.toArray(new Class[loadedClasses.size()]);
        return allClasses;
    }

    /**
     * Configures the loaded classes within the GrailsApplication instance using the registered ArtefactHandler instances
     *
     * @param classes The classes to configure
     */
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

    public GrailsResourceLoader getResourceLoader() {
        return resourceLoader;
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

    /**
     * Retrieves the number of artefacts registered for the given artefactType as defined by the ArtefactHandler
     *
     * @param artefactType The type of the artefact as defined by the ArtefactHandler
     * 
     * @return The number of registered artefacts
     */
    private int getArtefactCount(String artefactType) {
        ArtefactInfo info = getArtefactInfo(artefactType);
        return info == null ? 0 : info.getClasses().length;
    }

    /**
     * Retrieves all classes loaded by the GrailsApplication
     *
     * @return All classes loaded by the GrailsApplication
     */
    public Class[] getAllClasses() {
        return this.allClasses;
    }

    /**
     * Sets the parent ApplicationContext for the GrailsApplication
     *
     * @param applicationContext The ApplicationContext
     * 
     * @throws BeansException Thrown when an error occurs setting the ApplicationContext
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.parentContext = applicationContext;
    }

    /**
     * Retrieves the parent ApplicationContext for this GrailsApplication
     * @return The parent ApplicationContext
     */
    public ApplicationContext getParentContext() {
        return this.parentContext;
    }

    /**
     * Retrieves a class from the GrailsApplication for the given name
     *
     * @param className The class name
     * 
     * @return Either the java.lang.Class instance or null if it doesn't exist
     */
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

    /**
     * Refreshes constraints defined by the DomainClassArtefactHandler
     *
     * @TODO Move this out of GrailsApplication
     */
    public void refreshConstraints() {
        ArtefactInfo info = getArtefactInfo(DomainClassArtefactHandler.TYPE, true);
        GrailsClass[] domainClasses = info.getGrailsClasses();
        for (int i = 0; i < domainClasses.length; i++) {
            ((GrailsDomainClass)domainClasses[i]).refreshConstraints();
        }
    }

    /**
     * Refreshes this GrailsApplication, rebuilding all of the artefact definitions as defined by the registered ArtefactHandler instances
     */
    public void refresh() {
        configureLoadedClasses(this.cl.getLoadedClasses());
    }

    /**
     * Retrieves the Spring Resource that was used to load the given Class
     *
     * @param theClazz The class
     * @return Either a Spring Resource or null if no Resource was found for the given class
     */
    public Resource getResourceForClass(Class theClazz) {
        if (this.resourceLoader == null) {
            return null;
        }
        return this.resourceLoader.getResourceForClass(theClazz);
    }

    /**
     * Returns true if the given class is an artefact identified by one of the registered ArtefactHandler instances
     *
     * @param theClazz The class to check
     * @return True if it is an artefact
     */
    public boolean isArtefact(Class theClazz) {
        return this.allArtefactClasses.contains(theClazz);
    }

    /**
     * Returns true if the specified class is of the given artefact type as defined by the ArtefactHandler
     *
     * @param artefactType The type of the artefact
     * @param theClazz The class
     * @return True if it is of the specified artefactType
     *
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
    public boolean isArtefactOfType(String artefactType, Class theClazz) {
        return isArtefactOfType(artefactType, theClazz.getName());
    }

   /**
     * Returns true if the specified class name is of the given artefact type as defined by the ArtefactHandler
     *
     * @param artefactType The type of the artefact
     * @param className The class name
     * @return True if it is of the specified artefactType
     *
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
    public boolean isArtefactOfType(String artefactType, String className) {
        return getArtefact(artefactType, className) != null;
    }

    /**
     * Retrieves an artefact for the given type and nam
     *
     * @param artefactType The artefact type as defined by a registered ArtefactHandler
     * @param name The name of the class
     *
     * @return A GrailsClass instance or null if none could be found for the given artefactType and name
     */
    public GrailsClass getArtefact(String artefactType, String name) {
        ArtefactInfo info = getArtefactInfo(artefactType);
        return info == null ? null : info.getGrailsClass(name);
    }

    private GrailsClass getFirstArtefact(String artefactType) {
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
        ArtefactInfo info = getArtefactInfo(artefactType, true);
        return info.getGrailsClasses();
    }

    // This is next call is equiv to getControllerByURI / getTagLibForTagName
    public GrailsClass getArtefactForFeature(String artefactType, Object featureID) {
        ArtefactHandler handler = (ArtefactHandler) artefactHandlersByName.get(artefactType);
        return handler.getArtefactForFeature(featureID);
    }

    /**
     * Adds an artefact of the given type for the given Class.
     *
     *
     * @param artefactType The type of the artefact as defined by a ArtefactHandler instance
     * @param artefactClass A Class instance that matches the type defined by the ArtefactHandler
     *
     * @return The GrailsClass if successful or null if it couldn't be added
     *
     * @throws GrailsConfigurationException If the specified Class is not the same as the type defined by the ArtefactHandler
     *
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
    public GrailsClass addArtefact(String artefactType, Class artefactClass) {
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

    /**
     * Adds an artefact of the given type for the given GrailsClass.
     *
     * @param artefactType The type of the artefact as defined by a ArtefactHandler instance
     * @param artefactGrailsClass A GrailsClass instance that matches the type defined by the ArtefactHandler
     *
     * @return The GrailsClass if successful or null if it couldn't be added
     *  
     * @throws GrailsConfigurationException If the specified GrailsClass is not the same as the type defined by the ArtefactHandler
     *
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     */
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

    /**
     * Registers a new ArtefactHandler that is responsible for identifying and managing an particular artefact type that is defined by
     * some convention
     *
     * @param handler  The ArtefactHandler to regster
     */
    public void registerArtefactHandler(ArtefactHandler handler) {
        artefactHandlersByName.put(handler.getType(), handler);
    }

    public ArtefactHandler[] getArtefactHandlers() {
        return artefactHandlers;
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
     *
     * @param methodName The name of the method
     * @param args The arguments to the method
     *
     * @return The return value of the method
     *
     * @todo this is REALLY ugly
     * @todo Need to add matches for add<Artefact>Class(java.lang.Class) and add<Artefact>Class(GrailsClass) 
     */
    public Object invokeMethod(String methodName, Object args) {

        Object[] argsv = (Object[]) args;

        Matcher match = GETCLASS_PATTERN.matcher( methodName );
        // look for getXXXXClass(y)
        match.find();
        if (match.matches()) {
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
     * @param propertyName The name of the property, if it ends in *Classes then match and invoke internal ArtefactHandler
     * @return All the artifacts or delegate to super.getProperty
     */
    public Object getProperty(String propertyName) {
        // look for getXXXXClasses
        final Matcher match = GETCLASSESPROP_PATTERN.matcher( propertyName );
        // find match
        match.find();
        if (match.matches()) {
            String artefactName = GrailsClassUtils.getClassNameRepresentation(match.group(1));
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
        Class[] classes = populateAllClasses();
        configureLoadedClasses(classes);    	
    }

    public Map getMetadata()
    {
        return applicationMeta;
    }

}
