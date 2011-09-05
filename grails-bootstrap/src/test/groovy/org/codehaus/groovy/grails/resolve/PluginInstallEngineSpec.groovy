package org.codehaus.groovy.grails.resolve

import grails.util.*
import static groovy.lang.GroovySystem.*
import spock.lang.*
import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.module.id.ModuleRevisionId

class PluginInstallEngineSpec extends Specification {

    def "update existing metadata"() {
        given:
        def metadata = new MetadataStorage('plugins.test-plugin': '0.1')
        def engine = systemUnderTest(metadata)
        assert metadata['plugins.test-plugin'] == '0.1'

        when:
        engine.registerPluginWithMetadata('test-plugin', '2.3')

        then:
        metadata['plugins.test-plugin'] == '2.3'
    }

    def "persist to metadata when plugin defined in neither metadata nor BuildConfig"() {
        given:
        def metadata = new MetadataStorage()
        def engine = systemUnderTest(metadata)
        assert !metadata['plugins.test-plugin']

        when:
        engine.registerPluginWithMetadata('test-plugin', '0.1')

        then:
        metadata['plugins.test-plugin'] == '0.1'
    }

    def "do not persist to metadata when a transitive dependency exports the plugin"() {
        given:
        def metadata = new MetadataStorage()
        def engine = systemUnderTest(metadata, transitiveExported('test-plugin'))

        when:
        engine.registerPluginWithMetadata('test-plugin', '0.1')

        then:
        !metadata['plugins.test-plugin']
    }

    def "persist to metadata when transitive dependency does not export the plugin"() {
        given:
        def metadata = new MetadataStorage()
        def engine = systemUnderTest(metadata, transitiveNotExported('test-plugin'))

        when:
        engine.registerPluginWithMetadata('test-plugin', '0.1')

        then:
        metadata['plugins.test-plugin']
    }

    void "never persist to metadata when plugin is a non transitive dependency"() {
        given:
        def metadata = new MetadataStorage()
        def engine = systemUnderTest(metadata, dependency)

        when:
        engine.registerPluginWithMetadata('test-plugin', '0.1')

        then:
        !metadata['plugins.test-plugin']

        where:
        dependency << [exported('test-plugin'), notExported('test-plugin')]
    }

    private PluginInstallEngine systemUnderTest(MetadataStorage metadata, EnhancedDefaultDependencyDescriptor... dependencies = []) {
        def dependencyManager = dependencyManager(metadata.toMap(), dependencies)
        def engine = engine(dependencyManager, metadataPersistingToStorage(metadata))
        return engine
    }

    private IvyDependencyManager dependencyManager(registered, dependencies) {
        IvyDependencyManager.metaClass.getMetadataRegisteredPluginNames = {-> registered.keySet()}
        IvyDependencyManager.metaClass.getPluginDependencyDescriptors = {-> dependencies}
        def dependencyManager = new IvyDependencyManager('test', '0.1')
        return dependencyManager
    }

    private PluginInstallEngine engine(IvyDependencyManager dependencyManager, Metadata metadata) {
        def buildSettings = new BuildSettings()
        def pluginBuildSettings = new PluginBuildSettings(buildSettings)
        def engine = new PluginInstallEngine(buildSettings, pluginBuildSettings, metadata)
        // setting dependency manager after engine instantiation to avoid constructor hell
        buildSettings.setDependencyManager(dependencyManager)
        return engine
    }

    private Metadata metadataPersistingToStorage(storage) {
        Metadata.metaClass.persist = {-> storage.putAll(delegate)}
        return new Metadata()
    }

    def exported(plugin) {
        descriptor(plugin: plugin, export: true)
    }

    private notExported(plugin) {
        descriptor(plugin: plugin, export: false)
    }

    def transitiveExported(plugin) {
        descriptor(upstream: 'upstream-plugin', plugin: plugin, export: true)
    }

    private transitiveNotExported(plugin) {
        descriptor(upstream: 'upstream-plugin', plugin: plugin, export: false)
    }

    private EnhancedDefaultDependencyDescriptor descriptor(map) {
        def descriptor = new EnhancedDefaultDependencyDescriptor(new ModuleRevisionId(new ModuleId('org.test', map.plugin), '0.1'), true, 'compile')
        descriptor.plugin = map.upstream
        descriptor.export = map.export
        return descriptor
    }

    public void cleanup() {
        metaClassRegistry.removeMetaClass IvyDependencyManager
        metaClassRegistry.removeMetaClass Metadata
    }

    private class MetadataStorage {

        private def metadata = [:]

        MetadataStorage(map = [:]) {
            putAll(map)
        }

        void putAll(Map map) {
            metadata.putAll(map)
        }

        def getProperty(String key) {
            metadata[key]
        }

        Map toMap() {metadata}
    }
}
