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

package org.codehaus.groovy.grails.resolve.maven.aether.support

import groovy.transform.CompileStatic
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import static org.eclipse.aether.util.artifact.JavaScopes.*

/**
 * The default {@link org.eclipse.aether.util.graph.transformer.JavaScopeSelector} just takes whatever is the first top-level dependency as the valid scope without comparing the effective scope of multiple.
 *
 * This class fixes that
 *
 * @author Graeme Rocher
 * @since 2.3.3
 */
@CompileStatic
class MultipleTopLevelJavaScopeSelector extends ConflictResolver.ScopeSelector{
    @Override
    void selectScope(ConflictResolver.ConflictContext context) {
        String scope = context.winner.dependency.scope
        if ( !SYSTEM.equals( scope ) )
        {
            scope = chooseEffectiveScope( context.items )
        }
        context.scope = scope
    }

    private String chooseEffectiveScope( Collection<ConflictResolver.ConflictItem> items )
    {
        Set<String> scopes = [] as HashSet
        Set<String> topLevelScopes = [] as HashSet
        for ( ConflictResolver.ConflictItem item : items )
        {
            if ( item.depth <= 1 )
            {
                topLevelScopes << item.dependency.scope
            }
            else {
                scopes.addAll item.scopes
            }
        }
        if(topLevelScopes) {
            chooseEffectiveScope topLevelScopes
        }
        else {
            chooseEffectiveScope scopes
        }
    }

    private String chooseEffectiveScope( Set<String> scopes )
    {
        if ( scopes.size() > 1 )
        {
            scopes.remove SYSTEM
        }

        String effectiveScope = ""

        if ( scopes.size() == 1 )
        {
            effectiveScope = scopes.iterator().next()
        }
        else if ( scopes.contains( COMPILE ) )
        {
            effectiveScope = COMPILE
        }
        else if ( scopes.contains( RUNTIME ) )
        {
            effectiveScope = RUNTIME
        }
        else if ( scopes.contains( PROVIDED ) )
        {
            effectiveScope = PROVIDED
        }
        else if ( scopes.contains( TEST ) )
        {
            effectiveScope = TEST
        }

        return effectiveScope
    }
}
