/*
 * Copyright 2011 SpringSource
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

package org.codehaus.groovy.grails.plugins.publishing

import grails.util.GrailsNameUtils
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.springframework.core.io.Resource

/**
 * Packages a plugin in source or binary form.
 *
 * @since 1.4
 */
class PluginPackager {

    public static final INCLUDED_RESOURCES = [
        "application.properties",
        "*GrailsPlugin.groovy",
        "plugin.xml",
        "LICENSE",
        "LICENSE.txt",
        "dependencies.groovy",
        "grails-app/**",
        "lib/**",
        "scripts/**",
        "web-app/**",
        "src/**",
        "docs/api/**",
        "docs/gapi/**"
    ]

    public static final EXCLUDED_RESOURCES = [
        "web-app/WEB-INF/**",
        "web-app/plugins/**",
        "grails-app/conf/spring/resources.groovy",
        "grails-app/conf/*DataSource.groovy",
        "grails-app/conf/DataSource.groovy",
        "grails-app/conf/BootStrap.groovy",
        "grails-app/conf/Config.groovy",
        "grails-app/conf/BuildConfig.groovy",
        "grails-app/conf/UrlMappings.groovy",
        "**/.svn/**",
        "test/**",
        "**/CVS/**"
    ]

    private GrailsPluginInfo pluginInfo
    private Resource[] resourceList
    private File projectWorkDir
    private AntBuilder ant
    private File resourcesDir

    List<File> jarFiles = []
    boolean hasApplicationDependencies

    PluginPackager(GrailsPluginInfo pluginInfo, Resource[] resourceList, File projectWorkDir) {
        this.pluginInfo = pluginInfo
        this.resourceList = resourceList
        this.projectWorkDir = projectWorkDir
    }

    File getResourcesDir() {
        if (this.resourcesDir == null) resourcesDir = basedir
        return resourcesDir
    }

    void setResourcesDir(File resourcesDir) {
        this.resourcesDir = resourcesDir
    }

    AntBuilder getAnt() {
        if (this.ant == null) ant = new AntBuilder()
        return ant
    }

    void setAnt(AntBuilder ant) {
        this.ant = ant
    }

    File getBasedir() {
        pluginInfo.pluginDir.file
    }

    /**
     * Generates the plugin.xml file for the plugin descriptor
     *
     * @param descriptor The descriptor
     * @return The plugin properties
     */
    def generatePluginXml(File descriptor) {
        def pluginBaseDir = descriptor.parentFile
        def pluginProps = pluginInfo
        // Work out what the name of the plugin is from the name of the descriptor file.
        def pluginName = GrailsNameUtils.getPluginName(descriptor.name)

        // Remove the existing 'plugin.xml' if there is one.
        def pluginXml = new File(pluginBaseDir, "plugin.xml")
        pluginXml.delete()

        // Use MarkupBuilder with indenting to generate the file.
        def writer = new IndentPrinter(new PrintWriter(new FileWriter(pluginXml)))
        def generator = new PluginDescriptorGenerator(pluginName, resourceList)

        pluginProps["type"] = descriptor.name - '.groovy'
        generator.generatePluginXml(pluginProps, writer)

        return pluginProps
    }

    def packagePlugin(String pluginName, File classesDir, File targetDir) {
        generateDependencyDescriptor()
        if (!pluginInfo.packaging || pluginInfo.packaging == 'source') {
            return packageSource(pluginName, classesDir, targetDir)
        }
        return packageBinary(pluginName, classesDir, targetDir)
    }

