/*
 * Copyright 2007-2008 the original author or authors.
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

package grails.build.eclipse

class EclipseClasspathTests extends GroovyTestCase {
    void testClasspath() {
        if(Boolean.getBoolean("groovy.grails.joint")) return;
        def result = new XmlSlurper().parse(new File('.classpath'))
        def libs = result.classpathentry.grep {it.'@kind' == 'lib'}
        String errorMessage = ''
        libs.'@path'.each {
            errorMessage = 'Library ' + it + ' is referenced in the .classpath '
            errorMessage += ' file but is not present'
            assertTrue(errorMessage, new File(it.toString()).exists())
        }
    }

    /**
     * This test ensures that the launch file refers to the correct
     * version of Groovy.
     */
    void testLaunchTemplate() {
        if(Boolean.getBoolean("groovy.grails.joint")) return;
        // Extract the classpath entry for Groovy from the .launch file.
        def result = new XmlSlurper().parse(new File('src/grails/templates/ide-support/eclipse/.launch'))
        def classpathEntries = result.listAttribute.find {it.'@key' == 'org.eclipse.jdt.launching.CLASSPATH'}
        def groovyPath = classpathEntries.listEntry.find {it.@value.text().contains("groovy-all")}?.@value?.text()

        assertNotNull groovyPath

        // Now extract the path to the Groovy JAR using a regular expression.
        def m = groovyPath =~ /GRAILS_HOME\/(lib\/groovy-all-[\w-\.]+\.jar)/
        assertTrue m.find()

        // Now make sure that the Groovy JAR referred to exists!
        File file = new File(m[0][1])
        assertTrue "Eclipse .launch template refers to '$file', which doesn't exist!", file.exists()
    }
}
