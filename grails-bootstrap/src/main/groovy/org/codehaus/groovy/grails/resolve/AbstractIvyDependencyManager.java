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
package org.codehaus.groovy.grails.resolve;

import grails.util.BuildSettings;
import grails.util.CollectionUtils;
import grails.util.GrailsNameUtils;
import grails.util.Metadata;
import groovy.lang.Closure;
import org.apache.ivy.core.module.descriptor.*;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageLogger;
import org.codehaus.groovy.grails.resolve.config.DependencyConfigurationConfigurer;
import org.codehaus.groovy.grails.resolve.config.DependencyConfigurationContext;
import org.codehaus.groovy.grails.resolve.maven.PomModuleDescriptorParser;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Base class for IvyDependencyManager with some logic implemented in Java.
 *
 * @author Graeme Rocher
 * @since 1.3
 */
public abstract class AbstractIvyDependencyManager {

    public static final String SNAPSHOT_CHANGING_PATTERN = ".*SNAPSHOT";

    /*
     * Out of the box Ivy configurations are:

     * - agent: Used to configure the JVM agent
     * - build: Dependencies for the build system only
     * - compile: Dependencies for the compile step
     * - runtime: Dependencies needed at runtime but not for compilation (see above)
     * - test: Dependencies needed for testing but not at runtime (see above)
     * - provided: Dependencies needed at development time, but not during WAR deployment
     */
    public static Configuration AGENT_CONFIGURATION  = new Configuration(
        "agent",
        Configuration.Visibility.PUBLIC,
        "Agent dependencies",
        new String[] {"default"},
        true, null);
    public static Configuration BUILD_CONFIGURATION  = new Configuration(
            "build",
            Configuration.Visibility.PUBLIC,
            "Build system dependencies",
            new String[] {"default"},
            true, null);

    public static Configuration COMPILE_CONFIGURATION = new Configuration(
            "compile",
            Configuration.Visibility.PUBLIC,
            "Compile time dependencies",
            new String[] {"default" },
            true, null);

    public static Configuration RUNTIME_CONFIGURATION = new Configuration(
            "runtime",
            Configuration.Visibility.PUBLIC,
            "Runtime time dependencies",
            new String[] {"compile"},
            true, null);

    public static Configuration TEST_CONFIGURATION = new Configuration(
            "test",
            Configuration.Visibility.PUBLIC,
            "Testing dependencies",
            new String[] {"runtime"},
            true, null);

    public static Configuration PROVIDED_CONFIGURATION = new Configuration(
            "provided",
            Configuration.Visibility.PUBLIC,
            "Dependencies provided by the container",
            new String[] {"default"},
            true, null);

    public static Configuration DOCS_CONFIGURATION = new Configuration(
            "docs",
            Configuration.Visibility.PUBLIC,
            "Dependencies for the documenation engine",
            new String[] {"build"},
            true, null);

    public static List<Configuration> ALL_CONFIGURATIONS = Arrays.asList(
            BUILD_CONFIGURATION,
            COMPILE_CONFIGURATION,
            RUNTIME_CONFIGURATION,
            TEST_CONFIGURATION,
            PROVIDED_CONFIGURATION,
            DOCS_CONFIGURATION);
    public static final ExcludeRule[] NO_EXCLUDE_RULES = new ExcludeRule[0];

    @SuppressWarnings("unchecked")
    Map<String, List<String>> configurationMappings = CollectionUtils.<String, List<String>>newMap(
       "runtime", Arrays.asList("default"),
       "build", Arrays.asList("default"),
       "agent", Arrays.asList("default"),
       "compile", Arrays.asList("default"),
       "provided", Arrays.asList("default"),
       "docs", Arrays.asList("default"),
       "test", Arrays.asList("default"));

    protected boolean includeSource;
    protected boolean includeJavadoc;
    protected String[] configurationNames = configurationMappings.keySet().toArray(
            new String[configurationMappings.size()]);
    protected Set<ModuleId> modules = new HashSet<ModuleId>();
    protected Set<ModuleRevisionId> dependencies = new HashSet<ModuleRevisionId>();
    protected Set<DependencyDescriptor> dependencyDescriptors = new HashSet<DependencyDescriptor>();
    protected Set<DependencyDescriptor> pluginDependencyDescriptors = new HashSet<DependencyDescriptor>();

