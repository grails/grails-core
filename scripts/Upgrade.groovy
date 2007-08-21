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

 /**
 * Gant script that handles upgrading of a Grails applications
 * 
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 *
 * @since 0.4
 */    
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

Ant.property(environment:"env")       
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"   

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )

task( upgrade: "main upgrade task") {

	depends( createStructure )
   
    if (appGrailsVersion != grailsVersion) {
        def gv = appGrailsVersion == null ? "pre-0.5" : appGrailsVersion
        event("StatusUpdate", [ "NOTE: Your application currently expects grails version [$gv], "+
	        "this task will upgrade it to Grails ${grailsVersion}"])
    }

    Ant.input(message: """
	WARNING: This task will upgrade an older Grails application to ${grailsVersion}.
	However, tag libraries provided by earlier versions of Grails found in grails-app/taglib will be removed. 
	The task will not, however, delete tag libraries developed by yourself.
	Are you sure you want to continue? 
			   """,
			validargs:"y,n", 
			addproperty:"grails.upgrade.warning")

       def answer = Ant.antProject.properties."grails.upgrade.warning"        

	if(answer == "n") exit(0)
	
	def coreTaglibs = new File("${basedir}/plugins/core")

	Ant.delete(dir:"${coreTaglibs}", failonerror:false)

	
	Ant.sequential {    
		def testDir = "${basedir}/grails-tests"
		if(new File("${testDir}/CVS").exists()) {
			println """
WARNING: Your Grails tests directory '${testDir}' is currently under CVS control so the upgrade script cannot
move it to the new location of '${basedir}/test/integration'. Please move the directory using the relevant CVS commands."""
		}   
		else if(new File("${testDir}/.svn").exists()) {
						println """
WARNING: Your Grails tests directory '${testDir}' is currently under SVN control so the upgrade script cannot
move it to the new location of '${basedir}/test/integration'. Please move the directory using the relevant SVN commands."""			
		}   
		else {
			if(new File(testDir).exists()) {
				move(todir:"${basedir}/test/integration") {
					fileset(dir:testDir, includes:"**") 
				}                                       
				delete(dir:testDir)
			}      	   		
			
		}
        delete(dir:"${basedir}/tmp", failonerror:false)
		copy(todir:"${basedir}/web-app") {
			fileset(dir:"${grailsHome}/src/war") {
				include(name:"**/**")
				exclude(name:"WEB-INF/**")
                present(present:"srconly", targetdir:"${basedir}/web-app")
			} 
		}
		copy(file:"${grailsHome}/src/war/WEB-INF/sitemesh.xml",
			 tofile:"${basedir}/web-app/WEB-INF/sitemesh.xml", overwrite:true)
		copy(file:"${grailsHome}/src/war/WEB-INF/applicationContext.xml",
			 tofile:"${basedir}/web-app/WEB-INF/applicationContext.xml", overwrite:true)

		// These will still overwrite as the src name differs from target and the src name is not in target		                
		if(!new File("${basedir}/grails-app/conf/UrlMappings.groovy").exists()) {
	        copy(tofile:"${basedir}/grails-app/conf/UrlMappings.groovy") {
	            fileset(file:"${grailsHome}/src/grails/templates/artifacts/UrlMappings.groovy") {
	                present(present:"srconly", targetdir:"${basedir}/grails-app/conf")
	            }
	        }
		}
		if(!new File("${basedir}/grails-app/conf/Config.groovy").exists()) {
	        copy(tofile:"${basedir}/grails-app/conf/Config.groovy") {
	            fileset(file:"${grailsHome}/src/grails/grails-app/conf/Config.groovy") {
	                present(present:"srconly", targetdir:"${basedir}/grails-app/conf")
	            }
	        }
		}
		if(!new File("${basedir}/grails-app/conf/DataSource.groovy").exists()) {
	        copy(tofile:"${basedir}/grails-app/conf/DataSource.groovy") {
	            fileset(file:"${grailsHome}/src/grails/grails-app/conf/DataSource.groovy") {
	                present(present:"srconly", targetdir:"${basedir}/grails-app/conf")
	            }
	        }
		}
		

        if(new File("${basedir}/spring").exists()) {
            move(file:"${basedir}/spring", todir:"${basedir}/grails-app/conf")
        }
        if(new File("${basedir}/hibernate").exists()) {
            move(file:"${basedir}/hibernate", todir:"${basedir}/grails-app/conf")            
        }

	    def appKey = baseName.replaceAll( /\s/, '.' ).toLowerCase()

		replace(dir:"${basedir}/web-app/WEB-INF", includes:"**/*.*",
		    		token:"@grails.project.key@", value:"${appKey}" )				


		copy(todir:"${basedir}/web-app/WEB-INF/tld", overwrite:true) {
			fileset(dir:"${grailsHome}/src/war/WEB-INF/tld/${servletVersion}")	
			fileset(dir:"${grailsHome}/src/war/WEB-INF/tld", includes:"spring.tld")
			fileset(dir:"${grailsHome}/src/war/WEB-INF/tld", includes:"grails.tld")			
		}	 
		touch(file:"${basedir}/grails-app/i18n/messages.properties") 

        propertyfile(file:"${basedir}/application.properties",
            comment:"Do not edit app.grails.* properties, they may change automatically. "+
                "DO NOT put application configuration in here, it is not the right place!") {
            entry(key:"app.name", value:"$grailsAppName")
            entry(key:"app.grails.version", value:"$grailsVersion")
        }

        replaceregexp(match:"^.*GRAILS_HOME.*\$", replace:"", flags:"gm") {
            fileset(dir:"${basedir}", includes:".classpath")
        }
        replace(dir:"${basedir}",
                includes:".classpath",
                token:"</classpath>",
                value:"<classpathentry kind=\"var\" path=\"GRAILS_HOME/ant/lib/ant.jar\"/>\n${getGrailsLibs()}${getGrailsJar()}\n</classpath>")
        replaceregexp(match:"^\\s*", replace:"", flags:"gm") {
            fileset(dir:"${basedir}", includes:".classpath")
        }

    }

    // proceed plugin-specific upgrade logic contained in 'scripts/_Upgrade.groovy' under plugin's root
    def plugins = new File("${basedir}/plugins/")
	if(plugins.exists()) {
	    plugins.eachFile { f ->
	        if(f.isDirectory() && f.name != 'core' ) {
	            // fix for Windows-style path with backslashes

	            def pluginBase = "${basedir}/plugins/${f.name}".toString().replaceAll("\\\\","/")
	            // proceed _Upgrade.groovy plugin script if exists
	            def upgradeScript = new File ( "${pluginBase}/scripts/_Upgrade.groovy" )        
	            if( upgradeScript.exists() ) {
	                event("StatusUpdate", [ "Executing ${f.name} plugin upgrade script"])
	                // instrumenting plugin scripts adding 'pluginBasedir' variable
	                def instrumentedUpgradeScript = "def pluginBasedir = '${pluginBase}'\n" + upgradeScript.text
	                // we are using text form of script here to prevent Gant caching
	                includeTargets << instrumentedUpgradeScript
	            }
	        }
	    }
		
	}

    event("StatusUpdate", [ "Please make sure you view the README for important information about changes to your source code."])

    event("StatusFinal", [ "Project upgraded"])
}

task("default": "Upgrades a Grails application from a previous version of Grails") {
	depends( upgrade )
}  

