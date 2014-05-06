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
import org.codehaus.groovy.grails.cli.logging.*

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

// A map of events to lists of handlers. The handlers provided by plugin
// and application Events scripts are put in here.

eventListener.globalEventHooks = [
    StatusFinal: [ {message -> grailsConsole.addStatus message } ],
    StatusUpdate: [ {message -> grailsConsole.updateStatus message } ],
    StatusError: [ {message -> grailsConsole.error message } ],
    CreatedFile: [ {file -> grailsConsole.addStatus "Created file ${makeRelative(file)}" } ]
]

hooksLoaded = false
// Set up the classpath for the event hooks.
classpath()

// Now load them.
eventListener.initialize()

long eventInitStart = System.currentTimeMillis()

// Send a scripting event notification to any and all event hooks in plugins/user scripts
event = {String name, args ->
    try {
        boolean logTiming = binding.variables.containsKey('buildSettings') ? buildSettings.logScriptTiming : false
        if (logTiming) {
            grailsConsole.addStatus "#### (${System.currentTimeMillis() - eventInitStart}) $name $args"
            grailsConsole.lastMessage = ''
        }
        eventListener.triggerEvent(name, * args)
    }
    catch(e) {
        grailsConsole.error "Exception occurred trigger event [$name]: ${e.message}", e
    }
}

// Give scripts a chance to modify classpath
event('SetClasspath', [classLoader])
