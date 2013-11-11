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
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import org.eclipse.aether.version.VersionConstraint
import org.eclipse.aether.version.Version
import org.eclipse.aether.collection.UnsolvableVersionConflictException
import org.eclipse.aether.graph.DependencyFilter
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor
import org.eclipse.aether.util.artifact.JavaScopes

/**
 * Aether's default {@link org.eclipse.aether.util.graph.transformer.NearestVersionSelector} does not take into account differing scopes when picking a winner
 *
 * @author Graeme Rocher
 * @since 2.3.3
 */
@CompileStatic
class ScopeAwareNearestVersionSelector extends ConflictResolver.VersionSelector{
    @Override
    void selectVersion( ConflictResolver.ConflictContext context )
    {
        def group = new ConflictGroup()
        for ( ConflictResolver.ConflictItem item in context.items )
        {
            DependencyNode node = item.node
            VersionConstraint constraint = node.versionConstraint

            boolean shouldBacktrack = false
            boolean hardConstraint = constraint.range != null

            if ( hardConstraint )
            {
                if ( group.constraints.add( constraint ) )
                {
                    if ( group.winner && !constraint.containsVersion( group.winner.node.version ) )
                    {
                        shouldBacktrack = true
                    }
                }
            }

            if ( isAcceptable( group, node.version ) )
            {
                group.candidates << item

                final currentWinner = group.winner
                if ( shouldBacktrack )
                {
                    backtrack( group, context )
                }
                else if ( currentWinner == null || isNearer( item, currentWinner) || hasMoreSpecificScope(item, currentWinner))
                {
                    group.winner = item
                }
            }
            else if ( shouldBacktrack )
            {
                backtrack( group, context )
            }
        }
        context.winner =  group.winner
    }

    private void backtrack( ConflictGroup group, ConflictResolver.ConflictContext context )
    {
        group.winner = null

        def it = group.candidates.iterator()
        while ( it.hasNext() )
        {
            ConflictResolver.ConflictItem candidate = it.next()

            if ( !isAcceptable( group, candidate.node.version ) )
            {
                it.remove()
            }
            else if ( !group.winner || isNearer( candidate, group.winner ) )
            {
                group.winner = candidate
            }
        }

        if ( group.winner == null )
        {
            throw newFailure( context )
        }
    }

    private boolean isAcceptable( ConflictGroup group, Version version )
    {
        for ( VersionConstraint constraint in group.constraints )
            if ( !constraint.containsVersion( version ) )
                return false
        return true
    }

    private boolean hasMoreSpecificScope(ConflictResolver.ConflictItem item1, ConflictResolver.ConflictItem item2) {
        return (item1.depth <= 1 && item2.depth <= 1) && new ScopeComparator().compare(item1.dependency.scope, item2.dependency.scope) > 0
    }
    private boolean isNearer( ConflictResolver.ConflictItem item1, ConflictResolver.ConflictItem item2 )
    {
        if ( item1.isSibling( item2 ) )
        {
            return (item1.node.version <=> item2.node.version)  > 0
        }
        else
        {
            return item1.depth < item2.depth
        }
    }

    private UnsolvableVersionConflictException newFailure( final ConflictResolver.ConflictContext context )
    {
        def visitor = new PathRecordingDependencyVisitor( { DependencyNode node, List<DependencyNode> parents -> context.isIncluded(node) } as DependencyFilter)
        context.root.accept( visitor )
        new UnsolvableVersionConflictException( visitor.paths )
    }


    @CompileStatic
    static final class ConflictGroup
    {

        final Collection<VersionConstraint> constraints = [] as HashSet<VersionConstraint>

        final Collection<ConflictResolver.ConflictItem> candidates = [] as ArrayList<ConflictResolver.ConflictItem>

        ConflictResolver.ConflictItem winner


        @Override
        String toString() { return String.valueOf( winner ) }
    }

    @CompileStatic
    class ScopeComparator implements Comparator<String>{

        @Override
        int compare(String scope1, String scope2) {
            if(scope1 == JavaScopes.COMPILE && !scope2.equals(JavaScopes.COMPILE)) {
                 return 1
            }
            else if(scope1 == JavaScopes.RUNTIME) {
                if(scope2 == JavaScopes.TEST) return 1
                else if(scope2 == JavaScopes.COMPILE) return -1
            }
            else if(scope1 == JavaScopes.TEST) {
                if(scope2 != JavaScopes.TEST) return -1
            }

            return 0
        }
    }
}