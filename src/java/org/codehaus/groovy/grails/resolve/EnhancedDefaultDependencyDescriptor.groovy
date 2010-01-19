/* Copyright 2004-2005 the original author or authors.
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

package org.codehaus.groovy.grails.resolve

import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.module.id.ArtifactId
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.plugins.matcher.ExactPatternMatcher
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import java.lang.reflect.Field

/**
 * Adds new methods to make access to this class Groovier
 *
 * @author Graeme Rocher
 * @since 1.2
 */

public class EnhancedDefaultDependencyDescriptor extends DefaultDependencyDescriptor{

    static final String WILDCARD = '*'


    /**
     * Configuration scope of the plugin 'runtime', 'build', 'test' etc.
     */
    String scope
    /**
     * Plugin that the dependency relates to, null if it is a framework or application dependency
     */
    String plugin
    /**
     * Whether the dependency is inherted from a plugin or framework and not an application dependency
     */
    boolean inherited

    /**
     * Whether a plugin dependencies is 'exported' to the application or not 
     */
    boolean exported = true

    /**
     * Whether the dependency should be exposed to the application
     */
    boolean isExportedToApplication() {
        if(plugin && !exported) return false
        return true
    }


    EnhancedDefaultDependencyDescriptor(ModuleRevisionId mrid, boolean force, String scope) {
        super(mrid, force);
        this.scope = scope
    }

    EnhancedDefaultDependencyDescriptor(ModuleRevisionId mrid, boolean force, boolean transitive, String scope) {
        this(mrid, force, scope);        
        setTransitive(transitive) 
    }

    void setExport(boolean b) {
        this.exported = b
    }

    void excludes(Object... args) {
        for(arg in args) {
            exclude(arg)            
        }
    }

    void exclude(def exclude) {
        if (exclude instanceof String) {
            excludeForString(exclude)
        }
        else if (exclude instanceof Map) {
            excludeForMap(exclude)
        }
    }

    private excludeForString (String dep) {
        def mid = ModuleId.newInstance(WILDCARD, dep)
        addRuleForModuleId(mid, scope)
    }


    private excludeForMap (Map args) {
        def mid = ModuleId.newInstance(args?.group ?: WILDCARD, args?.name ?: WILDCARD)
        addRuleForModuleId(mid, scope)
    }
    
    void dependencyConfiguration(String config){
    	addDependencyConfiguration(scope, config)
    }

    void setTransitive (boolean b) {
        // nasty hack since the isTransitive Ivy field is not public
        Field field = getClass().getSuperclass().getDeclaredField("isTransitive")
        field.accessible = true
        field.set(this, b)         
    }


    void addRuleForModuleId(ModuleId mid, String scope) {
        def id = new ArtifactId(mid, WILDCARD, WILDCARD, WILDCARD)
        
        def rule = new DefaultExcludeRule(id, ExactPatternMatcher.INSTANCE, null)
        addExcludeRule scope, rule
    }

}
