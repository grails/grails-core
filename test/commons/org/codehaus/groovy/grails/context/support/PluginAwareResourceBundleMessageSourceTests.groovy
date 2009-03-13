package org.codehaus.groovy.grails.context.support

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.support.MockFileResource
import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.springframework.core.io.Resource
import grails.util.Metadata

/**
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Feb 6, 2009
 */

public class PluginAwareResourceBundleMessageSourceTests extends GroovyTestCase{

    protected void setUp() {
        Metadata.getCurrent().put(Metadata.WAR_DEPLOYED, "true")
    }

    protected void tearDown() {
        Metadata.getCurrent().put(Metadata.WAR_DEPLOYED, "")
    }




    void testMessageSource() {
        def testPlugin = new GroovyClassLoader().parseClass('''
class TestTwoGrailsPlugin {
    def version = 0.2
}
''')
        def messageSource = new TestPluginAwareResourceBundleMessageSource()


        def pluginManager = new DefaultGrailsPluginManager([testPlugin] as Class[], new DefaultGrailsApplication())

        pluginManager.loadPlugins()
        
        messageSource.pluginManager = pluginManager
        messageSource.basename = "WEB-INF/grails-app/i18n/messages"
        def loader = new MockStringResourceLoader()
        loader.registerMockResource("WEB-INF/plugins/test-two-0.2/grails-app/i18n/messages.properties", '''
foo.bar=test
one.two=wrong
''')
        loader.registerMockResource("WEB-INF/grails-app/i18n/messages.properties", '''
one.two=test
''')


        messageSource.resourceLoader = loader
        messageSource.afterPropertiesSet()

        assertTrue "plugin base name should have loaded", messageSource.pluginBaseNames.size() > 0
        assertTrue "plugin base names should have contained WEB-INF/plugins/test-two-0.2/grails-app/i18n/messages", messageSource.pluginBaseNames.contains("WEB-INF/plugins/test-two-0.2/grails-app/i18n/messages")

        assertEquals "test", messageSource.getMessage("foo.bar", [] as Object[], Locale.default)
        assertEquals "test", messageSource.getMessage("one.two", [] as Object[], Locale.default)
    }
}
class TestPluginAwareResourceBundleMessageSource extends PluginAwareResourceBundleMessageSource {
    protected Resource[] getPluginBundles(String pluginName) {
        [new MockFileResource("grails-app/i18n/messages.properties", '''
foo.bar=test
''')] as Resource[]        
    }
}
