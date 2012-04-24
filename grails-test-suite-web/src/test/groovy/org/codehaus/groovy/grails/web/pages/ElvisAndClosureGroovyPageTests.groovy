package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests
class ElvisAndClosureGroovyPageTests extends AbstractGrailsTagTests{
    
    
    void testElvisOperaturUsedWithClosure() {

        def template = '<g:set var="finder" value="${myList.find{ it == \'a\' } ?: \'default\'}"/>${finder}'
        
        def content = applyTemplate(template, [myList:['b','d','a', 'c']])
        
        assert content == 'a'
    }
}