    protected Set<String> metadataRegisteredPluginNames = new HashSet<String>();
    protected Map<String, Collection<ModuleRevisionId>> orgToDepMap = new HashMap<String, Collection<ModuleRevisionId>>();
    protected Collection<String> usedConfigurations = new ConcurrentLinkedQueue<String>();

    protected Map<String, DependencyDescriptor> pluginNameToDescriptorMap =
        new ConcurrentHashMap<String, DependencyDescriptor>();
    protected String applicationName;
    protected String applicationVersion;
    protected DefaultModuleDescriptor moduleDescriptor;
    protected boolean hasApplicationDependencies = false;
    protected boolean readPom = false;

    protected final IvySettings ivySettings;
    protected final BuildSettings buildSettings;
    protected final Metadata metadata;
    protected boolean legacyResolve = true;
    private boolean offline;

    private ChainResolver chainResolver;
    ResolveEngine resolveEngine;
    MessageLogger logger;

    public AbstractIvyDependencyManager(IvySettings ivySettings, BuildSettings buildSettings, Metadata metadata) {
        this.ivySettings = ivySettings;
        this.buildSettings = buildSettings;
        this.metadata = metadata;

        ModuleDescriptorParserRegistry.getInstance().addParser(org.codehaus.groovy.grails.resolve.maven.PomModuleDescriptorParser.getInstance());
        chainResolver = new ChainResolver();

        // Use the name cache because the root chain resolver is the one that is shown to have resolved the dependency
        // when it is resolved in the cache, which makes Ivy debug output easier to understand by making it clear what
        // came from the cache
        chainResolver.setName("cache");

        chainResolver.setReturnFirst(true);
        updateChangingPattern();
    }

    /**
     * Whether the legacy approach of parsing dependencies.groovy in addition to pom.xml should be used during dependency resolution
     *
     * @return True if it should
     */
    public boolean isLegacyResolve() {
        return legacyResolve;
    }

    public void setLegacyResolve(boolean legacyResolve) {
        this.legacyResolve = legacyResolve;
    }

    public ResolveEngine getResolveEngine() {
        return resolveEngine;
    }

    public void setResolveEngine(ResolveEngine resolveEngine) {
        this.resolveEngine = resolveEngine;
    }

    public MessageLogger getLogger() {
        return logger;
    }

    public void setLogger(MessageLogger logger) {
        Message.setDefaultLogger(logger);
        this.logger = logger;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
        updateChangingPattern();
    }

    private void updateChangingPattern() {
        chainResolver.setChangingPattern(isOffline() ? null : IvyDependencyManager.SNAPSHOT_CHANGING_PATTERN);
    }

    public ChainResolver getChainResolver() {
        return chainResolver;
    }

