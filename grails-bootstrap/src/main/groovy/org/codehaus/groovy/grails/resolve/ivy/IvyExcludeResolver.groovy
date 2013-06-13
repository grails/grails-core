/*
 * Copyright 2012 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.ivy

import groovy.transform.CompileStatic
import org.apache.ivy.core.event.EventManager
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.core.resolve.IvyNode
import org.apache.ivy.core.resolve.ResolveEngine
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.resolve.ResolvedModuleRevision
import org.apache.ivy.core.sort.SortEngine
import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor
import org.codehaus.groovy.grails.resolve.ExcludeResolver
import org.codehaus.groovy.grails.resolve.IvyDependencyManager

/**
 *
 * An exclude resolver for Ivy
 *
 * @since 2.3
 * @author Graeme Rocher
 */
@CompileStatic
class IvyExcludeResolver implements ExcludeResolver {

    IvyDependencyManager dependencyManager

    IvyExcludeResolver(IvyDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager
    }

    @Override
    Map<Dependency, List<Dependency>> resolveExcludes() {
        def results = [:]
        def applicationDescriptors = dependencyManager.getApplicationDependencyDescriptors()

        def eventManager = new EventManager()
        def sortEngine = new SortEngine(dependencyManager.ivySettings)
        def resolveEngine = new ResolveEngine(dependencyManager.ivySettings, eventManager,sortEngine)
        resolveEngine.dictatorResolver = dependencyManager.chainResolver

        DefaultModuleDescriptor md = (DefaultModuleDescriptor)dependencyManager.createModuleDescriptor()
        def directModulesId = []
        for (d in applicationDescriptors) {
            EnhancedDefaultDependencyDescriptor ed = (EnhancedDefaultDependencyDescriptor)d
            // the dependency without any exclude/transitive definitions
            directModulesId << d.dependencyId
            def newDescriptor = new DefaultDependencyDescriptor(d.getDependencyRevisionId(), false)
            newDescriptor.addDependencyConfiguration(ed.scope, "default")
            md.addDependency newDescriptor
        }

        if (directModulesId) {
            def options = new ResolveOptions()
            options.setDownload(false)
            options.setOutputReport(false)
            options.setValidate(false)
            def report = resolveEngine.resolve(md, options)
            for (d in report.dependencies) {
                IvyNode dep = (IvyNode)d
                def dependencyModuleId = dep.moduleId
                if (!directModulesId.contains(dependencyModuleId)) {
                    continue
                }

                def depDescriptor = dep.descriptor
                def transitiveDepList = []
                final moduleRevision = dep.moduleRevision
                if (moduleRevision == null) {
                    continue
                }

                final mrid = moduleRevision.id
                results[new Dependency(mrid.organisation,mrid.name,mrid.revision)] = transitiveDepList
                for (transitive in depDescriptor.dependencies) {
                    def tdid = transitive.dependencyId
                    if (tdid instanceof ResolvedModuleRevision) {
                        final id = tdid.getId()
                        transitiveDepList << new Dependency(id.organisation,id.name,id.revision)
                    }
                    else {
                        transitiveDepList << new Dependency(tdid.organisation,tdid.name, "*")
                    }
                }
            }
        }

        return results
    }
}
