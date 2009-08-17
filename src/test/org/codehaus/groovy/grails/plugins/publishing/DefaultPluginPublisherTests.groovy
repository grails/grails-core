package org.codehaus.groovy.grails.plugins.publishing

import groovy.util.slurpersupport.GPathResult
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource

/**
 * @author Graeme Rocher
 * @since 1.2
 */

public class DefaultPluginPublisherTests extends GroovyTestCase{
     void testPublishNewPluginRelease() {
        def publisher = new TestPluginPublisher()
        publisher.testPluginsXml = '''\
<?xml version="1.0" encoding="UTF-8"?>
<plugins revision="44">
    <plugin latest-release="0.1" name="test1">
        <release tag="RELEASE_0_1" type="svn" version="0.1">
            <title>Plugin summary/headline</title>
            <author>Your name</author>
            <authorEmail/>
            <description>\
Brief description of the plugin.
</description>
            <documentation>http://grails.org/Test1+Plugin</documentation>
            <file>file:///Developer/localsvn/grails-test1/tags/RELEASE_0_1/grails-test1-0.1.zip</file>
        </release>
    </plugin>
    <plugin latest-release="0.1" name="test2">
        <release tag="RELEASE_0_1" type="svn" version="0.1">
            <title>Plugin summary/headline</title>
            <author>Your name</author>
            <authorEmail/>
            <description>\
Brief description of the plugin.
</description>
            <documentation>http://grails.org/Test1+Plugin</documentation>
            <file>file:///Developer/localsvn/grails-test1/tags/RELEASE_0_1/grails-test1-0.1.zip</file>
        </release>
    </plugin>
</plugins>
'''
         publisher.testMetadata = '''\
<plugin name='foo-bar' version='0.1' grailsVersion='1.2-SNAPSHOT &gt; *'>
  <author>Bob</author>
  <title>FooBar Plugin</title>
  <description>some text</description>
  <documentation>http://grails.org/plugin/foo-bar</documentation>
  <resources>
    <resource>DataSource</resource>
    <resource>UrlMappings</resource>
  </resources>
  <dependencies />
  <behavior />
</plugin>
'''



         def result = publisher.publishRelease("foo-bar", new ByteArrayResource("".bytes))

         def writer = new StringWriter()
         writer << new groovy.xml.StreamingMarkupBuilder().bind {
               mkp.yield result
         }
         new XmlNodePrinter().print(new XmlParser().parseText(writer.toString()))
         result = new XmlSlurper().parseText(writer.toString()) 

         assertEquals 3, result.plugin.size()

         def testPlugin = result.plugin.find { it.@name == 'foo-bar' }

         assertEquals 'foo-bar', testPlugin.@name.text()
         assertEquals '0.1', testPlugin.'@latest-release'.text()
         def releaseInfo = testPlugin.release

         assertEquals 'RELEASE_0_1', releaseInfo.@tag.text()
         assertEquals '0.1', releaseInfo.@version.text()
         assertEquals 'Bob', releaseInfo.author.text()
         assertEquals 'FooBar Plugin', releaseInfo.title.text()
     }

     void testPublishExistingPluginRelease() {
         def publisher = new TestPluginPublisher()
         publisher.testPluginsXml = '''\
<?xml version="1.0" encoding="UTF-8"?>
<plugins revision="0">
    <plugin latest-release="0.1" name="foo-bar">
        <release tag="RELEASE_0_1" type="svn" version="0.1">
            <title>FooBar Plugin</title>
            <author>Bob</author>
            <authorEmail/>
            <description/>
            <documentation>http://grails.org/Test1+Plugin</documentation>
            <file>file:///Developer/localsvn/grails-test1/tags/RELEASE_0_1/grails-test1-0.1.zip</file>
        </release>
    </plugin>

</plugins>'''
          publisher.testMetadata = '''\
 <plugin name='foo-bar' version='0.2' grailsVersion='1.2-SNAPSHOT &gt; *'>
   <author>Bob</author>
   <title>FooBar Plugin</title>
   <description>some text</description>
   <documentation>http://grails.org/plugin/foo-bar</documentation>
   <resources>
     <resource>DataSource</resource>
     <resource>UrlMappings</resource>
   </resources>
   <dependencies />
   <behavior />
 </plugin>
 '''

             def result = publisher.publishRelease("foo-bar", new ByteArrayResource("".bytes))

          def writer = new StringWriter()
          writer << new groovy.xml.StreamingMarkupBuilder().bind {
                mkp.yield result
          }
          new XmlNodePrinter().print(new XmlParser().parseText(writer.toString()))
          result = new XmlSlurper().parseText(writer.toString())

          assertEquals 1, result.plugin.size()

          def testPlugin = result.plugin.find { it.@name == 'foo-bar' }

          assertEquals 'foo-bar', testPlugin.@name.text()
          assertEquals '0.2', testPlugin.'@latest-release'.text()
          def releaseInfo = testPlugin.release

          assertEquals 2, releaseInfo.size()

          assertEquals 'RELEASE_0_1', releaseInfo[0].@tag.text()
          assertEquals '0.1', releaseInfo[0].@version.text()
          assertEquals 'Bob', releaseInfo[0].author.text()
          assertEquals 'FooBar Plugin', releaseInfo[0].title.text()

         assertEquals 'RELEASE_0_2', releaseInfo[1].@tag.text()
         assertEquals '0.2', releaseInfo[1].@version.text()
         assertEquals 'Bob', releaseInfo[1].author.text()
         assertEquals 'FooBar Plugin', releaseInfo[1].title.text()

     }


