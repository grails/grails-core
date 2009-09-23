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
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener

/**
 * Gant script containing the Grails build event system.
 *
 * @author Peter Ledbrook
 *
 * @since 1.1
 */

// No point doing this stuff more than once.
if (getBinding().variables.containsKey("_grails_events_called")) return
_grails_events_called = true

includeTargets << grailsScript("_GrailsClasspath")

// Class loader to use for loading events scripts.
eventsClassLoader = new GroovyClassLoader(classLoader)

// A map of events to lists of handlers. The handlers provided by plugin
// and application Events scripts are put in here.

eventListener = new GrailsBuildEventListener(eventsClassLoader, binding, grailsSettings)
eventListener.globalEventHooks = [
        StatusFinal: [ {message -> println message } ],
        StatusUpdate: [ {message -> println message + ' ...' } ],
        StatusError: [ {message -> System.err.println message } ],
        CreatedArtefact: [ {artefactType, artefactName -> println "Created $artefactType for $artefactName" } ]
]


hooksLoaded = false
binding.addBuildListener(eventListener)
// Set up the classpath for the event hooks.
classpath()

// Now load them.
eventListener.classLoader = new GroovyClassLoader(classLoader)
eventListener.initialize()

// Send a scripting event notification to any and all event hooks in plugins/user scripts
event = {String name, def args ->
    eventListener.triggerEvent(name, * args)
}

// Give scripts a chance to modify classpath
event('SetClasspath', [classLoader])


