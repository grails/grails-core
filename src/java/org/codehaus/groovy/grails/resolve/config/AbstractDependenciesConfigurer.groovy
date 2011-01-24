/* Copyright 2011 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.config

import grails.util.DslUtils
import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor

abstract class AbstractDependenciesConfigurer extends AbstractDependencyManagementConfigurer {

    final boolean pluginMode
    
    AbstractDependenciesConfigurer(DependencyConfigurationContext context, boolean pluginMode = false) {
        super(context)
        this.pluginMode = pluginMode
    }
    
    def invokeMethod(String name, args) {
        println "invokeMethod($name, $args)"
        if (!args || !((args[0] instanceof CharSequence)||(args[0] instanceof Map)||(args[0] instanceof Collection))) {
            println "WARNING: Configurational method [$name] in grails-app/conf/BuildConfig.groovy doesn't exist. Ignoring.."
        }
        else {
            def dependencies = args
            def callable
            if (dependencies && (dependencies[-1] instanceof Closure)) {
                callable = dependencies[-1]
                dependencies = dependencies[0..-2]
            }

            if (dependencies) {
                parseDependenciesInternal(dependencies, name, callable)
            }
        }
    }

    private parseDependenciesInternal(dependencies, String scope, Closure dependencyConfigurer) {
        boolean usedArgs = false

        def parseDep = { dependency ->
            if ((dependency instanceof CharSequence)) {
                def args = [:]
                if (dependencies[-1] instanceof Map) {
                    args = dependencies[-1]
                    usedArgs = true
                }
                def depDefinition = dependency.toString()

                def m = depDefinition =~ /([a-zA-Z0-9\-\/\._+=]*?):([a-zA-Z0-9\-\/\._+=]+?):([a-zA-Z0-9\-\/\._+=]+)/

                if (m.matches()) {

                    String name = m[0][2]
                    boolean isExcluded = context.pluginName ? dependencyManager.isExcludedFromPlugin(context.pluginName, name) : dependencyManager.isExcluded(name)
                    if (!isExcluded) {
                        def group = m[0][1]
                        def version = m[0][3]
                        if (pluginMode) {
                            group = group ?: 'org.grails.plugins'
                        }

                        def mrid = ModuleRevisionId.newInstance(group, name, version)

                        def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, DslUtils.getBooleanValueOrDefault(args, 'transitive', true), scope)

                        if (!pluginMode) {
                            dependencyManager.addDependency mrid
                        }
                        else {
                            def artifact = new DefaultDependencyArtifactDescriptor(dependencyDescriptor, name, "zip", "zip", null, null )
                            dependencyDescriptor.addDependencyArtifact(scope, artifact)
                        }
                        dependencyDescriptor.exported = DslUtils.getBooleanValueOrDefault(args, 'export', true)
                        dependencyDescriptor.inherited = context.inherited || dependencyManager.inheritsAll || context.pluginName

                        if (context.pluginName) {
                            dependencyDescriptor.plugin = context.pluginName
                        }
                        
                        dependencyManager.configureDependencyDescriptor(dependencyDescriptor, scope, dependencyConfigurer, pluginMode)
                    }
                }
                else {
                    println "WARNING: Specified dependency definition ${scope}(${depDefinition.inspect()}) is invalid! Skipping.."
                }
            }
            else if (dependency instanceof Map) {
                def name = dependency.name
                def group = dependency.group
                def version = dependency.version
                
                if (!group && pluginMode) group = "org.grails.plugins"

                if (group && name && version) {
                    boolean isExcluded = context.pluginName ? dependencyManager.isExcludedFromPlugin(context.pluginName, name) : dependencyManager.isExcluded(name)
                    if (!isExcluded) {
                        def attrs = [:]
                        if(dependency.classifier) {
                            attrs["m:classifier"] = dependency.classifier
                        }

                        def mrid
                        if (dependency.branch) {
                            mrid = ModuleRevisionId.newInstance(group, name, dependency.branch, version, attrs)
                        }
                        else {
                            mrid = ModuleRevisionId.newInstance(group, name, version, attrs)
                        }

                        def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, DslUtils.getBooleanValueOrDefault(dependency, 'transitive', true), scope)
                        if (!pluginMode) {
                            dependencyManager.addDependency mrid
                        }
                        else {
                            def artifact
                            if (dependency.classifier == 'plugin') {
                                artifact = new DefaultDependencyArtifactDescriptor(dependencyDescriptor, name, "xml", "xml", null, null )
                            }
                            else {
                                artifact = new DefaultDependencyArtifactDescriptor(dependencyDescriptor, name, "zip", "zip", null, null )
                            }

                            dependencyDescriptor.addDependencyArtifact(scope, artifact)
                        }

                        dependencyDescriptor.exported = DslUtils.getBooleanValueOrDefault(dependency, 'export', true)
                        dependencyDescriptor.inherited = context.inherited || dependencyManager.inheritsAll
                        if (context.pluginName) {
                            dependencyDescriptor.plugin = context.pluginName
                        }

                        dependencyManager.configureDependencyDescriptor(dependencyDescriptor, scope, dependencyConfigurer, pluginMode)
                    }
                }
            }
        }

        
        for(dep in dependencies ) {
            if ((dependencies[-1] == dep) && usedArgs) return 
            parseDep(dep) 
        }
    }
    
}