/*
 * Copyright 2014 the original author or authors.
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

package grails.test.runtime

import grails.test.mixin.TestRuntimeAwareMixin
import grails.test.mixin.support.MixinInstance
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import java.lang.reflect.Field
import java.lang.reflect.Modifier

import org.springframework.util.ReflectionUtils

/**
 * TestRuntimeFactory is a singleton that contains the TestPlugin registry and has methods 
 * for getting a TestRuntime instance with given features.
 * 
 * It will resolve the requested features and pick the minimum set of plugins 
 * that are required for covering the requested features.
 * 
 * @author Lari Hotari
 * @since 2.4.0
 */
@CompileStatic
class TestRuntimeFactory {
    static TestRuntimeFactory INSTANCE=new TestRuntimeFactory()
    Set<Class<? extends TestPlugin>> availablePluginClasses=[
        GrailsApplicationTestPlugin,
        CoreBeansTestPlugin,
        MetaClassCleanerTestPlugin,
        ControllerTestPlugin,
        FiltersTestPlugin,
        GroovyPageTestPlugin,
        WebFlowTestPlugin,
        DomainClassTestPlugin
        ] as Set

    private TestRuntimeFactory() {
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    static TestRuntime getRuntimeForTestClass(Class<?> testClass) {
        Set<TestRuntimeAwareMixin> allInstances = [] as Set
        Set<String> allFeatures = [] as Set
        Class<?> currentClass = testClass
        while(currentClass != Object && currentClass != null) {
            currentClass.getDeclaredFields().each { Field field ->
                if(field.getAnnotation(MixinInstance.class) != null && Modifier.isStatic(field.getModifiers())) {
                    ReflectionUtils.makeAccessible(field)
                    def instance = field.get(null)
                    if(instance instanceof TestRuntimeAwareMixin) {
                        allInstances.add(instance)
                        allFeatures.addAll(instance.features)
                    }
                }
            }
            currentClass = currentClass.getSuperclass()
        }
        SharedRuntime sharedRuntimeAnnotation = findSharedRuntimeAnnotation(testClass)
        TestRuntime runtime
        if(sharedRuntimeAnnotation==null) {
            runtime = INSTANCE.findOrCreateRuntimeForTestClass(testClass, allFeatures)
        } else {
            runtime = INSTANCE.findOrCreateSharedRuntime(sharedRuntimeAnnotation.value(), allFeatures)
        }
        allInstances.each { testMixinInstance ->
            testMixinInstance.runtime = runtime
        }
        runtime
    }

    private static SharedRuntime findSharedRuntimeAnnotation(Class testClass) {
        SharedRuntime sharedRuntimeAnnotation = testClass.getAnnotation(SharedRuntime)
        Class<?> currentClass = testClass
        while(currentClass != Object && currentClass != null && sharedRuntimeAnnotation == null) {
            sharedRuntimeAnnotation = currentClass.getPackage()?.getAnnotation(SharedRuntime)
            currentClass = currentClass.getSuperclass()
        }
        return sharedRuntimeAnnotation
    }
    
    static void addPluginClass(Class<? extends TestPlugin> pluginClass) {
        INSTANCE.addTestPluginClass(pluginClass)
    }

    static void removePlugin(Class<? extends TestPlugin> pluginClass) {
        INSTANCE.removeTestPluginClass(pluginClass)
    }
    
    private TestRuntime findOrCreateSharedRuntime(Class<? extends SharedRuntimeConfigurer> sharedRuntimeConfigurer, Set<String> features) {
        TestRuntime runtime=sharedRuntimes.get(sharedRuntimeConfigurer)
        if(runtime==null) {
            SharedRuntimeConfigurer configurerInstance=(SharedRuntimeConfigurer)sharedRuntimeConfigurer.newInstance()
            runtime = createRuntimeForFeatures(configurerInstance.getRequiredFeatures() as Set, true)
            sharedRuntimes.put(sharedRuntimeConfigurer, runtime)
        }
        checkRuntimeFeatures(runtime, features)
        runtime
    }

    private TestRuntime findOrCreateRuntimeForTestClass(Class testClass, Set<String> features) {
        TestRuntime runtime=activeRuntimes.get(testClass)
        if(runtime==null) {
            runtime = createRuntimeForFeatures(features, null, false)
            activeRuntimes.put(testClass, runtime)
        } else {
            checkRuntimeFeatures(runtime, features)
        }
        runtime
    }
    
    private void checkRuntimeFeatures(TestRuntime runtime, Set<String> features) {
        if(features && !runtime.features.containsAll(features)) {
            Set<String> missingFeatures = [] as Set
            missingFeatures.addAll(features)
            missingFeatures.removeAll(runtime.features)
            throw new TestRuntimeFactoryException("Current shared runtime doesn't contain required feature:" + missingFeatures.join(',')) 
        }
    }
        
    private TestRuntime createRuntimeForFeatures(Set features, boolean shared) {
        TestRuntime runtime
        if(features) {
            runtime = new TestRuntime(features, resolveFeaturesToPlugins(features), shared)
        } else {
            // setup runtime with all available plugins
            Map<String, TestPlugin> featureToPlugin = resolvePlugins()
            Set<String> allFeatures = new LinkedHashSet(featureToPlugin.keySet())
            List<TestPlugin> allPlugins = resolveTransitiveDependencies(allFeatures, featureToPlugin)
            runtime = new TestRuntime(allFeatures, allPlugins, shared)
        }
        return runtime
    }

    private List resolveFeaturesToPlugins(Set features) {
        Map<String, TestPlugin> featureToPlugin = resolvePlugins()
        List<TestPlugin> requiredPlugins = resolveTransitiveDependencies(features, featureToPlugin)
        return requiredPlugins
    }

    private List<TestPlugin> resolveTransitiveDependencies(Set<String> features, Map<String, TestPlugin> featureToPlugin) {
        // resolve required plugins
        List<TestPlugin> requiredPlugins = features.collect { String feature ->
            resolveFeature(feature, featureToPlugin)
        }

        // resolve transitive plugins
        List<TestPlugin> allPlugins = sortByOrdinal((List<TestPlugin>)requiredPlugins.inject([]) { List<TestPlugin> collector, TestPlugin plugin ->
            collector.add(plugin)
            collector.addAll(resolveRequiredPlugins(plugin, featureToPlugin))
            collector
        })

        // sort by dependency order (topological sort)
        sortPlugins(allPlugins, featureToPlugin)
    }

    private List<TestPlugin> resolveRequiredPlugins(TestPlugin plugin, Map<String, TestPlugin> featureToPlugin) {
        sortByOrdinal((List<TestPlugin>)plugin.requiredFeatures.collect{ String feature -> resolveFeature(feature, featureToPlugin) })
    }

    private TestPlugin resolveFeature(String feature, Map<String, TestPlugin> featureToPlugin) {
        TestPlugin plugin = featureToPlugin.get(feature)
        if(plugin == null) {
            throw new TestRuntimeFactoryException("No plugin available for feature ${feature}")
        }
        plugin
    }
    
    private List<TestPlugin> sortPlugins(List<TestPlugin> toSort, Map<String, TestPlugin> featureToPlugin) {
        /* http://en.wikipedia.org/wiki/Topological_sorting
         *
        * L ← Empty list that will contain the sorted nodes
         S ← Set of all nodes

        function visit(node n)
            if n has not been visited yet then
                mark n as visited
                for each node m with an edge from n to m do
                    visit(m)
                add n to L

        for each node n in S do
            visit(n)

         */
        List<TestPlugin> sortedPlugins = new ArrayList<TestPlugin>(toSort.size())
        Set<TestPlugin> visitedPlugins = new HashSet<TestPlugin>()

        for (TestPlugin plugin : toSort) {
            visitTopologicalSort(plugin, sortedPlugins, visitedPlugins, featureToPlugin)
        }

        return sortedPlugins
    }

    private void visitTopologicalSort(TestPlugin plugin, List<TestPlugin> sortedPlugins, Set<TestPlugin> visitedPlugins, Map<String, TestPlugin> featureToPlugin) {
        if(plugin != null && !visitedPlugins.contains(plugin)) {
            visitedPlugins.add(plugin)
            List<TestPlugin> dependenciesForPlugin = resolveRequiredPlugins(plugin, featureToPlugin)
            if(dependenciesForPlugin != null) {
                for(TestPlugin dependentPlugin : dependenciesForPlugin) {
                    visitTopologicalSort(dependentPlugin, sortedPlugins, visitedPlugins, featureToPlugin)
                }
            }
            sortedPlugins.add(plugin)
        }
    }

    // maps feature to plugin with lowest ordinal (highest priority)
    private Map<String, TestPlugin> resolvePlugins() {
        Map<String, List<TestPlugin>> featureToPlugins = [:]
        List<TestPlugin> availablePlugins = availablePluginClasses.collect { Class<? extends TestPlugin> clazz -> clazz.newInstance() }
        for(TestPlugin plugin : availablePlugins) {
            for(String feature : plugin.getProvidedFeatures()) {
                def pluginList = featureToPlugins.get(feature)
                if(pluginList==null) {
                    pluginList = []
                    featureToPlugins.put(feature, pluginList)
                }
                pluginList.add(plugin)
            }
        }
        // pick one plugin for feature implementation
        (Map<String, TestPlugin>)featureToPlugins.collectEntries { String feature, List<TestPlugin> pluginList ->
            // sort plugins with lowest ordinal (highest priority) and pick the first one
            if(pluginList.size() > 1) {
                pluginList = sortByOrdinal(pluginList)
            }
            [feature, pluginList[0]]
        }
    }

    private List<TestPlugin> sortByOrdinal(List<TestPlugin> pluginList) {
        // remove duplicates
        def pluginSet = [] as Set
        pluginSet.addAll(pluginList)
        def newPluginList = new ArrayList(pluginSet)
        // sort by ordinal, reverse order (highest value comes first)
        newPluginList.sort(false) { TestPlugin a, TestPlugin b ->
            b.ordinal <=> a.ordinal
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void addTestPluginClass(Class<? extends TestPlugin> pluginClass) {
        availablePluginClasses.add(pluginClass)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void removeTestPluginClass(Class<? extends TestPlugin> pluginClass) {
        availablePluginClasses.remove(pluginClass)
    }
    
    // cached runtimes for sharing runtimes across test classes
    private Map<Class, TestRuntime> sharedRuntimes=new HashMap<Class, TestRuntime>()
    
    private Map<Class, TestRuntime> activeRuntimes=new HashMap<Class, TestRuntime>()
    
    static void removeRuntime(TestRuntime runtime) {
        INSTANCE.removeTestRuntime(runtime)
    }

    private void removeTestRuntime(TestRuntime runtime) {
        removeRuntimeFromMap(sharedRuntimes, runtime)
        removeRuntimeFromMap(activeRuntimes, runtime)
    }

    private removeRuntimeFromMap(Map<Class, TestRuntime> cachedRuntimeMap, TestRuntime runtime) {
        for(Iterator iterator=cachedRuntimeMap.entrySet().iterator();iterator.hasNext();) {
            Map.Entry entry=iterator.next()
            if(entry.value==runtime) {
                iterator.remove()
                return
            }
        }
    }
}

@CompileStatic
class TestRuntimeFactoryException extends RuntimeException {
    public TestRuntimeFactoryException(String message) {
        super(message)
    }
}