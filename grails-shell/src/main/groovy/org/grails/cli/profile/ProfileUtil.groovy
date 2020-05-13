package org.grails.cli.profile

import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.Exclusion

/**
 * The utility class for the Grails profiles.
 *
 * @author Puneet Behl
 * @since 4.1
 */
class ProfileUtil {

    static Dependency createDependency(String coords, String scope, Map configEntry) {
        if (coords.count(':') == 1) {
            coords = "$coords:BOM"
        }
        Dependency dependency = new Dependency(new DefaultArtifact(coords), scope.toString())
        if (configEntry.containsKey('excludes')) {
            List<Exclusion> dependencyExclusions = new ArrayList<>()
            List excludes = (List) configEntry.excludes
            for (ex in excludes) {
                if (ex instanceof Map) {
                    dependencyExclusions.add(new Exclusion((String) ex.group, (String) ex.module, (String) ex.classifier, (String) ex.extension))
                }
            }
            dependency = dependency.setExclusions(dependencyExclusions)
        }
        dependency
    }
}
