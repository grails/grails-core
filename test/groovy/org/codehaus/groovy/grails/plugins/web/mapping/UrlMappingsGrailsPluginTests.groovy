package org.codehaus.groovy.grails.plugins.web.mapping

import org.springframework.core.io.FileSystemResource
import groovy.xml.StreamingMarkupBuilder

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Feb 18, 2008
*/
class UrlMappingsGrailsPluginTests extends GroovyTestCase {

    void testErrorPageWebXmlPositioning() {
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

        FileSystemResource.metaClass.getFile = {->
            [ eachLine:{ Closure c ->
                c.call('"404"(controller:"foo")')
            }]
        }
        UrlMappingsGrailsPlugin.metaClass.getWatchedResources ={->
            new FileSystemResource("/dummy/path")
        }
        def plugin = new UrlMappingsGrailsPlugin()

        def xml = new XmlSlurper().parseText(text)

        plugin.doWithWebDescriptor(xml)

        def sw = new StringWriter()
        sw = new StreamingMarkupBuilder().bind{
          out << xml
        }
                
        assertEquals '<web-app><filter><filter-name>sitemesh</filter-name><filter-class>org.codehaus.groovy.grails.web.sitemesh.GrailsPageFilter</filter-class></filter><filter><filter-name>urlMapping</filter-name><filter-class>org.codehaus.groovy.grails.web.mapping.filter.UrlMappingsFilter</filter-class></filter><filter-mapping><filter-name>sitemesh</filter-name><url-pattern>/*</url-pattern></filter-mapping><filter-mapping><filter-name>urlMapping</filter-name><url-pattern>/*</url-pattern></filter-mapping><listener><listener-class>org.springframework.web.util.Log4jConfigListener</listener-class></listener><listener><listener-class>org.codehaus.groovy.grails.web.context.GrailsContextLoaderListener</listener-class></listener><servlet><servlet-name>grails</servlet-name><servlet-class>org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet</servlet-class><load-on-startup>1</load-on-startup></servlet><servlet><servlet-name>grails-errorhandler</servlet-name><servlet-class>org.codehaus.groovy.grails.web.servlet.ErrorHandlingServlet</servlet-class></servlet><servlet-mapping><servlet-name>grails</servlet-name><url-pattern>*.dispatch</url-pattern></servlet-mapping><servlet-mapping><servlet-name>grails-errorhandler</servlet-name><url-pattern>/grails-errorhandler</url-pattern></servlet-mapping><welcome-file-list><welcome-file>index.html</welcome-file><welcome-file>index.jsp</welcome-file><welcome-file>index.gsp</welcome-file></welcome-file-list><error-page><error-code>404</error-code><location>/grails-errorhandler</location></error-page></web-app>', sw.toString()
    }

}