    void testPublishExistingPluginReleaseDontMakeLatest() {
        def publisher = new TestPluginPublisher()
        publisher.testPluginsXml = '''\
<?xml version="1.0" encoding="UTF-8"?>
<plugins revision="0">
<plugin latest-release="0.1" name="foo-bar">
   <release tag="RELEASE_0_1" type="svn" version="0.1">
       <title>FooBar Plugin</title>
       <author>Bob</author>
       <authorEmail/>
       <description/>
       <documentation>http://grails.org/Test1+Plugin</documentation>
       <file>file:///Developer/localsvn/grails-test1/tags/RELEASE_0_1/grails-test1-0.1.zip</file>
   </release>
</plugin>

</plugins>'''
         publisher.testMetadata = '''\
<plugin name='foo-bar' version='0.2' grailsVersion='1.2-SNAPSHOT &gt; *'>
  <author>Bob</author>
  <title>FooBar Plugin</title>
  <description>some text</description>
  <documentation>http://grails.org/plugin/foo-bar</documentation>
  <resources>
    <resource>DataSource</resource>
    <resource>UrlMappings</resource>
  </resources>
  <dependencies />
  <behavior />
</plugin>
'''

            def result = publisher.publishRelease("foo-bar", new ByteArrayResource("".bytes),false)

         def writer = new StringWriter()
         writer << new groovy.xml.StreamingMarkupBuilder().bind {
               mkp.yield result
         }
         new XmlNodePrinter().print(new XmlParser().parseText(writer.toString()))
         result = new XmlSlurper().parseText(writer.toString())

         assertEquals 1, result.plugin.size()

         def testPlugin = result.plugin.find { it.@name == 'foo-bar' }

         assertEquals 'foo-bar', testPlugin.@name.text()
         assertEquals '0.1', testPlugin.'@latest-release'.text()
         def releaseInfo = testPlugin.release

         assertEquals 2, releaseInfo.size()

         assertEquals 'RELEASE_0_1', releaseInfo[0].@tag.text()
         assertEquals '0.1', releaseInfo[0].@version.text()
         assertEquals 'Bob', releaseInfo[0].author.text()
         assertEquals 'FooBar Plugin', releaseInfo[0].title.text()

        assertEquals 'RELEASE_0_2', releaseInfo[1].@tag.text()
        assertEquals '0.2', releaseInfo[1].@version.text()
        assertEquals 'Bob', releaseInfo[1].author.text()
        assertEquals 'FooBar Plugin', releaseInfo[1].title.text()

    }

     void testPublishFirstPluginRelease() {
         def publisher = new TestPluginPublisher()
         publisher.testPluginsXml = '<?xml version="1.0" encoding="UTF-8"?><plugins revision="0" />'
          publisher.testMetadata = '''\
 <plugin name='foo-bar' version='0.1' grailsVersion='1.2-SNAPSHOT &gt; *'>
   <author>Bob</author>
   <title>FooBar Plugin</title>
   <description>some text</description>
   <documentation>http://grails.org/plugin/foo-bar</documentation>
   <resources>
     <resource>DataSource</resource>
     <resource>UrlMappings</resource>
   </resources>
   <dependencies />
   <behavior />
 </plugin>
 '''



          def result = publisher.publishRelease("foo-bar", new ByteArrayResource("".bytes))

          def writer = new StringWriter()
          writer << new groovy.xml.StreamingMarkupBuilder().bind {
                mkp.yield result
          }
          new XmlNodePrinter().print(new XmlParser().parseText(writer.toString()))
          result = new XmlSlurper().parseText(writer.toString())

          assertEquals 1, result.plugin.size()

          def testPlugin = result.plugin.find { it.@name == 'foo-bar' }

          assertEquals 'foo-bar', testPlugin.@name.text()
          assertEquals '0.1', testPlugin.'@latest-release'.text()
          def releaseInfo = testPlugin.release

          assertEquals 'RELEASE_0_1', releaseInfo.@tag.text()
          assertEquals '0.1', releaseInfo.@version.text()
          assertEquals 'Bob', releaseInfo.author.text()
          assertEquals 'FooBar Plugin', releaseInfo.title.text()

     }
}

class TestPluginPublisher extends DefaultPluginPublisher{
    String testPluginsXml
    String testMetadata

    public TestPluginPublisher(String revNumber) {
        super("0");    
    }

    protected GPathResult getPluginMetadata(String pluginName) {
        new XmlSlurper().parseText(testMetadata)
    }

    public GPathResult parsePluginList(Resource pluginsListFile) {
        new XmlSlurper().parseText(testPluginsXml)
    }


}