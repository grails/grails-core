package org.codehaus.groovy.grails.resolve

import org.apache.ivy.core.module.id.ModuleRevisionId

/**
 * @author Graeme Rocher
 * @since 1.3
 */
class GrailsRepoResolverTests extends GroovyTestCase {

    void testTransformGrailsRepositoryPattern() {
        def repoResolver = new GrailsRepoResolver("test", new URL("http://localhost"))

        def url = "http://localhost/grails-[artifact]/tags/RELEASE_*/grails-[artifact]-[revision].[ext]"
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.grails.plugins","feeds", "latest.integration")
        assertEquals "http://localhost/grails-[artifact]/tags/LATEST_RELEASE/grails-[artifact]-[revision].[ext]",repoResolver.transformGrailsRepositoryPattern(mrid, url)

        mrid = ModuleRevisionId.newInstance("org.grails.plugins","feeds", "1.1")
        assertEquals "http://localhost/grails-[artifact]/tags/RELEASE_1_1/grails-[artifact]-[revision].[ext]",repoResolver.transformGrailsRepositoryPattern(mrid, url)
    }

    void testGetPluginList() {
        def repoResolver = new GrailsRepoResolver("test", new URL("http://plugins.grails.org"))

        repoResolver.getPluginList()
    }
}
