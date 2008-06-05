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

import org.codehaus.groovy.grails.compiler.support.*

/**
 * Gant script that creates a WAR file from a Grails project
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    
                              

includeTargets << new File ( "${grailsHome}/scripts/Clean.groovy" ) 
includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

target ('default':'''Creates a WAR archive for deployment onto a Java EE application server.

Examples: 
grails war
grails prod war
''') {
    depends( checkVersion)

	war()
} 

generateLog4jFile = true

warName = null

DEFAULT_DEPS = [
    "ant.jar",
    "ant-launcher.jar",
    "hibernate3.jar",
    "jdbc2_0-stdext.jar",
    "jta.jar",
    "groovy-all-*.jar",
    "standard-${servletVersion}.jar",
    "jstl-${servletVersion}.jar",
    "antlr-*.jar",
    "cglib-*.jar",
    "dom4j-*.jar",
    "oscache-*.jar",
    "ehcache-*.jar",
    "junit-*.jar",
    "commons-logging-*.jar",
    "sitemesh-*.jar",
    "spring-*.jar",
    "log4j-*.jar",
    "ognl-*.jar",
    "hsqldb-*.jar",
    "commons-lang-*.jar",
    "commons-collections-*.jar",
    "commons-beanutils-*.jar",
    "commons-pool-*.jar",
    "commons-dbcp-*.jar",
    "commons-cli-*.jar",
    "commons-validator-*.jar",
    "commons-fileupload-*.jar",
    "commons-io-*.jar",
    "*oro-*.jar",
    "jaxen-*.jar",
    "xercesImpl.jar",
    "xstream-1.2.1.jar",
    "xpp3_min-1.1.3.4.O.jar"
]

DEFAULT_J5_DEPS = [
    "hibernate-annotations.jar",
    "hibernate-commons-annotations.jar",
    "ejb3-persistence.jar",
]

target (configureRunningScript:"Sets the currently running script, in case called directly") {
    System.setProperty('current.gant.script',"war")
}
target(startLogging:"Bootstraps logging") {
  // do nothing, overrides default behaviour so that logging doesn't kick in    
}

target (war: "The implementation target") {
	depends( configureRunningScript, clean,  packageApp)
	 
	try {
        stagingDir = "${basedir}/staging"		

        if(config.grails.war.destFile || args) {
            // Pick up the name of the WAR to create from the command-line
            // argument or the 'grails.war.destFile' configuration option.
            // The command-line argument takes precedence.
            warName = args ? args.trim() : config.grails.war.destFile

            // Find out whether WAR name is an absolute file path or a
            // relative one.
            def warFile = new File(warName)
            if(!warFile.absolute) {
                // It's a relative path, so adjust it for 'basedir'.
                warFile = new File(basedir, warFile.path)
                warName = warFile.canonicalPath
            }

            String parentDir = warFile.parentFile.absolutePath
            stagingDir = "${parentDir}/staging"
        }		
        else {
            def fileName = grailsAppName	
            def version = Ant.antProject.properties.'app.version'
            if(version) {
                version = '-'+version
            }
            else {
                version = ''
            }
            warName = "${basedir}/${fileName}${version}.war"
        }
        Ant.mkdir(dir:stagingDir)

		Ant.copy(todir:stagingDir, overwrite:true) {
            // Allow the application to override the step that copies
            // 'web-app' to the staging directory.
            if(config.grails.war.copyToWebApp instanceof Closure) {
				def callable = config.grails.war.copyToWebApp
				callable.delegate = delegate
				callable.resolveStrategy = Closure.DELEGATE_FIRST
				callable(args)
			}
			else {
			    fileset(dir:"${basedir}/web-app", includes:"**")
            }
		}       
		Ant.copy(todir:"${stagingDir}/WEB-INF/grails-app", overwrite:true) {
			fileset(dir:"${basedir}/grails-app", includes:"views/**")
            fileset(dir:"${resourcesDirPath}/grails-app", includes:"i18n/**")
        }
		Ant.copy(todir:"${stagingDir}/WEB-INF/classes") {
            fileset(dir:classesDirPath) {
				exclude(name:"hibernate")
				exclude(name:"spring")	
				exclude(name:"hibernate/*")
				exclude(name:"spring/*")
			}
        }

        Ant.mkdir(dir:"${stagingDir}/WEB-INF/spring")
        Ant.copy(todir:"${stagingDir}/WEB-INF/spring") {
            fileset(dir:"${basedir}/grails-app/conf/spring", includes:"**/*.xml")            
        }

        Ant.copy(todir:"${stagingDir}/WEB-INF/classes", failonerror:false) {
            fileset(dir:"${basedir}/grails-app/conf") {
				exclude(name:"*.groovy")
				exclude(name:"log4j.*")
				exclude(name:"**/hibernate/**")
				exclude(name:"**/spring/**")
			}
            fileset(dir:"${basedir}/grails-app/conf/hibernate", includes:"**/**")
            fileset(dir:"${basedir}/src/java") {
                include(name:"**/**")
                exclude(name:"**/*.java")
            }
            fileset(dir:"${resourcesDirPath}", includes:"log4j.properties")
        }
		              
		scaffoldDir = "${stagingDir}/WEB-INF/templates/scaffolding"
		packageTemplates()

		Ant.copy(todir:"${stagingDir}/WEB-INF/lib") {
			fileset(dir:"${grailsHome}/dist") {
					include(name:"grails-*.jar")
			}
			fileset(dir:"${basedir}/lib") {
					include(name:"*.jar")
			}
			if(config.grails.war.dependencies instanceof Closure) {
				def fileset = config.grails.war.dependencies
				fileset.delegate = delegate
				fileset.resolveStrategy = Closure.DELEGATE_FIRST
				fileset()
			}
			else {
	            fileset(dir:"${grailsHome}/lib") {
                    def deps = config.grails.war.dependencies ?: DEFAULT_DEPS
                    for(d in deps) {
	                    include(name:d)
	                }
	                if(antProject.properties."ant.java.version" != "1.4") {
                        deps = config.grails.war.java5.dependencies ?: DEFAULT_J5_DEPS
                        for(d in deps) {
	                        include(name:d)
	                    }
	                }
	            }
	            if(antProject.properties."ant.java.version" == "1.4") {
	                fileset(dir:"${grailsHome}/lib/endorsed") {
	                        include(name:"*.jar")
	                }
	            }				
			}
		}
        Ant.copy(file:webXmlFile.absolutePath, tofile:"${stagingDir}/WEB-INF/web.xml")
        Ant.delete(file:webXmlFile)
        Ant.copy(todir:"${stagingDir}/WEB-INF/lib", flatten:true, failonerror:false) {
			fileset(dir:"${basedir}/plugins") {
                include(name:"*/lib/*.jar")
            }
        }
		  
	    Ant.propertyfile(file:"${stagingDir}/WEB-INF/classes/application.properties") {
	        entry(key:"grails.env", value:grailsEnv)
	        entry(key:"grails.war.deployed", value:"true")
	    }		
		
		Ant.replace(file:"${stagingDir}/WEB-INF/applicationContext.xml",
				token:"classpath*:", value:"" )
				
	    if(config.grails.war.resources instanceof Closure) {
			Closure callable = config.grails.war.resources
			callable.delegate = Ant
			callable.resolveStrategy = Closure.DELEGATE_FIRST

            if(callable.maximumNumberOfParameters == 1) {
                callable(stagingDir)
            }
            else {
                callable(stagingDir, args)
            }
        }

		warPlugins()
		createDescriptor()
    	event("WarStart", [warName])
		Ant.jar(destfile:warName, basedir:stagingDir)
    	event("WarEnd", [warName])
	}   
	finally {
		cleanUpAfterWar()
	}
    event("StatusFinal", ["Done creating WAR ${warName}"])
}                                                                    
  
