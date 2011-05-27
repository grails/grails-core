/* Copyright 2004-2005 the original author or authors.
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
import grails.util.Metadata;
import groovy.lang.Closure;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParser;
import org.codehaus.groovy.grails.resolve.config.DependencyConfigurationConfigurer;
import org.codehaus.groovy.grails.resolve.config.DependencyConfigurationContext;

/**
 * Base class for IvyDependencyManager with some logic implemented in Java.
 *
 * @author Graeme Rocher
 * @since 1.3
 */
@SuppressWarnings("serial")
public abstract class AbstractIvyDependencyManager {

    /*
     * Out of the box Ivy configurations are:
     *
     * - build: Dependencies for the build system only
     * - compile: Dependencies for the compile step
     * - runtime: Dependencies needed at runtime but not for compilation (see above)
     * - test: Dependencies needed for testing but not at runtime (see above)
     * - provided: Dependencies needed at development time, but not during WAR deployment
     */
    public static Configuration BUILD_CONFIGURATION  = new Configuration("build",
                                                                Configuration.Visibility.PUBLIC,
                                                                "Build system dependencies",
                                                                new String[]{"default"},
                                                                true, null);

    public static Configuration COMPILE_CONFIGURATION = new Configuration("compile",
                                                                Configuration.Visibility.PUBLIC,
                                                                "Compile time dependencies",
                                                                new String[]{"default" },
                                                                true, null);

    public static Configuration RUNTIME_CONFIGURATION = new Configuration("runtime",
                                                                Configuration.Visibility.PUBLIC,
                                                                "Runtime time dependencies",
                                                                new String[]{"compile"},
                                                                true, null);

    public static Configuration TEST_CONFIGURATION = new Configuration("test",
                                                                Configuration.Visibility.PUBLIC,
                                                                "Testing dependencies",
                                                                new String[]{"runtime"},
                                                                true, null);

    public static Configuration PROVIDED_CONFIGURATION = new Configuration("provided",
                                                                Configuration.Visibility.PUBLIC,
                                                                "Dependencies provided by the container",
                                                                new String[]{"default"},
                                                                true, null);

    public static Configuration DOCS_CONFIGURATION = new Configuration("docs",
            Configuration.Visibility.PUBLIC,
            "Dependencies for the documenation engine",
            new String[]{"build"},
            true, null);

    public static List<Configuration> ALL_CONFIGURATIONS = Arrays.asList(
            BUILD_CONFIGURATION,
            COMPILE_CONFIGURATION,
            RUNTIME_CONFIGURATION,
            TEST_CONFIGURATION,
            PROVIDED_CONFIGURATION,
            DOCS_CONFIGURATION);

    Map<String, List<String>> configurationMappings = new HashMap<String, List<String>>() {{
       put("runtime", Arrays.asList("default"));
       put("build", Arrays.asList("default"));
       put("compile", Arrays.asList("default"));
       put("provided", Arrays.asList("default"));
       put("docs", Arrays.asList("default"));
       put("test", Arrays.asList("default"));
    }};

    protected String[] configurationNames = configurationMappings.keySet().toArray(
            new String[configurationMappings.size()]);
    protected Set<ModuleId> modules = new HashSet<ModuleId>();
    protected Set<ModuleRevisionId> dependencies = new HashSet<ModuleRevisionId>();
    protected Set<DependencyDescriptor> dependencyDescriptors = new HashSet<DependencyDescriptor>();
    protected Set<DependencyDescriptor> pluginDependencyDescriptors = new HashSet<DependencyDescriptor>();
    protected Set<String> pluginDependencyNames = new HashSet<String>();
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

    final protected IvySettings ivySettings;
    final protected BuildSettings buildSettings;
    final protected Metadata metadata;

