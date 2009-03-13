/*
 * Copyright 2004-2005 the original author or authors.
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

package org.codehaus.groovy.grails.web.pages.ext.jsp

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.tools.RootLoader
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.mock.web.MockServletContext

class TagLibraryResolverTests extends GroovyTestCase{
    

    void testResolveTagLibraryFromJar() {
        def resolver = new MockRootLoaderTagLibraryResolver()
        resolver.servletContext = new MockServletContext()
        resolver.grailsApplication= new DefaultGrailsApplication()

        JspTagLib tagLib = resolver.resolveTagLibrary( "http://java.sun.com/jsp/jstl/fmt" )

        assert tagLib

        JspTag messageTag = tagLib.getTag("message")
        assert messageTag

        // when resolving second time the code will take a different branch
        // because certain locations have been cached. This test tests that

        tagLib = resolver.resolveTagLibrary("http://java.sun.com/jstl/core_rt")

        assert tagLib

        assert tagLib.getTag("redirect")
    }

    void testResolveTagLibraryFromWebXml() {

        def resolver = new MockWebXmlTagLibraryResolver()
        resolver.servletContext = new MockServletContext()
        resolver.grailsApplication= new DefaultGrailsApplication()

        JspTagLib tagLib = resolver.resolveTagLibrary( "http://grails.codehaus.org/tags" )

        assert tagLib

        assert tagLib.getTag("javascript")

    }

}
class MockWebXmlTagLibraryResolver extends TagLibraryResolver {

    protected RootLoader resolveRootLoader() {
        println "WHAT THE FUCK"
        println "WHAT THE FUCK"
        println "WHAT THE FUCK"
        println "WHAT THE FUCK"
        
        new RootLoader([] as URL[], Thread.currentThread().getContextClassLoader())
    }

    protected InputStream getTldFromServletContext(String loc) {

        assert "/WEB-INF/tld/grails.tld" == loc

        new ByteArrayResource('''<?xml version="1.0" encoding="UTF-8"?>
<taglib xmlns="http://java.sun.com/xml/ns/j2ee"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee
            http://java.sun.com/xml/ns/j2ee/web-jsptaglibrary_2_0.xsd"
        version="2.0">
    <description>The Grails (Groovy on Rails) custom tag library</description>
    <tlib-version>0.2</tlib-version>
    <short-name>grails</short-name>
    <uri>http://grails.codehaus.org/tags</uri>

    <tag>
        <description>
        	Includes a javascript src file, library or inline script
	 	if the tag has no src or library attributes its assumed to be an inline script
        </description>
        <name>javascript</name>
        <tag-class>org.codehaus.groovy.grails.plugins.web.taglib.JavascriptTagLib</tag-class>
        <body-content>JSP</body-content>
        <attribute>
            <description>A predefined JavaScript or AJAX library to load</description>
            <name>library</name>
            <required>false</required>
            <rtexprvalue>true</rtexprvalue>
        </attribute>
        <attribute>
            <description>A custom (or unknown to Grails) JavaScript source file</description>
            <name>src</name>
            <required>false</required>
            <rtexprvalue>true</rtexprvalue>
        </attribute>
        <attribute>
            <description>Since 0.6 Specifies the full base url to prepend to the library name</description>
            <name>base</name>
            <required>false</required>
            <rtexprvalue>true</rtexprvalue>
        </attribute>
        <dynamic-attributes>false</dynamic-attributes>
    </tag>
</taglib>

'''.getBytes()).getInputStream()

    }

    protected Resource getWebXmlFromServletContext() {
        new ByteArrayResource('''<?xml version="1.0" encoding="UTF-8"?>

<!DOCTYPE web-app PUBLIC
    "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
    "http://java.sun.com/dtd/web-app_2_3.dtd">

<web-app>

    <display-name>/@grails.project.key@</display-name>

	<!-- Grails dispatcher servlet -->
	<servlet>
		<servlet-name>grails</servlet-name>
        <servlet-class>org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet</servlet-class>
		<load-on-startup>2</load-on-startup>
	</servlet>

    <servlet-mapping>
        <servlet-name>gsp</servlet-name>
        <url-pattern>*.gsp</url-pattern>
    </servlet-mapping>

    <taglib>
        <taglib-uri>http://grails.codehaus.org/tags</taglib-uri>
        <taglib-location>/WEB-INF/tld/grails.tld</taglib-location>
    </taglib>
</web-app>

'''.getBytes())
    }


}