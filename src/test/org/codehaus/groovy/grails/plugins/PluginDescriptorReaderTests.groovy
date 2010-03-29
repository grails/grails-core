package org.codehaus.groovy.grails.plugins

import org.springframework.core.io.ByteArrayResource;

class PluginDescriptorReaderTests extends GroovyTestCase {
	
	void testReadPluginInfoFromDescriptor() {
		def pluginReader = new AstPluginDescriptorReader()
		
		def plugin = pluginReader.readPluginInfo(new ByteArrayResource('''
import org.codehaus.groovy.grails.plugins.springsecurity.AuthorizeTools				
class FooBarGrailsPlugin {
  String version = "0.1"
  String grailsVersion = "1.3"				
}
'''.bytes))

	
		assert "0.1" == plugin.version : "plugin version should have been 0.1"
		assert "foo-bar" == plugin.name : "plugin name should have been 'foo-bar'"
		assert "1.3" == plugin.grailsVersion : "grails version should have been '1.2'"
		assert "foo-bar-1.3" == plugin.fullName : "The full plugin name should be 'foo-bar-1.3'"
	}

}