    public AbstractIvyDependencyManager(IvySettings ivySettings, BuildSettings buildSettings, Metadata metadata) {
        this.ivySettings = ivySettings;
        this.buildSettings = buildSettings;
        this.metadata = metadata;
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
    Set<DependencyDescriptor> getPluginDependencyDescriptors() {
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
            if(descriptor instanceof EnhancedDefaultDependencyDescriptor) {
                if(!((EnhancedDefaultDependencyDescriptor)descriptor).isTransitivelyIncluded()) {
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
    public Set<String> getPluginDependencyNames() { return pluginDependencyNames; }

    /**
     * Obtains a list of dependencies defined in the project
     */
    public Set<ModuleRevisionId> getDependencies() { return dependencies; }

    /**
     * Tests whether the given ModuleId is defined in the list of dependencies
     */
    boolean hasDependency(ModuleId mid) {
        return modules.contains(mid);
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
     * @return True if the plugin is transitive
     */
    public boolean isPluginTransitive(String pluginName) {
        DependencyDescriptor dd = pluginNameToDescriptorMap.get(pluginName);
        return dd == null || dd.isTransitive();
    }

    /**
     * Whether the plugin is directly included or a transitive dependency of another plugin
     * @param pluginName The plugin name
     * @return True if is transitively included
     */
    public boolean isPluginTransitivelyIncluded(String pluginName) {
        EnhancedDefaultDependencyDescriptor dd = (EnhancedDefaultDependencyDescriptor) pluginNameToDescriptorMap.get(pluginName);
        return dd != null && dd.isTransitivelyIncluded();
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
        registerDependencyCommon(scope, descriptor);

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
        if (descriptor.isExportedToApplication()) {
            moduleDescriptor.addDependency(descriptor);
        }
    }

    /**
     * Registers a plugin dependency (as in Grails plugin).
     *
     * @see #registerDependency(String, EnhancedDefaultDependencyDescriptor)
     */
    public void registerPluginDependency(String scope, EnhancedDefaultDependencyDescriptor descriptor) {
        String name = descriptor.getDependencyId().getName();

        String classifierAttribute = descriptor.getExtraAttribute("m:classifier");
        String packaging;
        if (classifierAttribute != null && classifierAttribute.equals("plugin")) {
            packaging = "xml";
        } else {
            packaging = "zip";
        }

        DependencyArtifactDescriptor artifact = new DefaultDependencyArtifactDescriptor(descriptor, name, packaging, packaging, null, null);
        descriptor.addDependencyArtifact(scope, artifact);

        registerDependencyCommon(scope, descriptor);

        pluginDependencyNames.add(name);
        pluginDependencyDescriptors.add(descriptor);
        pluginNameToDescriptorMap.put(name, descriptor);
    }

    /**
     * Parses the Ivy DSL definition
     */
    public void parseDependencies(@SuppressWarnings("rawtypes") Closure definition) {
        if (definition != null && applicationName != null && applicationVersion != null) {
            if (moduleDescriptor == null) {
                setModuleDescriptor((DefaultModuleDescriptor)createModuleDescriptor());
            }

            doParseDependencies(definition, null);

            // The dependency config can use the pom(Boolean) method to declare
            // that this project has a POM and it has the dependencies, which means
            // we now have to inspect it for the dependencies to use.
            if (readPom == true && buildSettings != null) {
                registerPomDependencies();
            }

            // Legacy support for the old mechanism of plugin dependencies being
            // declared in the application.properties file.
            if (metadata != null) {
                Map<String, String> metadataDeclaredPlugins = metadata.getInstalledPlugins();
                if (metadataDeclaredPlugins != null) {
                    addMetadataPluginDependencies(metadataDeclaredPlugins);
                }
            }
        }
    }

    /**
     * Parses dependencies of a plugin.
     *
     * @param pluginName the name of the plugin
     * @param definition the Ivy DSL definition
     */
    public void parseDependencies(String pluginName, Closure<?> definition) throws IllegalStateException {
        if (definition != null) {
            if (moduleDescriptor == null) {
                throw new IllegalStateException("Call parseDependencies(Closure) first to parse the application dependencies");
            }

            doParseDependencies(definition, pluginName);
        }
    }

    /**
     * Evaluates the given DSL definition.
     *
     * If pluginName is not null, all dependencies will record that they were defined by this plugin.
     *
     * @see EnhancedDefaultDependencyDescriptor#plugin
     */
    private void doParseDependencies(Closure<?> definition, String pluginName) {
        DependencyConfigurationContext context;

        // Temporary while we move all of the Groovy super class here
        IvyDependencyManager dependencyManager = (IvyDependencyManager)this;

        if (pluginName != null) {
            context = DependencyConfigurationContext.forPlugin(dependencyManager, pluginName);
        } else {
            context = DependencyConfigurationContext.forApplication(dependencyManager);
        }

        definition.setDelegate(new DependencyConfigurationConfigurer(context));
        definition.setResolveStrategy(Closure.DELEGATE_FIRST);
        definition.call();
    }

    /**
     * Aspects of registering a dependency common to both plugins and jar dependencies.
     */
    private void registerDependencyCommon(String scope, EnhancedDefaultDependencyDescriptor descriptor) {
        registerUsedConfigurationIfNecessary(scope);

        if (descriptor.getModuleConfigurations().length == 0) {
            addDefaultModuleConfigurations(descriptor, scope);
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

    private void addDefaultModuleConfigurations(EnhancedDefaultDependencyDescriptor descriptor, String configurationName) {
        List<String> mappings = configurationMappings.get(configurationName);
        if (mappings != null) {
            for (String m : mappings) {
                descriptor.addDependencyConfiguration(configurationName, m);
            }
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
        @SuppressWarnings("hiding")
        DefaultModuleDescriptor moduleDescriptor =
            DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance("org.grails.internal", applicationName, applicationVersion));

        // TODO: make configurations extensible
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
        if (dd != null)  {
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
        if (pomDependencies != null) {
            for (DependencyDescriptor dependencyDescriptor : pomDependencies) {
                registerPomDependency(dependencyDescriptor);
            }
        }
    }

    private void registerPomDependency(DependencyDescriptor dependencyDescriptor) {
        ModuleRevisionId moduleRevisionId = dependencyDescriptor.getDependencyRevisionId();
        ModuleId moduleId = moduleRevisionId.getModuleId();

        String scope = Arrays.asList(dependencyDescriptor.getModuleConfigurations()).get(0);

        if (!hasDependency(moduleId)) {
            EnhancedDefaultDependencyDescriptor enhancedDependencyDescriptor = new EnhancedDefaultDependencyDescriptor(moduleRevisionId, false, true, scope);
            for (ExcludeRule excludeRule : dependencyDescriptor.getAllExcludeRules()) {
                ModuleId excludedModule = excludeRule.getId().getModuleId();
                enhancedDependencyDescriptor.addRuleForModuleId(excludedModule, scope);
            }

            registerDependency(scope, enhancedDependencyDescriptor);
        }
    }

    private void addMetadataPluginDependencies(Map<String, String> plugins) {
        for (Map.Entry<String, String> plugin : plugins.entrySet()) {
            String name = plugin.getKey();
            String version = plugin.getValue();

            if (!pluginDependencyNames.contains(name)) {
                String scope = "runtime";
                metadataRegisteredPluginNames.add(name);
                ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.grails.plugins", name, version);
                EnhancedDefaultDependencyDescriptor enhancedDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, true, true, scope);
                // since the plugin dependency isn't declared but instead installed via install-plugin it should be not be exported by another plugin
                enhancedDescriptor.setExport(false);

                registerPluginDependency(scope, enhancedDescriptor);
            }
        }
    }
}
