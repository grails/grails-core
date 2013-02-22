package org.codehaus.groovy.grails.compiler

import spock.lang.Specification
import org.junit.runner.RunWith
import org.spockframework.runtime.Sputnik
import grails.util.PluginBuildSettings
import grails.util.BuildSettings
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils

@RunWith(Sputnik)
class GrailsProjectWatcherSpec extends Specification {
    static excludedConfig, includedConfig, includedAndExcludedConfig, slurper
    static GrailsProjectWatcher projectWatcher
    void setupSpec() {
        slurper = new ConfigSlurper()
        excludedConfig = slurper.parse("""
            grails.reload.excludes = ['com.domain.package.Class']
        """)
        includedConfig = slurper.parse("""
            grails.reload.includes = ['com.domain.package.Class']
        """)
        includedAndExcludedConfig = slurper.parse("""
            grails.reload.excludes = ['com.domain.package.NonReloadableClass']
            grails.reload.includes = ['com.domain.package.ReloadableClass']
        """)

        def compiler = new GrailsProjectCompiler(new PluginBuildSettings(new BuildSettings()))
        compiler.compilerExtensions = [".java", ".groovy"]
        projectWatcher = new GrailsProjectWatcher(compiler, null)

    }
    def "test that excluded reload classes make it to the project watcher"() {
        given:
        GrailsProjectWatcher.reloadExcludes = []
        GrailsProjectWatcher.reloadIncludes = []
        def excludes = excludedConfig?.grails?.reload?.excludes
        projectWatcher.reloadExcludes = (excludes instanceof List) ? excludes : []

        expect:
        1 == GrailsProjectWatcher.reloadExcludes.size()
    }
    def "test that included reload classes make it to the project watcher"() {
        given:
        GrailsProjectWatcher.reloadExcludes = []
        GrailsProjectWatcher.reloadIncludes = []
        def includes = includedConfig?.grails?.reload?.includes
        projectWatcher.reloadIncludes = (includes instanceof List) ? includes : []

        expect:
        1 == GrailsProjectWatcher.reloadIncludes.size()
    }
    def "test that included and excluded reload classes can be configured"() {
        given:
        GrailsProjectWatcher.reloadExcludes = []
        GrailsProjectWatcher.reloadIncludes = []
        def excludes = excludedConfig?.grails?.reload?.excludes
        projectWatcher.reloadExcludes = (excludes instanceof List) ? excludes : []
        def includes = includedConfig?.grails?.reload?.includes
        projectWatcher.reloadIncludes = (includes instanceof List) ? includes : []

        expect:
        1 == GrailsProjectWatcher.reloadExcludes.size()
        1 == GrailsProjectWatcher.reloadIncludes.size()
    }
    def "test that excluded classes will not be reloaded"() {
        given:
        GrailsProjectWatcher.reloadExcludes = []
        GrailsProjectWatcher.reloadIncludes = []
        def file = new MockFile(System.properties["java.io.tmpdir"])
        def excludes = excludedConfig?.grails?.reload?.excludes
        projectWatcher.reloadExcludes = (excludes instanceof List) ? excludes : []

        when:
        def isReloadable = projectWatcher.fileIsReloadable(file)

        then:
        false == isReloadable

    }
    def "test that included classes will be reloaded"() {
        given:
        GrailsProjectWatcher.reloadExcludes = []
        GrailsProjectWatcher.reloadIncludes = []
        def file = new MockFile(System.properties["java.io.tmpdir"])
        def includes = includedConfig?.grails?.reload?.includes
        projectWatcher.reloadIncludes = (includes instanceof List) ? includes : []

        when:
        def isReloadable = projectWatcher.fileIsReloadable(file)

        then:
        true == isReloadable

    }
    def "test that non-configured classes will be reloaded when includes are not configured"() {
        given:
        GrailsProjectWatcher.reloadExcludes = []
        GrailsProjectWatcher.reloadIncludes = []
        def file = new MockFile(System.properties["java.io.tmpdir"])
        file.absPath = "reloading-test/src/java/com/domain/package/NonConfiguredClassTest.java"

        when:
        def isReloadable = projectWatcher.fileIsReloadable(file)

        then:
        true == isReloadable

    }
    def "test that non-configured classes will NOT be reloaded when includes are configured"() {
        given:
        GrailsProjectWatcher.reloadExcludes = []
        GrailsProjectWatcher.reloadIncludes = []
        def file = new MockFile(System.properties["java.io.tmpdir"])
        file.absPath = "reloading-test/src/java/com/domain/package/NonConfiguredClassTest.java"
        def includes = includedConfig?.grails?.reload?.includes
        projectWatcher.reloadIncludes = (includes instanceof List) ? includes : []

        when:
        def isReloadable = projectWatcher.fileIsReloadable(file)

        then:
        false == isReloadable
    }
    def "test that configured excluded classes will NOT be reloaded and that configured included classes WILL be reloaded"() {
        given:
        GrailsProjectWatcher.reloadExcludes = []
        GrailsProjectWatcher.reloadIncludes = []
        def reloadable = new MockFile(System.properties["java.io.tmpdir"])
        reloadable.absPath = "reloading-test/src/java/com/domain/package/ReloadableClass.java"

        def nonReloadable = new MockFile(System.properties["java.io.tmpdir"])
        nonReloadable.absPath = "reloading-test/src/java/com/domain/package/NonReloadableClass.java"

        def includes = includedAndExcludedConfig?.grails?.reload?.includes
        def excludes = includedAndExcludedConfig?.grails?.reload?.excludes
        projectWatcher.reloadIncludes = (includes instanceof List) ? includes : []
        projectWatcher.reloadExcludes = (excludes instanceof List) ? excludes : []

        when:
        def reloadableIsReloadable = projectWatcher.fileIsReloadable(reloadable)
        def nonReloadableIsReloadable = projectWatcher.fileIsReloadable(nonReloadable)

        then:
        true == reloadableIsReloadable
        false == nonReloadableIsReloadable
    }
}

class MockFile extends File {
    String absPath = "reloading-test/src/java/com/domain/package/Class.java"
    MockFile(String fileName) {
        super(fileName)
    }
    String getAbsolutePath() {
        return absPath
    }
}
