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

import org.apache.ivy.core.module.descriptor.*;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.PatternMatcher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for IvyDependencyManager with some logic implemented
 * in Java
 *
 * @author Graeme Rocher
 * @since 1.3
 */
@SuppressWarnings("serial")
abstract public class AbstractIvyDependencyManager {

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
    

    public static List<Configuration> ALL_CONFIGURATIONS = new ArrayList<Configuration>() {{
        add(BUILD_CONFIGURATION);
        add(COMPILE_CONFIGURATION);
        add(RUNTIME_CONFIGURATION);
        add(TEST_CONFIGURATION);
        add(PROVIDED_CONFIGURATION);
        add(DOCS_CONFIGURATION);
    }};

    Map<String, List> configurationMappings = new HashMap<String, List>() {{
        put("runtime", new ArrayList<String>() {{
            add("runtime(*)"); add("master(*)");
        }});
        put("build", new ArrayList<String>() {{
            add("default");
        }});
        put("compile", new ArrayList<String>() {{
            add("'compile(*)"); add("master(*)");
        }});
        put("provided", new ArrayList<String>() {{
            add("'compile(*)"); add("master(*)");
        }});
        put("docs", new ArrayList<String>() {{
            add("'compile(*)"); add("master(*)");
        }});        
        put("test", new ArrayList<String>() {{
            add("''runtime(*)'(*)"); add("master(*)");
        }});
    }};
    protected String[] configurationNames = configurationMappings.keySet().toArray(new String[configurationMappings.size()]);
    protected Set<ModuleId> modules = new HashSet<ModuleId>();
    protected Set<ModuleRevisionId> dependencies = new HashSet<ModuleRevisionId>();
    protected Set<DependencyDescriptor> dependencyDescriptors = new HashSet<DependencyDescriptor>();
    protected Set<DependencyDescriptor> pluginDependencyDescriptors = new HashSet<DependencyDescriptor>();
    protected Set<String> pluginDependencyNames = new HashSet<String>();
    protected Set<String> metadataRegisteredPluginNames = new HashSet<String>();
    protected Map<String, Collection<ModuleRevisionId>> orgToDepMap = new HashMap<String, Collection<ModuleRevisionId>>();


    protected Map<String, DependencyDescriptor> pluginNameToDescriptorMap = new ConcurrentHashMap<String, DependencyDescriptor>();
    protected String applicationName;
    protected String applicationVersion;

    /**
    * Obtains a set of dependency descriptors defined in the project
     */
    Set<DependencyDescriptor> getDependencyDescriptors() {
        return dependencyDescriptors;
    };

    
    public Set<String> getMetadataRegisteredPluginNames() {
		return metadataRegisteredPluginNames;
	}


	public void setMetadataRegisteredPluginNames(
			Set<String> metadataRegisteredPluginNames) {
		this.metadataRegisteredPluginNames = metadataRegisteredPluginNames;
	}


	/**
    * Obtains a set of plugin dependency descriptors defined in the project
     */
    Set<DependencyDescriptor> getPluginDependencyDescriptors() {
        return pluginDependencyDescriptors;
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
    public Set<ModuleRevisionId> getDependencies() { return this.dependencies; }


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

    public Map<String, List> getConfigurationMappings() {
        return configurationMappings;
    }

    /**
     * Returns whether a plugin is transitive
     *
     * @param pluginName The name of the plugin
     * @return True if the plugin is transitive
     */
    public boolean isPluginTransitive(String pluginName) {
        DependencyDescriptor dd = pluginNameToDescriptorMap.get(pluginName);
        return dd == null || dd.isTransitive();
    }

    /**
     * Adds a dependency to the project
     *
     * @param revisionId The ModuleRevisionId instance
     */
    public void addDependency(ModuleRevisionId revisionId) {
        modules.add(revisionId.getModuleId());
        dependencies.add(revisionId);
        final String org = revisionId.getOrganisation();
        if(orgToDepMap.containsKey(org)) {
            orgToDepMap.get(org).add(revisionId);
        }
        else {
            Collection<ModuleRevisionId> deps = new HashSet<ModuleRevisionId>();
            deps.add(revisionId);
            orgToDepMap.put(org, deps);
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

    /**
     * Adds a dependency descriptor to the project
     * @param dd The DependencyDescriptor instance
     */
    public void addDependencyDescriptor(DependencyDescriptor dd) {
        if(dd != null) {
            dependencyDescriptors.add(dd);
            addDependency(dd.getDependencyRevisionId());
        }
    }

    public ModuleDescriptor createModuleDescriptor() {
        DefaultModuleDescriptor moduleDescriptor =
            DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(applicationName, applicationName, applicationVersion));

        // TODO: make configurations extensible
        moduleDescriptor.addConfiguration( BUILD_CONFIGURATION );
        moduleDescriptor.addConfiguration( COMPILE_CONFIGURATION );
        moduleDescriptor.addConfiguration( RUNTIME_CONFIGURATION );
        moduleDescriptor.addConfiguration( TEST_CONFIGURATION );
        moduleDescriptor.addConfiguration( PROVIDED_CONFIGURATION );
        moduleDescriptor.addConfiguration( DOCS_CONFIGURATION );
        return moduleDescriptor;
    }

    public boolean isExcludedFromPlugin(String plugin, String dependencyName) {
        DependencyDescriptor dd = pluginNameToDescriptorMap.get(plugin);
        if(dd == null) return false;
        if(!dd.isTransitive()) return true;
        else {
            ArtifactId aid = createExcludeArtifactId(dependencyName);
            return isExcludedFromPlugin(dd, aid);
        }
    }

    public boolean isExcludedFromPlugin(DependencyDescriptor currentPlugin, ArtifactId dependency) {
        return currentPlugin != null && currentPlugin.doesExclude(configurationNames, dependency);
    }

    public Set<String> getPluginExcludes(String plugin) {
        Set<String> excludes = new HashSet<String>();
        DependencyDescriptor dd = pluginNameToDescriptorMap.get(plugin);
        if(dd != null)  {
            for(ExcludeRule er : dd.getAllExcludeRules()) {
              excludes.add(er.getId().getName());
            }
        }
        return excludes;
    }

}
