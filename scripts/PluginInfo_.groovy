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

import grails.util.BuildSettings
import org.codehaus.groovy.grails.resolve.PluginResolveEngine

/**
 * Gant script that displays info about a given plugin
 *
 * @author Sergey Nebolsin
 *
 * @since 0.5.5
 */

includeTargets << grailsScript("_GrailsPlugins")

def displayPluginInfo = { pluginName, version ->

    BuildSettings settings = grailsSettings
    def pluginResolveEngine = new PluginResolveEngine(settings.dependencyManager, settings)
    pluginXml = pluginResolveEngine.renderPluginInfo(pluginName, version, System.out)
}


target(pluginInfo:"Implementation target") {
    depends(parseArguments)

    if (argsMap.params) {
        def pluginName = argsMap.params[0]
        def version = argsMap.params.size() > 1 ? argsMap.params[1] : null

        displayPluginInfo(pluginName, version)
    }
    else {
        event("StatusError", ["Usage: grails plugin-info <plugin-name> [version]"])
    }
}

setDefaultTarget("pluginInfo")
