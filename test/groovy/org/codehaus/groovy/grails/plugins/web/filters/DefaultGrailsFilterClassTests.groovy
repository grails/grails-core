/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 11, 2007
 */
package org.codehaus.groovy.grails.plugins.web.filters
class DefaultGrailsFilterClassTests extends GroovyTestCase {

     void testBasicFilterParsing() {
         def filterClass = new DefaultGrailsFiltersClass(FirstFilters)

         def configs = filterClass.configs
         def first = configs[0]

         assertEquals "all", first.name
         assert first.scope
         assertEquals "*", first.scope.controller
         assertEquals "*", first.scope.action
         assertTrue first.before instanceof Closure
         assertTrue first.after instanceof Closure
         assertTrue first.afterView instanceof Closure
         
     }
}
class FirstFilters {
    def filters = {
        all(controller:"*", action:"*") {
            before = {

            }
            after = {

            }
            afterView = {
                
            }
        }
    }            
}