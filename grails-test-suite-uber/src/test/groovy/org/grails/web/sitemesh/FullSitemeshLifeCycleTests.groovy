package org.grails.web.sitemesh

import org.grails.core.io.MockStringResourceLoader
import org.grails.web.taglib.AbstractGrailsTagTests
import org.springframework.mock.web.MockServletConfig

import com.opensymphony.module.sitemesh.Config
import com.opensymphony.module.sitemesh.Decorator
import com.opensymphony.module.sitemesh.DecoratorMapper
import com.opensymphony.module.sitemesh.PageParser
import com.opensymphony.module.sitemesh.RequestConstants
import com.opensymphony.module.sitemesh.factory.BaseFactory

/**
 * Tests the sitemesh capturing and rendering tags end-to-end
 *
 * @author Graeme Rocher
 * @since 1.2
 */
class FullSitemeshLifeCycleTests extends AbstractGrailsTagTests {

    void testSimpleLayout() {
        def template = '''
<html>
        <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"><title>This is the title</title></head>
        <body onload="test();">body text</body>
</html>
'''

        assertOutputEquals '''
<html>
        <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"><title>This is the title</title></head>
        <body onload="test();">body text</body>
</html>
''', template

        def layout = '''
<html>
    <head><title>Decorated <g:layoutTitle /></title><g:layoutHead /></head>
    <body><h1>Hello</h1><g:layoutBody /></body>
</html>
'''
        def result = applyLayout(layout, template)

        assertEquals '''
<html>
    <head><title>Decorated This is the title</title><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"></head>
    <body><h1>Hello</h1>body text</body>
</html>
''', result
    }

    void testTitleInSubTemplate() {
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/_title.gsp', '<title>This is the title</title>')
        appCtx.groovyPageLocator.addResourceLoader resourceLoader

        def template = '''
<html>
        <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"><g:render template="/title"/></head>
        <body onload="test();">body text</body>
</html>
'''

        assertOutputEquals '''
<html>
        <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"><title>This is the title</title></head>
        <body onload="test();">body text</body>
</html>
''', template

        def layout = '''
<html>
    <head><title>Decorated <g:layoutTitle /></title><g:layoutHead /></head>
    <body><h1>Hello</h1><g:layoutBody /></body>
</html>
'''
        def result = applyLayout(layout, template)

        assertEquals '''
<html>
    <head><title>Decorated This is the title</title><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"></head>
    <body><h1>Hello</h1>body text</body>
</html>
''', result
    }

    static class DummySiteMeshFactory extends BaseFactory {
        DummySiteMeshFactory(Config config) {
            super(config)
        }

        @Override
        void refresh() {
        }
        
        @Override
        public PageParser getPageParser(String contentType) {
            new GrailsHTMLPageParser()
        }
    }

    def configureSitemesh() {
        def mockServletConfig = new MockServletConfig()
        def siteMeshConfig = new Config(mockServletConfig)
        def siteMeshFactory = new DummySiteMeshFactory(siteMeshConfig)
        def decorator = {name -> [getPage: {-> "/layout/${name}.gsp".toString()}] as Decorator }
        siteMeshFactory.decoratorMapper = [getNamedDecorator: {request, name -> decorator(name)}] as DecoratorMapper
        FactoryHolder.factory = siteMeshFactory
    }

    void testMultipleLevelsOfLayouts() {
        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource('/layout/dialog.gsp', '''<html>
        <head><g:layoutHead /><title>Dialog - <g:layoutTitle /></title></head>
        <body onload="${g.pageProperty(name:'body.onload')}"><div id="dialog"><g:layoutBody /></div></body>
</html>''')
        resourceLoader.registerMockResource('/layout/base.gsp', '''<html>
        <head><g:layoutHead /><title>Base - <g:layoutTitle /></title></head>
        <body onload="${g.pageProperty(name:'body.onload')}"><div id="base"><g:layoutBody /></div></body>
</html>''')
        appCtx.groovyPageLocator.addResourceLoader resourceLoader

        configureSitemesh()

        def template = '''
<g:applyLayout name="base"><g:applyLayout name="dialog">
<html>
        <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"><title>This is the title</title></head>
        <body onload="test();">body text</body>
</html>
</g:applyLayout></g:applyLayout>
'''
        request.setAttribute(RequestConstants.PAGE, new GSPSitemeshPage())
        request.setAttribute(GrailsLayoutView.GSP_SITEMESH_PAGE, new GSPSitemeshPage())
        assertOutputEquals '''
<html>
        <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"><title>Base - Dialog - This is the title</title></head>
        <body onload="test();"><div id="base"><div id="dialog">body text</div></div></body>
</html>
''', template

        def layout = '''
<html>
    <head><title>Decorated <g:layoutTitle default="defaultTitle"/></title><g:layoutHead /></head>
    <body><h1>Hello</h1><g:layoutBody /></body>
</html>
'''
        def result = applyLayout(layout, template)

        assertEquals '''
<html>
    <head><title>Decorated Base - Dialog - This is the title</title><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"></head>
    <body><h1>Hello</h1><div id="base"><div id="dialog">body text</div></div></body>
</html>
''', result
    }

    void testParameters() {
        def template = '''
<html>
  <head>
      <title>Simple GSP page</title>
      <meta name="layout" content="main"/>
      <parameter name="navigation" value="here!"/>
  </head>
  <body>Place your content here</body>
</html>
'''

        def layout = '<h1>pageProperty: ${pageProperty(name: \'page.navigation\')}</h1>'

        def result = applyLayout(layout, template)

        assertEquals '<h1>pageProperty: here!</h1>', result
    }

    void testParametersWithLogic() {
        def template = '''
<html>
  <head>
      <title>Simple GSP page</title>
      <meta name="layout" content="main"/>
      <parameter name="sideBarSetting" value="vendor"/>
  </head>
  <body>Place your content here</body>
</html>
'''
        def layout = '''<g:if test="${pageProperty(name:'page.sideBarSetting') == 'vendor'}">good</g:if>'''

        def result = applyLayout(layout, template, [:])

        assertEquals 'good', result
    }
    
    // GRAILS-11484
    void testMultilineTitle() {
        def template = '''
<html>
        <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>
This is the title
</title></head>
        <body onload="test();">body text</body>
</html>
'''

        assertOutputEquals '''
<html>
        <head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>
This is the title
</title></head>
        <body onload="test();">body text</body>
</html>
''', template

        def layout = '''
<html>
    <head><title>Decorated <g:layoutTitle /></title><g:layoutHead /></head>
    <body><h1>Hello</h1><g:layoutBody /></body>
</html>
'''
        def result = applyLayout(layout, template)

        assertEquals '''
<html>
    <head><title>Decorated 
This is the title
</title><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
</head>
    <body><h1>Hello</h1>body text</body>
</html>
''', result
    }

}