    public BuildSettings getBuildSettings() {
        return buildSettings;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setChainResolver(ChainResolver chainResolver) {
        resolveEngine.setDictatorResolver(chainResolver);
        this.chainResolver = chainResolver;
        updateChangingPattern();
    }

    public IvyDependencyManager createCopy(BuildSettings settings) {
        IvyDependencyManager copy = new IvyDependencyManager(applicationName, applicationVersion, settings);
        copy.setOffline(isOffline());
        copy.setChainResolver(getChainResolver());
        copy.setResolveEngine(getResolveEngine());
        if (getLogger() != null) {
            copy.logger = getLogger();
        }
        return copy;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setIncludeSource(boolean includeSource) {
        this.includeSource = includeSource;
    }

    public void setIncludeJavadoc(boolean includeJavadoc) {
        this.includeJavadoc = includeJavadoc;
    }

    public IvySettings getIvySettings() {
        return ivySettings;
    }

    public DefaultModuleDescriptor getModuleDescriptor() {
        return moduleDescriptor;
    }

    public void setModuleDescriptor(DefaultModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    /**
     * Returns true if the application has any dependencies that are not inherited
     * from the framework or other plugins
     */
    public boolean hasApplicationDependencies() {
        return hasApplicationDependencies;
    }

    public Collection<String> getUsedConfigurations() {
        return usedConfigurations;
    }

    public void setUsedConfigurations(Collection<String> usedConfigurations) {
        this.usedConfigurations = usedConfigurations;
    }

    /**
     * Obtains a set of dependency descriptors defined in the project
     */
    Set<DependencyDescriptor> getDependencyDescriptors() {
        return dependencyDescriptors;
    }

    public Set<String> getMetadataRegisteredPluginNames() {
        return metadataRegisteredPluginNames;
    }

    public void setMetadataRegisteredPluginNames(Set<String> metadataRegisteredPluginNames) {
        this.metadataRegisteredPluginNames = metadataRegisteredPluginNames;
    }

    /**
     * Obtains a set of plugin dependency descriptors defined in the project
     */
    public Set<DependencyDescriptor> getPluginDependencyDescriptors() {
        return pluginDependencyDescriptors;
    }

    /**
     * Returns all plugin dependency descriptors that are not transitively included
     *
     * @return Declared plugin descriptors
     */
    Set<DependencyDescriptor> getDeclaredPluginDependencyDescriptors() {
        Set<DependencyDescriptor> descriptors = getPluginDependencyDescriptors();
        Set<DependencyDescriptor> declaredDescriptors = new HashSet<DependencyDescriptor>();
        for (DependencyDescriptor descriptor : descriptors) {
            if (descriptor instanceof EnhancedDefaultDependencyDescriptor) {
                if (!((EnhancedDefaultDependencyDescriptor)descriptor).isTransitivelyIncluded()) {
                    declaredDescriptors.add(descriptor);
                }
            }
        }
        return declaredDescriptors;
    }

    /**
     * Obtains a particular DependencyDescriptor by the plugin name
     * @param pluginName The plugin name
     * @return A DependencyDescriptor or null
     */
    public DependencyDescriptor getPluginDependencyDescriptor(String pluginName) {
        return pluginNameToDescriptorMap.get(pluginName);
    }

    /**
     * Obtains a set of plugins this application is dependent onb
     * @return A set of plugins names
     */
    public Set<String> getPluginDependencyNames() { return pluginNameToDescriptorMap.keySet(); }

    /**
     * Obtains a list of dependencies defined in the project
     */
    public Set<ModuleRevisionId> getDependencies() { return dependencies; }

    /**
     * Tests whether the given ModuleId is defined in the list of dependencies
     */
    boolean hasDependency(ModuleId mid) {
        return modules.contains(mid) || mid.getName().equals("grails-dependencies");
    }

    /**
     * Tests whether the given group and name are defined in the list of dependencies
     */
    boolean hasDependency(String group, String name) {
        return hasDependency(ModuleId.newInstance(group, name));
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String[] getConfigurationNames() {
        return configurationNames;
    }

    public Map<String, List<String>> getConfigurationMappings() {
        return configurationMappings;
    }

    public boolean getReadPom() {
        return readPom;
    }

    public void setReadPom(boolean flag) {
        readPom = flag;
    }

    /**
     * Returns whether a plugin is transitive, ie whether its dependencies are resolved transitively
     *
     * @param pluginName The name of the plugin
     * @return true if the plugin is transitive
     */
    public boolean isPluginTransitive(String pluginName) {
        DependencyDescriptor dd = pluginNameToDescriptorMap.get(pluginName);
        return dd == null || dd.isTransitive();
    }

    /**
     * Whether the plugin is directly included or a transitive dependency of another plugin
     * @param pluginName The plugin name
     * @return true if is transitively included
     */
    public boolean isPluginTransitivelyIncluded(String pluginName) {
        EnhancedDefaultDependencyDescriptor dd = (EnhancedDefaultDependencyDescriptor) pluginNameToDescriptorMap.get(pluginName);
        return dd != null && dd.isTransitivelyIncluded() && dd.isExported();
    }

    /**
     * @deprecated use registerDependency(String, EnhancedDefaultDependencyDescriptor)
     */
    @Deprecated
    public void configureDependencyDescriptor(EnhancedDefaultDependencyDescriptor dependencyDescriptor, String scope) {
        configureDependencyDescriptor(dependencyDescriptor, scope, false);
    }

    /**
     * @deprecated use registerDependency(String, EnhancedDefaultDependencyDescriptor) or registerPluginDependency(String EnhancedDefaultDependencyDescriptor)
     */
    @Deprecated
    public void configureDependencyDescriptor(EnhancedDefaultDependencyDescriptor dependencyDescriptor, String scope, boolean pluginMode) {
        if (pluginMode) {
            registerPluginDependency(scope, dependencyDescriptor);
        } else {
            registerDependency(scope, dependencyDescriptor);
        }
    }

    /**
     * Registers a JAR dependency with the dependency manager.
     *
     * @see #registerPluginDependency(String, EnhancedDefaultDependencyDescriptor)
     */
    public void registerDependency(String scope, EnhancedDefaultDependencyDescriptor descriptor) {

        String plugin = descriptor.getPlugin();
        if (plugin != null) {
            EnhancedDefaultDependencyDescriptor pluginDependencyDescriptor = (EnhancedDefaultDependencyDescriptor) getPluginDependencyDescriptor(plugin);
            if (pluginDependencyDescriptor != null) {
                ExcludeRule[] excludeRules = pluginDependencyDescriptor.getExcludeRules(scope);
                if (excludeRules != null) {
                    for (ExcludeRule excludeRule : excludeRules) {
                        descriptor.addExcludeRule(scope, excludeRule);
                    }
                }

                String pluginScope = pluginDependencyDescriptor.getScope();
                if (pluginScope != null) {
                    if (isCompileOrRuntimeScope(scope)) {
                       scope = pluginScope;
                    }
                }
            }
        }

        registerDependencyCommon(scope, descriptor, false);
        ModuleRevisionId revisionId = descriptor.getDependencyRevisionId();
        modules.add(revisionId.getModuleId());
        dependencies.add(revisionId);
        String org = revisionId.getOrganisation();
        if (orgToDepMap.containsKey(org)) {
            orgToDepMap.get(org).add(revisionId);
        } else {
            Collection<ModuleRevisionId> deps = new HashSet<ModuleRevisionId>();
            deps.add(revisionId);
            orgToDepMap.put(org, deps);
        }

        dependencyDescriptors.add(descriptor);
        if (shouldIncludeDependency(descriptor)) {
            addToModuleDescriptor(scope, descriptor);
        }
    }

    private boolean areSameLogicalDependency(ModuleRevisionId lhs, ModuleRevisionId rhs) {
        return lhs.getModuleId().equals(rhs.getModuleId()) && lhs.getRevision().equals(rhs.getRevision());
    }

    private void addToModuleDescriptor(String scope, EnhancedDefaultDependencyDescriptor descriptor) {
        boolean foundDependency = false;
        for (DependencyDescriptor existingDescriptor : moduleDescriptor.getDependencies()) {
            if (existingDescriptor instanceof EnhancedDefaultDependencyDescriptor) {
                EnhancedDefaultDependencyDescriptor existingEnhancedDefaultDependencyDescriptor = (EnhancedDefaultDependencyDescriptor) existingDescriptor;
                if (!descriptor.getScope().equals(existingEnhancedDefaultDependencyDescriptor.getScope())) {
                    continue;
                }
            }
            if (areSameLogicalDependency(descriptor.getDependencyRevisionId(), existingDescriptor.getDependencyRevisionId())) {
                foundDependency = true;
                for (DependencyArtifactDescriptor artifactToAdd : descriptor.getAllDependencyArtifacts()) {
                    boolean foundArtifact = false;
                    for (DependencyArtifactDescriptor existingArtifact : existingDescriptor.getAllDependencyArtifacts()) {
                        if (existingArtifact.equals(artifactToAdd)) {
                            if (existingArtifact.getExtraAttributes().equals(artifactToAdd.getExtraAttributes())) {
                                foundArtifact = true;
                                break;
                            }
                        }
                    }
                    if (!foundArtifact) {
                        ((DefaultDependencyDescriptor)existingDescriptor).addDependencyArtifact(scope, artifactToAdd);
                    }
                }
                break;
            }
        }

        if (!foundDependency) {
            moduleDescriptor.addDependency(descriptor);
        }
    }

    private boolean isCompileOrRuntimeScope(String scope) {
        return scope.equals("runtime") || scope.equals("compile");
    }

    /**
     * Registers a plugin dependency (as in Grails plugin).
     *
     * @see #registerDependency(String, EnhancedDefaultDependencyDescriptor)
     */
    public void registerPluginDependency(String scope, EnhancedDefaultDependencyDescriptor descriptor) {
        ModuleId dependencyId = descriptor.getDependencyId();
        String name = dependencyId.getName();

        DependencyDescriptor existing = pluginNameToDescriptorMap.get(name);
        if (existing != null && descriptor.isTransitivelyIncluded()) {
            ModuleRevisionId dependencyRevisionId = existing.getDependencyRevisionId();
            if (dependencyRevisionId.equals(descriptor.getDependencyRevisionId())) return;
            if (descriptor.getPlugin() != null && (existing instanceof EnhancedDefaultDependencyDescriptor) && ((EnhancedDefaultDependencyDescriptor)existing).getPlugin() == null) {
                // if the descriptor is coming from a plugin and the dependency is already declared in an application then the one declared in the application takes priority
                return;
            }
        }

        registerDependencyCommon(scope, descriptor, true);

        pluginNameToDescriptorMap.put(name, descriptor);
        pluginDependencyDescriptors.add(descriptor);
        if (shouldIncludeDependency(descriptor)) {
            addToModuleDescriptor(scope, descriptor);
        }
    }

    private boolean shouldIncludeDependency(EnhancedDefaultDependencyDescriptor descriptor) {
        return descriptor.isExported()|| (buildSettings.isPluginProject() && isExposedByThisPlugin(descriptor));
    }

    private boolean isExposedByThisPlugin(EnhancedDefaultDependencyDescriptor descriptor) {
        File basePluginDescriptor = buildSettings.getBasePluginDescriptor();
        if (basePluginDescriptor != null) {
            String basePluginName = GrailsNameUtils.getPluginName(basePluginDescriptor.getName());
            String plugin = descriptor.getPlugin();
            return plugin == null || plugin.equals(basePluginName);
        }
        return false;

    }

    /**
     * Parses the Ivy DSL definition
     */
    public void parseDependencies(@SuppressWarnings("rawtypes") Closure definition) {
        if (definition == null || applicationName == null || applicationVersion == null) {
            return;
        }

        if (moduleDescriptor == null) {
            setModuleDescriptor((DefaultModuleDescriptor)createModuleDescriptor());
        }

        doParseDependencies(definition, null, null, NO_EXCLUDE_RULES);

        // The dependency config can use the pom(Boolean) method to declare
        // that this project has a POM and it has the dependencies, which means
        // we now have to inspect it for the dependencies to use.
        if (readPom == true && buildSettings != null) {
            registerPomDependencies();
        }

        if (metadata == null) {
            return;
        }

        // Legacy support for the old mechanism of plugin dependencies being
        // declared in the application.properties file.
        Map<String, String> metadataDeclaredPlugins = metadata.getInstalledPlugins();
        if (metadataDeclaredPlugins != null) {
            addMetadataPluginDependencies(metadataDeclaredPlugins);
        }
    }

    /**
     * Parses dependencies of a plugin.
     *
     * @param pluginName the name of the plugin
     * @param definition the Ivy DSL definition
     */
    public void parseDependencies(String pluginName, Closure<?> definition) throws IllegalStateException {
        if (definition == null) {
            return;
        }

        if (moduleDescriptor == null) {
            throw new IllegalStateException("Call parseDependencies(Closure) first to parse the application dependencies");
        }
        String scope = getParentScope(pluginName);
        doParseDependencies(definition, pluginName, scope, NO_EXCLUDE_RULES);
    }

    /**
     * Parses dependencies of a plugin.
     *
     * @param pluginName the name of the plugin
     * @param definition the Ivy DSL definition
     */
    public void parseDependencies(String pluginName, Closure<?> definition, ExcludeRule[] excludeRules) throws IllegalStateException {
        if (definition == null) {
            return;
        }

        if (moduleDescriptor == null) {
            throw new IllegalStateException("Call parseDependencies(Closure) first to parse the application dependencies");
        }

        String scope = getParentScope(pluginName);

        doParseDependencies(definition, pluginName, scope, excludeRules);
    }

    private String getParentScope(String pluginName) {
        DependencyDescriptor pluginDependencyDescriptor = getPluginDependencyDescriptor(pluginName);
        String scope = null;
        if (pluginDependencyDescriptor instanceof EnhancedDefaultDependencyDescriptor) {
            scope = ((EnhancedDefaultDependencyDescriptor)pluginDependencyDescriptor).getScope();
        }
        return scope;
    }

    /**
     * Evaluates the given DSL definition.
     *
     * If pluginName is not null, all dependencies will record that they were defined by this plugin.
     *
     * @see EnhancedDefaultDependencyDescriptor#plugin
     */
    private void doParseDependencies(Closure<?> definition, String pluginName, String scope, ExcludeRule[] excludeRules) {
        DependencyConfigurationContext context;

        // Temporary while we move all of the Groovy super class here
        IvyDependencyManager dependencyManager = (IvyDependencyManager)this;

        if (pluginName == null) {
            context = DependencyConfigurationContext.forApplication(dependencyManager);
        } else {
            context = DependencyConfigurationContext.forPlugin(dependencyManager, pluginName);
        }

        context.setOffline(offline);
        context.setParentScope(scope);
        context.setExcludeRules(excludeRules);

        definition.setDelegate(new DependencyConfigurationConfigurer(context));
        definition.setResolveStrategy(Closure.DELEGATE_FIRST);
        definition.call();
    }

    /**
     * Aspects of registering a dependency common to both plugins and jar dependencies.
     */
    private void registerDependencyCommon(String scope, EnhancedDefaultDependencyDescriptor descriptor, boolean isPluginDep) {
        if (offline && descriptor.isChanging()) {
            descriptor.setChanging(false);
        }

        registerUsedConfigurationIfNecessary(scope);

        if (descriptor.getModuleConfigurations().length == 0) {
            addDefaultModuleConfigurations(descriptor, scope,isPluginDep);
        }

        if (!descriptor.isInherited()) {
            hasApplicationDependencies = true;
        }
    }

    private void registerUsedConfigurationIfNecessary(String configurationName) {
        if (!usedConfigurations.contains(configurationName)) {
            usedConfigurations.add(configurationName);
        }
    }

    private void addDefaultModuleConfigurations(EnhancedDefaultDependencyDescriptor descriptor, String configurationName, boolean pluginDep) {
        List<String> mappings = configurationMappings.get(configurationName);
        if (mappings == null) {
            return;
        }

        String org = descriptor.getDependencyId().getOrganisation();
        if (!pluginDep && !"org.grails".equals(org) && !"org.springframework.uaa".equals(org)) {
            mappings = new ArrayList<String>(mappings);

            if (includeJavadoc) {
                mappings.add("javadoc");
            }
            if (includeSource) {
                mappings.add("sources");
            }
        }

        for (String m : mappings) {
            descriptor.addDependencyConfiguration(configurationName, m);
        }
    }

    protected ArtifactId createExcludeArtifactId(String excludeName) {
        return createExcludeArtifactId(excludeName, PatternMatcher.ANY_EXPRESSION);
    }

    protected ArtifactId createExcludeArtifactId(String excludeName, String group) {
        ModuleId mid = ModuleId.newInstance(group, excludeName);
        return new ArtifactId(
                mid, PatternMatcher.ANY_EXPRESSION,
                PatternMatcher.ANY_EXPRESSION,
                PatternMatcher.ANY_EXPRESSION);
    }

    public ModuleDescriptor createModuleDescriptor() {
        // This is a blatant hack: we use an organisation that is highly
        // unlikely to conflict with the project's dependencies. The
        // truth is, the dependency manager doesn't really care what the
        // organisation is. See:
        //
        //    http://jira.codehaus.org/browse/GRAILS-6270
        //
        DefaultModuleDescriptor moduleDescriptor =
            DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(
                    "org.grails.internal", applicationName, applicationVersion));

        // TODO: make configurations extensible
        moduleDescriptor.addConfiguration(AGENT_CONFIGURATION);
        moduleDescriptor.addConfiguration(BUILD_CONFIGURATION);
        moduleDescriptor.addConfiguration(COMPILE_CONFIGURATION);
        moduleDescriptor.addConfiguration(RUNTIME_CONFIGURATION);
        moduleDescriptor.addConfiguration(TEST_CONFIGURATION);
        moduleDescriptor.addConfiguration(PROVIDED_CONFIGURATION);
        moduleDescriptor.addConfiguration(DOCS_CONFIGURATION);
        return moduleDescriptor;
    }

    public boolean isExcludedFromPlugin(String plugin, String dependencyName) {
        DependencyDescriptor dd = pluginNameToDescriptorMap.get(plugin);
        if (dd == null) {
            return false;
        }

        if (!dd.isTransitive()) {
            return true;
        }

        ArtifactId aid = createExcludeArtifactId(dependencyName);
        return isExcludedFromPlugin(dd, aid);
    }

    public boolean isExcludedFromPlugin(DependencyDescriptor currentPlugin, ArtifactId dependency) {
        return currentPlugin != null && currentPlugin.doesExclude(configurationNames, dependency);
    }

    public Set<String> getPluginExcludes(String plugin) {
        Set<String> excludes = new HashSet<String>();
        DependencyDescriptor dd = pluginNameToDescriptorMap.get(plugin);
        if (dd != null) {
            for (ExcludeRule er : dd.getAllExcludeRules()) {
                excludes.add(er.getId().getName());
            }
        }
        return excludes;
    }

    public DependencyDescriptor[] readDependenciesFromPOM() {
        DependencyDescriptor[] fixedDependencies = null;
        File pom = new File(buildSettings.getBaseDir().getPath(), "pom.xml");
        if (pom.exists()) {
            PomModuleDescriptorParser parser = PomModuleDescriptorParser.getInstance();
            try {
                ModuleDescriptor md = parser.parseDescriptor(ivySettings, pom.toURI().toURL(), false);
                fixedDependencies = md.getDependencies();
            } catch (MalformedURLException e) {
                // Ignore (effectively returns null)
            } catch (ParseException e) {
                // Ignore (effectively returns null)
            } catch (IOException e) {
                // Ignore (effectively returns null)
            }
        }

        return fixedDependencies;
    }

    private void registerPomDependencies() {
        DependencyDescriptor[] pomDependencies = readDependenciesFromPOM();
        if (pomDependencies == null) {
            return;
        }

        for (DependencyDescriptor dependencyDescriptor : pomDependencies) {
            registerPomDependency(dependencyDescriptor);
        }
    }

    private void registerPomDependency(DependencyDescriptor dependencyDescriptor) {
        ModuleRevisionId moduleRevisionId = dependencyDescriptor.getDependencyRevisionId();
        moduleRevisionId = ModuleRevisionId.newInstance(moduleRevisionId.getOrganisation(), moduleRevisionId.getName(), moduleRevisionId.getRevision());
        ModuleId moduleId = moduleRevisionId.getModuleId();
        if (hasDependency(moduleId)) {
            return;
        }

        String scope = dependencyDescriptor.getModuleConfigurations()[0];
        EnhancedDefaultDependencyDescriptor enhancedDependencyDescriptor = new EnhancedDefaultDependencyDescriptor(
                moduleRevisionId, false, true, scope);
        for (ExcludeRule excludeRule : dependencyDescriptor.getAllExcludeRules()) {
            ModuleId excludedModule = excludeRule.getId().getModuleId();
            enhancedDependencyDescriptor.addRuleForModuleId(excludedModule, scope);
        }

        DependencyArtifactDescriptor[] allDependencyArtifacts = dependencyDescriptor.getAllDependencyArtifacts();
        boolean isPlugin = false;
        for (DependencyArtifactDescriptor dependencyArtifact : allDependencyArtifacts) {
            if (dependencyArtifact.getType() != null && dependencyArtifact.getType().equals("zip")) {
                isPlugin = true; break;
            }
        }
        if (isPlugin) {
            registerPluginDependency(scope, enhancedDependencyDescriptor);
        }
        else {
            registerDependency(scope, enhancedDependencyDescriptor);
        }
    }

    private void addMetadataPluginDependencies(Map<String, String> plugins) {
        for (Map.Entry<String, String> plugin : plugins.entrySet()) {
            String name = plugin.getKey().contains(":") ? plugin.getKey().split(":")[1] : plugin.getKey();
            String group = plugin.getKey().contains(":") ? plugin.getKey().split(":")[0] : "org.grails.plugins";
            String version = plugin.getValue();

            if (pluginNameToDescriptorMap.containsKey(name)) {
                continue;
            }

            String scope = "runtime";
            metadataRegisteredPluginNames.add(name);
            ModuleRevisionId mrid = ModuleRevisionId.newInstance(group, name, version);
            EnhancedDefaultDependencyDescriptor enhancedDescriptor = new EnhancedDefaultDependencyDescriptor(
                    mrid, true, true, scope);
            // since the plugin dependency isn't declared but instead installed via install-plugin
            // it should be not be exported by another plugin
            if (buildSettings.isPluginProject()) {
                enhancedDescriptor.setExport(false);
            }

            registerPluginDependency(scope, enhancedDescriptor);
        }
    }
}
