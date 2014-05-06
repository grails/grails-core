/*
 * Copyright 2014 the original author or authors.
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
 * Gant script that manages the Grails version
 *
 * @since 2.4
 */

includeTargets << grailsScript("_GrailsEvents")

target ('default': "Sets the Grails version") {
    def params = argsMap.params
    def newVersion = params ? params[0] : grailsVersion

    metadata.'app.grails.version' = newVersion
    metadata.persist()
    event("StatusFinal", [ "Grails version updated to $newVersion"])
}

USAGE = """
    set-grails-version [NUMBER]

where
    NUMBER     = The number to set the Grails version to.  If no argument is supplied then the version of Grails which is being used to run the command will be used.
"""