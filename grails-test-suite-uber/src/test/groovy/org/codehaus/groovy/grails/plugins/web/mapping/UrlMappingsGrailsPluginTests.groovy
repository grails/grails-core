package org.codehaus.groovy.grails.plugins.web.mapping

import groovy.xml.StreamingMarkupBuilder

import org.springframework.core.io.FileSystemResource

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 */
class UrlMappingsGrailsPluginTests extends GroovyTestCase {

    def text = '''<?xml version="1.0"?>
 <web-app>
 <filter>
 <filter-name>sitemesh</filter-name>
 <filter-class>org.codehaus.groovy.grails.web.sitemesh.GrailsPageFilter</filter-class>
 </filter>
 <filter-mapping>
 <filter-name>sitemesh</filter-name>
 <url-pattern>/*</url-pattern>
 </filter-mapping>
 <listener>
 <listener-class>org.springframework.web.util.Log4jConfigListener</listener-class>
 </listener>
 <listener>
 <listener-class>org.codehaus.groovy.grails.web.context.GrailsContextLoaderListener</listener-class>
 </listener>
 <servlet>
 <servlet-name>grails</servlet-name>
 <servlet-class>org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet</servlet-class>
 <load-on-startup>1</load-on-startup>
 </servlet>
 <servlet-mapping>
 <servlet-name>grails</servlet-name>
 <url-pattern>*.dispatch</url-pattern>
 </servlet-mapping>
 <welcome-file-list>
 <welcome-file>index.html</welcome-file>
 <welcome-file>index.jsp</welcome-file>
 <welcome-file>index.gsp</welcome-file>
 </welcome-file-list>
 </web-app>
 '''

    void testNoDuplicateErrorCodes() {
        FileSystemResource.metaClass.getFile = {->
            [ text: '"404"(controller:"foo")' ]
        }

        UrlMappingsGrailsPlugin.metaClass.getWatchedResources ={->
            [new FileSystemResource("/dummy/path"), new FileSystemResource("/dummy/two")]
        }

        def plugin = new UrlMappingsGrailsPlugin()
        def xml = new XmlSlurper().parseText(text)

        plugin.doWithWebDescriptor(xml)

        def sw = new StringWriter()
        sw = new StreamingMarkupBuilder().bind { out << xml }

        def newXml = new XmlSlurper().parseText(sw.toString())
        assertEquals 1, newXml.'error-page'.size()
    }

    void testErrorPageWebXmlPositioning() {
        FileSystemResource.metaClass.getFile = {->
            [ text: '"404"(controller:"foo")' ]
        }

        UrlMappingsGrailsPlugin.metaClass.getWatchedResources = { ->
            new FileSystemResource("/dummy/path")
        }
        def plugin = new UrlMappingsGrailsPlugin()
        def xml = new XmlSlurper().parseText(text)

        plugin.doWithWebDescriptor(xml)

        def sw = new StringWriter()
        sw = new StreamingMarkupBuilder().bind{ out << xml }

        assertEquals '<web-app><filter><filter-name>sitemesh</filter-name><filter-class>org.codehaus.groovy.grails.web.sitemesh.GrailsPageFilter</filter-class></filter><filter><filter-name>urlMapping</filter-name><filter-class>org.codehaus.groovy.grails.web.mapping.filter.UrlMappingsFilter</filter-class></filter><filter-mapping><filter-name>sitemesh</filter-name><url-pattern>/*</url-pattern></filter-mapping><filter-mapping><filter-name>urlMapping</filter-name><url-pattern>/*</url-pattern><dispatcher>FORWARD</dispatcher><dispatcher>REQUEST</dispatcher></filter-mapping><listener><listener-class>org.springframework.web.util.Log4jConfigListener</listener-class></listener><listener><listener-class>org.codehaus.groovy.grails.web.context.GrailsContextLoaderListener</listener-class></listener><servlet><servlet-name>grails</servlet-name><servlet-class>org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet</servlet-class><load-on-startup>1</load-on-startup></servlet><servlet><servlet-name>grails-errorhandler</servlet-name><servlet-class>org.codehaus.groovy.grails.web.servlet.ErrorHandlingServlet</servlet-class></servlet><servlet-mapping><servlet-name>grails</servlet-name><url-pattern>*.dispatch</url-pattern></servlet-mapping><servlet-mapping><servlet-name>grails-errorhandler</servlet-name><url-pattern>/grails-errorhandler</url-pattern></servlet-mapping><welcome-file-list><welcome-file>index.html</welcome-file><welcome-file>index.jsp</welcome-file><welcome-file>index.gsp</welcome-file></welcome-file-list><error-page><error-code>404</error-code><location>/grails-errorhandler</location></error-page></web-app>', sw.toString()
    }

    void testCommentsOnUrlMappingsBeginningOfLine() {
        FileSystemResource.metaClass.getFile = {->
            [ text: '// "404"(controller:"foo")' ]
        }

        UrlMappingsGrailsPlugin.metaClass.getWatchedResources = { ->
            new FileSystemResource("/dummy/path")
        }
        def plugin = new UrlMappingsGrailsPlugin()
        def xml = new XmlSlurper().parseText(text)

        plugin.doWithWebDescriptor(xml)

        def sw = new StringWriter()
        sw = new StreamingMarkupBuilder().bind{ out << xml }

        def content = sw.toString()

        assertFalse content.contains(
            "<error-page>" +
                "<error-code>404</error-code>" +
                "<location>/grails-errorhandler</location>" +
            "</error-page>")
    }