target(createDescriptor:"Creates the WEB-INF/grails.xml file used to load Grails classes in WAR mode") {
     def resourceList = GrailsResourceLoaderHolder.resourceLoader.getResources()
     def pluginList = resolveResources("file:${basedir}/plugins/*/*GrailsPlugin.groovy")
      
	 new File("${stagingDir}/WEB-INF/grails.xml").withWriter { writer ->
		def xml = new groovy.xml.MarkupBuilder(writer)
		xml.grails {
			resources {
			    for(r in resourceList) {
                    def matcher = r.URL.toString() =~ artefactPattern

                    // Replace the slashes in the capture group with '.' so
                    // that we get a qualified class name. So for example,
                    // the file:
                    //
                    //    grails-app/domain/org/example/MyFilters.groovy
                    //
                    // will result in a capturing group of:
                    //
                    //    org/example/MyFilters
                    //
                    // which the following step will convert to:
                    //
                    //    org.example.MyFilters
                    //
                    def name = matcher[0][1].replaceAll('/', /\./)
                    if(name == 'spring.resources') resource("resources")
                    else resource(name)
			    }
			}
			plugins {
                for(p in pluginList) {

                    def name = p.file.name - '.groovy'
                    plugin(name)
                }
            }
		}
	 }
	 
}

target(cleanUpAfterWar:"Cleans up after performing a WAR") {
	Ant.delete(dir:"${stagingDir}", failonerror:true)
}

target(warPlugins:"Includes the plugins in the WAR") {
	Ant.sequential {
		mkdir(dir:"${stagingDir}/WEB-INF/plugins")
        copy(todir:"${stagingDir}/WEB-INF/plugins", failonerror:false) {
            fileset(dir:"${basedir}/plugins")  {
                include(name:"**/*plugin.xml")
            }
        }
        
        pluginResources += resolveResources("file:${basedir}/plugins/*/*GrailsPlugin.groovy").toList()
        for(p in pluginResources) {
           def pluginBase = p.file.parentFile.canonicalFile
           def pluginPath = pluginBase.absolutePath
           def pluginName = pluginBase.name
           def pluginNameWithVersion = pluginBase.name

           if(new File("${pluginBase}/grails-app/views").exists()) {
               def pluginViews = "${stagingDir}/WEB-INF/plugins/${pluginNameWithVersion}/grails-app/views"
               mkdir(dir:pluginViews)
               copy(todir:pluginViews, failonerror:false) {
                  fileset(dir:"${pluginBase}/grails-app/views", includes:"**")
               }
           }

        }
    }
}

