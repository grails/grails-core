package org.codehaus.groovy.grails.web.pages

import grails.test.AbstractGrailsEnvChangingSpec

import spock.lang.Specification
import grails.test.mixin.TestFor
import grails.util.Environment;
import grails.artefact.Artefact

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 11/7/11
 * Time: 12:03 PM
 * To change this template use File | Settings | File Templates.
 */
@TestFor(CustomApplicationTagLib)
class InvokeTagWithCustomBodyClosureSpec extends AbstractGrailsEnvChangingSpec {
    def "Test that a custom tag library can invoke another tag with a closure body"(grailsEnv) {
        when:'We call a custom tag that invokes an existing tag with a closure body'
            changeGrailsEnv(grailsEnv)
            def content = applyTemplate("<a:myLink />")
            def content2 = applyTemplate("<a:myLink />")
        then:"The expected result is rendered and when we call it a second time the cached version is used so we test that too"
            content == '<a href="/one/two"></a><a href="/foo/bar">Hello World</a>'
            content == content2
        where: 
            grailsEnv << grailsEnvs
    }
}
@Artefact("TagLibrary")
class CustomApplicationTagLib {
    static namespace = "a"
    def myLink = { attrs, body ->
       out << g.link(controller:"one", action:"two")
       out << g.link(controller: "foo", action:"bar") {
           setLink(attrs, "World")
       }
    }

    private setLink(attrs, name) {
        "Hello $name"
    }
}
