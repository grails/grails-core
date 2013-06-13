/*
 * Copyright 2011 the original author or authors.
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

aliasFile = new File(grailsSettings.userHome, '.grails/.aliases')

includeTargets << grailsScript("_GrailsArgParsing")
target(default: 'Configures aliases for grails commands') {
     def params = argsMap.params
     if (!params) {
         if (argsMap.list) {
             listAliases()
         } else if(argsMap.delete) {
             removeAlias()
         } else {
             println usage()
             exit 1
         }
         exit 0
     }
     if (params.size() == 1) {
         showAlias()
     } else {
         configureAlias()
     }
 }

def configureAlias() {
    def params = argsMap.params
    def aliases = loadProperties()
    def numberOfParams = params.size()
    def alias = params[0]
    if (aliases.containsKey(alias) && isInteractive) {
        def oldValue = aliases.get(alias)
        if (!confirmInput("An alias named ${alias} already exists.  The current value is ${oldValue}. Overwrite existing value? ")) {
            exit 0
        }
    }
    def value = params[1..-1].join(' ')
    aliases.put alias, value
    aliases.store(new FileWriter(aliasFile), null)
    println "Alias ${alias} with value ${value} configured"
}

def removeAlias() {
    def aliasToDelete = argsMap.delete
    if (aliasToDelete == Boolean.TRUE) {
        println usage()
        exit 1
    }
    def aliases = loadProperties()
    aliases.remove aliasToDelete
    aliases.store(new FileWriter(aliasFile), null)
    println "Alias ${aliasToDelete} removed"
}

def showAlias() {
    def aliasToShow = argsMap.params[0]
    def aliases = loadProperties()
    def value = aliases.get aliasToShow
    if (value) {
        println "${aliasToShow} = ${value}"
    } else {
        println "No alias configured for ${aliasToShow}"
    }
}

def listAliases() {
    def aliases = loadProperties()
    if (aliases) {
        aliases.each { k, v ->
            println "${k} = ${v}"
        }
    } else {
        println "No aliases configured"
    }
}

def loadProperties() {
    def aliases = new Properties()
    if (aliasFile.exists()) {
        aliases.load(new FileReader(aliasFile))
    }
    aliases
}

def usage() {
'''\
Usage:
    grails alias [--delete=alias] [--list] [alias [command]]

Examples:
    grails alias ra run-app
    grails alias rft test-app functional:
    grails alias --list
    grails alias rft
    grails alias --delete=ra
'''
}

