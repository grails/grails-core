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

import grails.util.BuildSettings
import grails.util.GrailsNameUtils

import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import org.codehaus.groovy.grails.io.support.FileSystemResource
import org.codehaus.groovy.grails.io.support.Resource
import org.codehaus.groovy.grails.plugins.AstPluginDescriptorReader
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.springframework.util.AntPathMatcher

/**
 * Packages a plugin in source or binary form.
 *
 * @since 2.0
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
    private BuildSettings buildSettings
    private AntPathMatcher antPathMatcher = new AntPathMatcher()

    List<File> jarFiles = []
    boolean hasApplicationDependencies

    GrailsBuildEventListener eventListener
    BuildSettings grailsSettings

    PluginPackager(BuildSettings buildSettings, pluginInfo, Resource[] resourceList, File projectWorkDir,
                   GrailsBuildEventListener eventListener, BuildSettings grailsSettings) {
        this.pluginInfo = pluginInfo
        this.resourceList = resourceList
        this.projectWorkDir = projectWorkDir
        this.buildSettings = buildSettings
        this.eventListener = eventListener
        this.grailsSettings = grailsSettings
    }

    File getResourcesDir() {
        if (resourcesDir == null) resourcesDir = basedir
        return resourcesDir
    }

    void setResourcesDir(File resourcesDir) {
        this.resourcesDir = resourcesDir
    }

    AntBuilder getAnt() {
        if (ant == null) ant = new AntBuilder()
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
        pluginInfo = new AstPluginDescriptorReader().readPluginInfo(new FileSystemResource(descriptor))
        def pluginBaseDir = descriptor.parentFile
        def pluginProps = pluginInfo
        // Work out what the name of the plugin is from the name of the descriptor file.
        def pluginName = GrailsNameUtils.getPluginName(descriptor.name)

        // Remove the existing 'plugin.xml' if there is one.
        def pluginXml = new File(pluginBaseDir, "plugin.xml")
        pluginXml.delete()

        // Use MarkupBuilder with indenting to generate the file.
        def writer = new IndentPrinter(new PrintWriter(new FileWriter(pluginXml)))
        def generator = new PluginDescriptorGenerator(buildSettings, pluginName, resourceList)

        pluginProps["type"] = descriptor.name - '.groovy'
        generator.generatePluginXml(pluginProps, writer)

        return pluginProps
    }

    def packagePlugin(String pluginName, File classesDir, File targetDir) {
        if (!pluginInfo.packaging || pluginInfo.packaging == 'source') {
            return packageSource(pluginName, classesDir, targetDir)
        }
        return packageBinary(pluginName, classesDir, targetDir)
    }

    String packageSource(String pluginName, File classesDir, File targetDir) {
        def pluginProps = pluginInfo

        // Package plugin's zip distribution
        def pluginZip = "${basedir}/grails-${pluginName}-${pluginInfo.version}.zip"
        ant.delete(file:pluginZip)

        def stagingDir
        try {
           stagingDir = grailsSettings.pluginStagingDir
           ant.delete(dir: stagingDir, failonerror:true)
           ant.mkdir(dir:stagingDir)

           if (hasApplicationDependencies) {
               ant.copy(file: "${pluginInfo.pluginDir.file}/grails-app/conf/BuildConfig.groovy",
                        tofile: "$stagingDir/dependencies.groovy", failonerror:false)
           }

           runResourcesClosure stagingDir

           def (includesList, excludesList) = findIncludesAndExcludes(pluginProps)
           ant.copy(todir: stagingDir, preservelastmodified:true) {
               fileset(dir: basedir, includes: includesList.join(","), excludes: excludesList.join(","))
           }

           if (jarFiles) {
               ant.mkdir(dir: "$stagingDir/lib")
               for (File file in jarFiles) {
                   ant.copy(file: file, todir: "$stagingDir/lib", preservelastmodified: true)
               }
           }

           eventListener.triggerEvent 'CreatePluginArchiveStart', stagingDir

           ant.zip(destfile:pluginZip, filesonly:true) {
               fileset(dir: stagingDir)
           }

           eventListener.triggerEvent 'CreatePluginArchiveEnd', stagingDir
        }
        finally {
            if (stagingDir?.exists()) {
                ant.delete(dir: stagingDir, failonerror:true)
            }
        }

        return pluginZip
    }

    protected findIncludesAndExcludes(pluginProps) {
        def pluginExcludes = EXCLUDED_RESOURCES
        if (pluginProps?.pluginExcludes) {
            pluginExcludes.addAll(pluginInfo?.pluginExcludes)
        }

        [INCLUDED_RESOURCES, pluginExcludes]
    }

    private boolean matchesExcludes(excludes, path) {
        for(String exclude : excludes) {
            if (antPathMatcher.match(exclude, path)) {
                return true
            }
        }

        return false
    }

    private void extraExcludesProcessDirectory(File dir, int stripLength, int fullStripLength, excludes, List<String> ejectedFiles) {
        dir.listFiles().each { File f ->
            if (f.isFile()) {
                if (matchesExcludes(excludes, f.absolutePath.substring(stripLength+1))) {
                    ejectedFiles << f.absolutePath.substring(fullStripLength+1)
                }
            } else if (f.isDirectory() && !f.name.startsWith(".")) {
                extraExcludesProcessDirectory(f, stripLength, fullStripLength, excludes, ejectedFiles)
            }
        }
    }

    /**
     * We have to go through and build up a list of exclusions because the plugin excludes are based from to "basedir" and all of the excludes
     * when copying are based from the directory we are copying from (e.g. APP/web-app will not match web-app/** because the exclude doesn't include
     * web-app when determining that pattern). This makes all plugin excludes worthless when copy files unless we go through and manually match them
     * ourselves and return a list of specifically excluded files.
     *
     * @param basedir - the Grails Applications base directory
     * @param subdir - the sub directory we are copying from so we can prefix this for our pattern matching
     * @param excludes - the excludes from pluginExcludes
     * @param base - the ones that the Ant copy wants to include no matter what (originally included as specific exclude: lines)
     * @return returns a list of files to exclude relative to basedir/subdir
     */
    private List<String> extraExcludes(File basedir, String subdir, excludes, List<String> base = []) {
        if (!excludes) return base

        List<String> ejectedFiles = []

        File dir = new File(basedir, subdir)

        if (dir.exists()) {
            int stripLength = basedir.absolutePath.size() // e.g. [/blah/blah/]web-dir/ - preserve web-dir so we can match it
            int fullStripLength = stripLength + 1 + subdir.size() // e.g. /blah/blah/[web-dir/]

            extraExcludesProcessDirectory(dir, stripLength, fullStripLength, excludes, ejectedFiles)
        }

        ejectedFiles.addAll(base)

        return ejectedFiles
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
            File i18n = new File(resourcesDir, "grails-app/i18n")
            if (i18n.exists()) {
                copy(todir: "${metaInf}/grails-app/i18n", includeEmptyDirs:false,failonerror:false) {
                    fileset(dir: i18n) {
                        extraExcludes(basedir, "grails-app/i18n", pluginProps?.pluginExcludes).each {
                            exclude name: it
                        }
                    }
                }
            }

            // the excludes get more difficult now, as they are from the root of the project and these
            // excludes would be from the fileset's dir

            mkdir(dir:"${metaInf}/static")
            copy(todir:"${metaInf}/static", includeEmptyDirs:false, failonerror:false) {
                fileset(dir:"${basedir}/web-app") {
                    extraExcludes(basedir, "web-app",
                        pluginProps?.pluginExcludes, ["plugins/**", "**/WEB-INF/**", "**/META-INF/**",
                        "**/*.gsp", "**/*.jsp"]).each {
                            exclude name: it
                    }
                }
            }
            mkdir(dir:"${metaInf}/scripts")
            copy(todir:"${metaInf}/scripts") {
                fileset(dir:"${basedir}/scripts") {
                    extraExcludes(basedir, "scripts", pluginProps?.pluginExcludes, ["_Install.groovy","_Uninstall.groovy","_Upgrade.groovy"]).each {
                        exclude name: it
                    }
                }
            }
            mkdir(dir:"${classesDir}/src")
            copy(todir:"${classesDir}/src") {
                fileset(dir:"${basedir}/src") {
                    extraExcludes(basedir, "src", pluginProps?.pluginExcludes, ["groovy/**","java/**"]).each {
                        exclude name: it
                    }
                }
            }

            def stagingDir
            try {
               stagingDir = grailsSettings.pluginStagingDir
               delete(dir: stagingDir, failonerror:true)
               stagingDir.mkdirs()
               runResourcesClosure stagingDir

               copy(todir: stagingDir, preservelastmodified:true) {
                   fileset(dir:classesDir, excludes:excludeList.join(','))
               }

               if (grailsSettings.pluginIncludeSource) {
                   def (includesList, excludesList) = findIncludesAndExcludes(pluginProps)
                   excludesList << 'grails-app/i18n/**'
                   excludesList << 'lib/**'
                   excludesList << 'web-app/**'
                   excludesList << 'application.properties'
                   excludesList << 'plugin.xml'

                   // copy src to the root of the jar; do this in steps so the exclusions are correct
                   String tempSrcDir = "$stagingDir/TEMP_SRC"
                   mkdir(dir: tempSrcDir)
                   copy(todir: tempSrcDir, preservelastmodified:true, verbose: true) {
                       fileset(dir: basedir, includes: 'src/java/**', excludes: excludesList.join(","))
                       fileset(dir: basedir, includes: 'src/groovy/**', excludes: excludesList.join(","))
                   }
                   copy(todir: stagingDir, preservelastmodified:true) {
                       fileset(dir: "$tempSrcDir/src/java")
                       fileset(dir: "$tempSrcDir/src/groovy")
                   }
                   delete(dir: tempSrcDir, failonerror:true)

                   copy(todir: stagingDir, preservelastmodified:true) {
                       excludesList << 'src/**'
                       fileset(dir: basedir, includes: includesList.join(","), excludes: excludesList.join(","))
                   }
               }

               eventListener.triggerEvent 'CreatePluginArchiveStart', stagingDir

               jar(destfile: destinationFile, filesonly: true) {
                   fileset(dir: stagingDir)
                   manifest {
                       attribute name:"Implementation-Title", value:pluginProps.title
                       attribute name:"Implementation-Version", value:pluginProps.version
                   }
               }

               eventListener.triggerEvent 'CreatePluginArchiveEnd', stagingDir
            }
            finally {
                if (stagingDir?.exists()) {
                    ant.delete(dir: stagingDir, failonerror:true)
                }
            }
        }
        return destinationFile
    }

    protected void runResourcesClosure(File stagingDir) {
        ConfigObject buildConfig = grailsSettings.config
        if (buildConfig.grails.plugin.resources instanceof Closure) {
            Closure callable = buildConfig.grails.plugin.resources
            callable.delegate = ant
            callable.resolveStrategy = Closure.DELEGATE_FIRST
            callable(stagingDir)
        }
    }
}
