package org.codehaus.groovy.grails.web.sitemesh;

import com.opensymphony.module.sitemesh.RequestConstants;

import org.codehaus.groovy.grails.web.servlet.view.*;
import grails.util.*
import org.springframework.web.context.request.*
import org.springframework.mock.web.*
import org.springframework.core.io.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.errors.*
import org.codehaus.groovy.grails.web.pages.*
import org.codehaus.groovy.grails.support.*
import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests


class GSPSitemeshPageTests extends AbstractGrailsTagTests {

    void testCaptureContent() {
        def template='<g:captureComponent tag=\"testtag\">this is the captured content</g:captureComponent>'
        def gspSiteMeshPage = new GSPSitemeshPage()
        webRequest.currentRequest.setAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE, gspSiteMeshPage)
        webRequest.currentRequest.setAttribute(RequestConstants.PAGE, gspSiteMeshPage)
        def result = applyTemplate(template, [:])
        assertEquals 'this is the captured content', gspSiteMeshPage.getComponentBuffer('page.testtag').toString()
    }
    
    void testCaptureContent2() {
        def template='<g:captureComponent tag=\"testtag\">this is the <g:if test="${true}">captured</g:if> content</g:captureComponent>'
        def gspSiteMeshPage = new GSPSitemeshPage()
        webRequest.currentRequest.setAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE, gspSiteMeshPage)
        webRequest.currentRequest.setAttribute(RequestConstants.PAGE, gspSiteMeshPage)
        def result = applyTemplate(template, [:])
        assertEquals 'this is the captured content', gspSiteMeshPage.getComponentBuffer('page.testtag').toString()
    }

    void testCaptureContent3() {
        def template='<component tag=\"testtag\">this is the <g:if test="${true}">captured</g:if> content</component>'
        def gspSiteMeshPage = new GSPSitemeshPage()
        webRequest.currentRequest.setAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE, gspSiteMeshPage)
        webRequest.currentRequest.setAttribute(RequestConstants.PAGE, gspSiteMeshPage)
        def result = applyTemplate(template, [:])
        assertEquals 'this is the captured content', gspSiteMeshPage.getComponentBuffer('page.testtag').toString()
    }
    
    void testCaptureTitleAndBody() {
        def template='<html><head><title>This is the title</title></head><body onload="somejs();">body here</body></html>'
        def gspSiteMeshPage = new GSPSitemeshPage()
        webRequest.currentRequest.setAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE, gspSiteMeshPage)
        webRequest.currentRequest.setAttribute(RequestConstants.PAGE, gspSiteMeshPage)
        def result = applyTemplate(template, [:])
        assertEquals 'This is the title', gspSiteMeshPage.getProperty('title')
        FastStringWriter writer=new FastStringWriter()
        gspSiteMeshPage.writeBody(writer)
        assertEquals 'body here', writer.toString()
        assertEquals 'somejs();', gspSiteMeshPage.getProperty('body.onload')
    }
    
    void tearDown() {
         RequestContextHolder.setRequestAttributes(null)
    }
}