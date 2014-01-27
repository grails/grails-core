package grails.test.runtime

import grails.test.mixin.TestRuntimeAwareMixin
import grails.test.mixin.support.MixinInstance;
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import java.lang.reflect.Field
import java.lang.reflect.Modifier;

import org.springframework.util.ReflectionUtils;

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
        Class<?> currentClass = testClass
        Set<TestRuntimeAwareMixin> allInstances = [] as Set
        Set<String> allFeatures = [] as Set
        while(currentClass != Object) {
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
            currentClass = currentClass.superclass
        }
        TestRuntime runtime = getRuntime(allFeatures)
        allInstances.each { testMixinInstance ->
            testMixinInstance.runtime = runtime
        }
        runtime
    }
    
    static TestRuntime getRuntime(String... features) {
        getRuntime(features as Set)
    }

    static TestRuntime getRuntime(Set<String> features) {
        ExpandoMetaClass.enableGlobally()
        INSTANCE.getTestRuntimeForFeatures(features)
    }

    static void addPluginClass(Class<? extends TestPlugin> pluginClass) {
        INSTANCE.addTestPluginClass(pluginClass)
    }

    static void removePlugin(Class<? extends TestPlugin> pluginClass) {
        INSTANCE.removeTestPluginClass(pluginClass)
    }

    private TestRuntime getTestRuntimeForFeatures(Set<String> features) {
        if(activeRuntime) {
            if(!activeRuntime.features.containsAll(features)) {
                Set<String> combinedFeatures = [] as Set
                combinedFeatures.addAll(activeRuntime.features)
                combinedFeatures.addAll(features)
                activeRuntime.changeFeaturesAndPlugins(combinedFeatures, resolveFeaturesToPlugins(combinedFeatures))
            }
            return activeRuntime
        }
        TestRuntime runtime = new TestRuntime(features, resolveFeaturesToPlugins(features))
        activeRuntime = runtime
        runtime
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
        // sort by ordinal
        newPluginList.sort(false) { TestPlugin a, TestPlugin b ->
            a.ordinal <=> b.ordinal
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
    private TestRuntime activeRuntime=null

    static void removeRuntime(TestRuntime runtime) {
        INSTANCE.removeTestRuntime(runtime)
    }

    private void removeTestRuntime(TestRuntime runtime) {
        if(activeRuntime==runtime) {
            activeRuntime=null
        }
    }
}

@CompileStatic
class TestRuntimeFactoryException extends RuntimeException {
    public TestRuntimeFactoryException(String message) {
        super(message)
    }
}