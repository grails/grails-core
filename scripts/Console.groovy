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
 * Gant script that loads the Grails console
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import groovy.text.SimpleTemplateEngine  
import org.codehaus.groovy.grails.support.*

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/Bootstrap.groovy" )

task ('default': "Load the Grails interactive Swing console") {
	depends( checkVersion, configureProxy, packageApp, classpath, packagePlugins )
	console()
}            

task(console:"The console implementation task") { 
     
	rootLoader.addURL(classesDir.toURL())
	loadApp()
	configureApp()
	def b = new Binding()
	b.ctx = appCtx
	b.grailsApplication = grailsApp
	def c = new groovy.ui.Console(grailsApp.classLoader, b)
	c.beforeExecution = {
		appCtx.getBeansOfType(PersistenceContextInterceptor).each { k,v ->
			v.init()
		}
	}           
	c.afterExecution = {
		appCtx.getBeansOfType(PersistenceContextInterceptor).each { k,v ->
			v.flush()
			v.destroy()
		}
	}       
	c.run()
}
