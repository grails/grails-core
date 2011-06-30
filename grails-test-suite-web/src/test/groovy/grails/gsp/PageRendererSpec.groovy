package grails.gsp

import grails.spring.BeanBuilder
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.codehaus.groovy.grails.support.SimpleMapResourceLoader
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup
import org.springframework.core.io.ByteArrayResource
import spock.lang.Specification

 /**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 29/06/2011
 * Time: 11:37
 * To change this template use File | Settings | File Templates.
 */
class PageRendererSpec extends Specification{

    SimpleMapResourceLoader resourceLoader = new SimpleMapResourceLoader()


    void "Test render simple template"() {
        given:
            resourceLoader.resources.put("/foo/_bar.gsp", new ByteArrayResource("Hello \${person}".bytes))
        when:
            def contents = pageRenderer.render(template:"/foo/bar", model:[person:"John"])
        then:
            contents != null
            contents == "Hello John"
    }

    void "Test render template with tag"() {
        given:
            resourceLoader.resources.put("/foo/_bar.gsp", new ByteArrayResource('Hello <g:join in="[\'john\', \'fred\']" />'.bytes))
        when:
            def contents = pageRenderer.render(template:"/foo/bar", model:[person:"John"])
        then:
            contents != null
            contents == "Hello john, fred"
    }

    void "Test renderTo simple template"() {
        given:
            resourceLoader.resources.put("/foo/_bar.gsp", new ByteArrayResource("Hello \${person}".bytes))
        when:
            def sw = new StringWriter()
            pageRenderer.renderTo(template:"/foo/bar", model:[person:"John"], sw)
            def contents = sw.toString()

        then:
            contents != null
            contents == "Hello John"
    }

    PageRenderer getPageRenderer() {
        GroovyPagesTemplateEngine te = new GroovyPagesTemplateEngine()

        te.afterPropertiesSet()
        def renderer = new PageRenderer(te)


        def bb = new BeanBuilder().beans {
            grailsApplication(DefaultGrailsApplication)
            "${ApplicationTagLib.name}"(ApplicationTagLib)
        }
        def ctx = bb.createApplicationContext()
        def ga = ctx.getBean(DefaultGrailsApplication)
        ga.initialise()
        ga.addArtefact(TagLibArtefactHandler.TYPE, ApplicationTagLib)

        def tll = new TagLibraryLookup()
        tll.grailsApplication = ga
        tll.applicationContext = ctx

        tll.afterPropertiesSet()

        te.tagLibraryLookup = tll

        renderer.addResourceLoader(resourceLoader)
        return renderer
    }
}
