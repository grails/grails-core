package org.codehaus.groovy.grails.web.mapping.reporting

import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.springframework.mock.web.MockServletContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class AnsiConsoleUrlMappingsRendererSpec extends Specification{

    void "Test render URL mappings to target stream"() {
        given:"A URL mappings renderer"
            def sw = new ByteArrayOutputStream()
            def ps = new PrintStream(sw)
            def renderer = new AnsiConsoleUrlMappingsRenderer(ps)
            renderer.isAnsiEnabled = false
            def urlMappingsHolder = getUrlMappingsHolder {
                "/images/$name**.jpg"(controller:"image")
                "/foo"(resources:"foo")
                "/$controller/$action/$id"()
                "500"(controller:"errors")
                "/"(view:"/index")
            }

        when:"The URL mappings are rendered"
            renderer.render(urlMappingsHolder.urlMappings.toList())
            println sw.toString()
        then:"The output is correct"
            sw.toString() == '''Dynamic Mappings
 |    *     | /                                   | View:   /index           |
 |    *     | /${controller}/${action}/${id}      | Action: (default action) |

Controller: errors
 |    *     | ERROR: 500                          | Action: (default action) |

Controller: foo
 |   GET    | /foo/create                         | Action: create           |
 |   POST   | /foo                                | Action: save             |
 |   GET    | /foo                                | Action: index            |
 |   GET    | /foo/${id}/edit                     | Action: edit             |
 |  DELETE  | /foo/${id}                          | Action: delete           |
 |   PUT    | /foo/${id}                          | Action: update           |
 |   GET    | /foo/${id}                          | Action: show             |

Controller: image
 |    *     | /images/${name}**.jpg               | Action: (default action) |

'''
    }
    UrlMappingsHolder getUrlMappingsHolder(Closure mappings) {
        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
        def allMappings = evaluator.evaluateMappings mappings
        return new DefaultUrlMappingsHolder(allMappings)
    }
}