    String packageSource(String pluginName, File classesDir, File targetDir) {
        def pluginProps = pluginInfo
        generateDependencyDescriptor()

        // Package plugin's zip distribution
        def pluginZip = "${basedir}/grails-${pluginName}-${pluginInfo.version}.zip"
        ant.delete(file:pluginZip)

        def pluginExcludes = EXCLUDED_RESOURCES
        def pluginIncludes = INCLUDED_RESOURCES
        def pluginGrailsVersion = "${GrailsUtil.grailsVersion} > *"

        if (pluginProps?.pluginExcludes) {
            pluginExcludes.addAll(pluginInfo?.pluginExcludes)
        }

        def includesList = pluginIncludes.join(",")
        def excludesList = pluginExcludes.join(",")
        def libsDir = new File("${projectWorkDir}/tmp-libs")
        ant.delete(dir:libsDir, failonerror:false)

        if (jarFiles) {
            ant.mkdir(dir:"${libsDir}/lib")
            ant.copy(todir:"${libsDir}/lib") {
                for (File file in jarFiles) {
                    fileset(dir:file.parentFile, includes:file.name)
                }
            }
        }

        def dependencyInfoDir = new File("$projectWorkDir/plugin-info")
        ant.zip(destfile:pluginZip, filesonly:true) {
            fileset(dir:basedir, includes:includesList, excludes:excludesList)
            if (dependencyInfoDir.exists()) {
                fileset(dir:dependencyInfoDir)
            }
            if (libsDir.exists()) {
                fileset(dir:libsDir)
            }
        }

        return pluginZip
    }

    String packageBinary(String pluginName, File classesDir, File targetDir) {
        def pluginProps = pluginInfo
        ant.taskdef (name: 'gspc', classname : 'org.codehaus.groovy.grails.web.pages.GroovyPageCompilerTask')
        // compile gsps in grails-app/views directory
        File gspTmpDir = new File(projectWorkDir, "gspcompile")
        ant.gspc(destdir:classesDir,
                 srcdir:"${basedir}/grails-app/views",
                 packagename:GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(pluginName),
                 serverpath:"/WEB-INF/grails-app/views/",
                 classpathref:"grails.compile.classpath",
                 tmpdir:gspTmpDir)

        def metaInf = "${classesDir}/META-INF"
        def excludeList = ['application.properties']
        def defaultExcludes = ["UrlMappings", "DataSource", "BuildConfig", "Config"]
        for (exclude in defaultExcludes) {
            excludeList << "${exclude}.class" << "${exclude}\$*.class"
        }

        if (pluginProps?.pluginExcludes) {
            for (exclude in pluginProps.pluginExcludes) {
                exclude = "/$pluginName/$exclude"

                def excludeMatch = exclude =~ PluginDescriptorGenerator.ARTEFACT_PATTERN
                if (excludeMatch) {
                    exclude = excludeMatch[0][1].replaceAll(/[\/\\]/, /\./)
                    excludeList << "${exclude}.class" << "${exclude}\$*.class"
                }
            }
        }

        def destinationFile = "${targetDir}/grails-plugin-${pluginName}-${pluginProps.version}.jar"
        ant.sequential {
            mkdir(dir:metaInf)
            copy(file:"${basedir}/plugin.xml", tofile:"${metaInf}/grails-plugin.xml")
            move(file:"${classesDir}/gsp/views.properties", todir:metaInf, failonerror:false)
            mkdir(dir:"${metaInf}/grails-app/i18n")
            copy(todir:"${metaInf}/grails-app/i18n", failonerror:false) {
                fileset(dir:"${resourcesDir}/grails-app/i18n")
            }
            mkdir(dir:"${metaInf}/scripts")
            copy(todir:"${metaInf}/scripts") {
                fileset(dir:"${basedir}/scripts",
                        excludes:"_Install.groovy,_Uninstall.groovy,_Upgrade.groovy")
            }
            mkdir(dir:"${classesDir}/src")
            copy(todir:"${classesDir}/src") {
                fileset(dir:"${basedir}/src", excludes:"groovy/**,java/**")
            }

            jar(destfile:destinationFile) {
                fileset(dir:classesDir, excludes:excludeList.join(','))
                manifest {
                    attribute name:"Implementation-Title", value:pluginProps.title
                    attribute name:"Implementation-Version", value:pluginProps.version
                }
            }
        }
        return destinationFile
    }

    protected generateDependencyDescriptor() {
        ant.delete(dir:"$projectWorkDir/plugin-info", failonerror:false)
        if (hasApplicationDependencies) {
            ant.mkdir(dir:"$projectWorkDir/plugin-info")
            ant.copy(file:"${pluginInfo.pluginDir.file}/grails-app/conf/BuildConfig.groovy",
                     tofile:"$projectWorkDir/plugin-info/dependencies.groovy", failonerror:false)
        }
    }
}