    void testCommentsOnUrlMappingsMultiLineComment() {
        FileSystemResource.metaClass.getFile = {->
            [ text: '''\
                /*
                "404"(controller: "foo")
                */
              ''']
            [ text: '/* \n "404"(controller:"foo") \n */' ]
        }

        UrlMappingsGrailsPlugin.metaClass.getWatchedResources = { ->
            new FileSystemResource("/dummy/path")
        }
        def plugin = new UrlMappingsGrailsPlugin()
        def xml = new XmlSlurper().parseText(text)

        plugin.doWithWebDescriptor(xml)

        def sw = new StringWriter()
        sw = new StreamingMarkupBuilder().bind{ out << xml }

        def content = sw.toString()

        assertFalse content.contains(
            "<error-page>" +
                "<error-code>404</error-code>" +
                "<location>/grails-errorhandler</location>" +
            "</error-page>")
    }

    void testCommentsOnUrlMappingsMultiLineCommentBeforeGoodMapping() {
        FileSystemResource.metaClass.getFile = {->
            [ text: '/* "404"(controller:"foo") */ "400"(controller:"foo")' ]
        }

        UrlMappingsGrailsPlugin.metaClass.getWatchedResources = { ->
            new FileSystemResource("/dummy/path")
        }
        def plugin = new UrlMappingsGrailsPlugin()
        def xml = new XmlSlurper().parseText(text)

        plugin.doWithWebDescriptor(xml)

        def sw = new StringWriter()
        sw = new StreamingMarkupBuilder().bind{ out << xml }

        def content = sw.toString()

        assertFalse content.contains(
            "<error-page>" +
                "<error-code>404</error-code>" +
                "<location>/grails-errorhandler</location>" +
            "</error-page>")

        assertTrue content.contains(
            "<error-page>" +
                "<error-code>400</error-code>" +
                "<location>/grails-errorhandler</location>" +
            "</error-page>")
    }

    void testCommentCharactersInsideStrings() {
        FileSystemResource.metaClass.getFile = {->
            [ text: '''\
                "/*" (controller: "foo")
                "\\\\"bar" (controller: "bar")
                "400" (controller: "error")
                '\\\\'bar' (controller: "bar")
                "404" (controller: "error")
                "*/" (controller: "foo")
            ''']
        }

        UrlMappingsGrailsPlugin.metaClass.getWatchedResources = { ->
            new FileSystemResource("/dummy/path")
        }
        def plugin = new UrlMappingsGrailsPlugin()
        def xml = new XmlSlurper().parseText(text)

        plugin.doWithWebDescriptor(xml)

        def sw = new StringWriter()
        sw = new StreamingMarkupBuilder().bind{ out << xml }

        def content = sw.toString()

        assertTrue content.contains(
            "<error-page>" +
                "<error-code>400</error-code>" +
                "<location>/grails-errorhandler</location>" +
            "</error-page>")

        assertTrue content.contains(
            "<error-page>" +
                "<error-code>404</error-code>" +
                "<location>/grails-errorhandler</location>" +
            "</error-page>")
    }

    void testCommentCharactersInsideMultilineStrings() {
        FileSystemResource.metaClass.getFile = {->
            [ text:
                "'''\\\n" +
                "Test /*\n" +
                "'''\n"+
                "\"400\" (controller: \"error\")\n" +
                "/*Test*/\n"+
                "\"404\" (controller: \"error\")\n"
            ]
        }

        UrlMappingsGrailsPlugin.metaClass.getWatchedResources = { ->
            new FileSystemResource("/dummy/path")
        }
        def plugin = new UrlMappingsGrailsPlugin()
        def xml = new XmlSlurper().parseText(text)

        plugin.doWithWebDescriptor(xml)

        def sw = new StringWriter()
        sw = new StreamingMarkupBuilder().bind{ out << xml }

        def content = sw.toString()

        assertTrue content.contains(
            "<error-page>" +
                "<error-code>400</error-code>" +
                "<location>/grails-errorhandler</location>" +
            "</error-page>")

        assertTrue content.contains(
            "<error-page>" +
                "<error-code>404</error-code>" +
                "<location>/grails-errorhandler</location>" +
            "</error-page>")
    }

    void testCommentCharactersInsideRegularExpressions() {
        FileSystemResource.metaClass.getFile = {->
            [ text: '''\
                /\\/*/ (controller: "foo")
                "404" (controller: "error")
                /* Comment */
                "foo" (controller: "foo")
            ''']
        }

        UrlMappingsGrailsPlugin.metaClass.getWatchedResources = { ->
            new FileSystemResource("/dummy/path")
        }
        def plugin = new UrlMappingsGrailsPlugin()
        def xml = new XmlSlurper().parseText(text)

        plugin.doWithWebDescriptor(xml)

        def sw = new StringWriter()
        sw = new StreamingMarkupBuilder().bind{ out << xml }

        def content = sw.toString()

        assertTrue content.contains(
            "<error-page>" +
                "<error-code>404</error-code>" +
                "<location>/grails-errorhandler</location>" +
            "</error-page>")
    }


    protected void tearDown() {
        GroovySystem.metaClassRegistry.removeMetaClass(FileSystemResource)
        GroovySystem.metaClassRegistry.removeMetaClass(UrlMappingsGrailsPlugin)
    }
}
