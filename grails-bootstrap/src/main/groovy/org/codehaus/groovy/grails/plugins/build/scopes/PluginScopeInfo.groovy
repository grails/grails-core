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
package org.codehaus.groovy.grails.plugins.build.scopes

import org.codehaus.groovy.grails.io.support.Resource
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo

/**
 * Encapsulates information about plugins contained within a particular scope.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class PluginScopeInfo {
    String scopeName

    PluginScopeInfo(String scopeName) {
        this.scopeName = scopeName
    }

    Set<Resource> sourceDirectories = []
    Set<GrailsPluginInfo> pluginInfos = []
    Set<String> pluginNames = []
    Set<Resource> pluginDescriptors = []
    Set<Resource> artefactResources = []

    PluginScopeInfo minus(PluginScopeInfo other) {
        PluginScopeInfo newInfo = new PluginScopeInfo()
        newInfo.sourceDirectories = this.sourceDirectories - other.sourceDirectories
        newInfo.pluginInfos = this.pluginInfos - other.pluginInfos
        newInfo.pluginNames = this.pluginNames - other.pluginNames
        newInfo.pluginDescriptors = this.pluginDescriptors - other.pluginDescriptors
        newInfo.artefactResources = this.artefactResources - other.artefactResources
        newInfo
    }
}
