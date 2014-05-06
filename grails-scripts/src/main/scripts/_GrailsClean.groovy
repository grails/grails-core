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
 * Gant script that cleans a Grails project
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

// No point doing this stuff more than once.
if (getBinding().variables.containsKey("_grails_clean_called")) return
_grails_clean_called = true

includeTargets << grailsScript("_GrailsEvents")

import org.codehaus.groovy.grails.project.creation.*

projectCleaner = new GrailsProjectCleaner(grailsSettings, eventListener)
projectCleaner.ant = ant

target (cleanAll: "Cleans a Grails project") {
    projectCleaner.cleanAll(false)
}

target (cleanWork: "Cleans a Grails project") {
    projectCleaner.cleanAll(false)
    projectCleaner.cleanWork()
}

target (clean: "Implementation of clean") {
    projectCleaner.clean(false)
}

target (cleanCompiledSources: "Cleans compiled Java and Groovy sources") {
    projectCleaner.cleanCompiledSources(false)
}

target (cleanTestReports: "Cleans the test reports") {
    projectCleaner.cleanTestReports(false)
}

target (cleanWarFile: "Cleans the deployable .war file") {
    projectCleaner.cleanWarFile(false)
}
