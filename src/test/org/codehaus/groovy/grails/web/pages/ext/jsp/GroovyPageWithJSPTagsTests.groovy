package org.codehaus.groovy.grails.web.pages.ext.jsp

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests
import org.codehaus.groovy.tools.RootLoader
import org.springframework.core.io.FileSystemResource
import org.codehaus.groovy.grails.web.pages.GroovyPagesServlet
import javax.servlet.http.HttpServletRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 9, 2008
 */
class GroovyPageWithJSPTagsTests extends AbstractGrailsTagTests{

    protected void onInit() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()
        GroovySystem.metaClassRegistry.removeMetaClass HttpServletRequest
        GroovySystem.metaClassRegistry.removeMetaClass MockHttpServletRequest
        TagLibraryResolver.metaClass.resolveRootLoader = {->
            def rootLoader = new RootLoader([] as URL[], Thread.currentThread().getContextClassLoader())
            def res = new FileSystemResource("lib/standard-2.4.jar")
            rootLoader.addURL res.getURL()
            resolver.getResources("file:lib/org.springframework.web*.jar").each {
                rootLoader.addURL it.getURL()
            }            
            return rootLoader
        }
        webRequest.getCurrentRequest().setAttribute(GroovyPagesServlet.SERVLET_INSTANCE, new GroovyPagesServlet())
    }

    protected void onDestroy() {
        GroovySystem.metaClassRegistry.removeMetaClass TagLibraryResolver
    }


    // test for GRAILS-4573 FIXME!
    /*void testIterativeTags() {

        def template = '''
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<body>
<c:forEach var="i" begin="1" end="3"><c:out value="${i}" /> . <c:out value="${i}" /><br/></c:forEach>
</body>
</html>
'''

        printCompiledSource(template) 
        assertOutputContains("1 . 1<br/>2 . 2<br/>3 . 3<br/>",template)
    } */

    void testGRAILS3797() {
        println request.locale
		
		File tempdir=new File(System.getProperty("java.io.tmpdir"),"gspgen")
        tempdir.mkdir()
		
        withConfig("grails.views.gsp.keepgenerateddir='${tempdir.absolutePath}'") {

        messageSource.addMessage("A_ICON",request.locale, "test")
        
          def template = '''
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<html>
  <body>
      <g:form controller="search" action="search" method="get">
        <g:textField name="q" value="" />
        <g:actionSubmit value="search" /><br/>
        <img src="<spring:theme code="A_ICON" alt="icon"/>"/>
      </g:form>
  </body>
</html>
'''
         assertOutputContains '<img src="test"/>', template
        }
        println "gsp source generated to ${tempdir}"

    }

    // test for GRAILS-3845
    void testNestedJSPTags() {
        def template = '''
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
<title>test</title>
</head>
<body>
<c:choose>
<c:when test="${1==1}">
hello
</c:when>
<c:when test="${1==0}">
goodbye
</c:when>

</c:choose>
</body>
</html>
'''
        assertOutputContains 'hello', template
        assertOutputNotContains "goodbye", template
    }
    void testGSPCantOverrideDefaultNamespaceWithJSP() {
       def template = '<%@ taglib prefix="g" uri="http://java.sun.com/jsp/jstl/fmt" %><g:formatNumber number="10" format=".00"/>'

        assertOutputEquals '10.00', template
    }

    void testGSPWithIterativeJSPTag() {
        def template = '''
 <%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<g:set var="foo" value="${[1,2,3]}" />
<c:forEach items="${foo}" var="num"><p>${num}</p></c:forEach>
'''

        assertOutputEquals '''<p>1</p><p>2</p><p>3</p>''', template, [:], { it.toString().trim() }
    }

    void testSimpleTagWithValue() {
        def template = '<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><fmt:formatNumber value="${10}" pattern=".00"/>'

        assertOutputEquals '10.00', template
    }

    void testInvokeJspTagAsMethod() {
        def template = '<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>${fmt.formatNumber(value:10, pattern:".00")}'

        assertOutputEquals '10.00', template

    }

    void testInvokeJspTagAsMethodWithBody() {
        def template = '<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>${fmt.formatNumber(pattern:".00",10)}'

        assertOutputEquals '10.00', template

    }

    void testSimpleTagWithBody() {
        def template = '<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><fmt:formatNumber pattern=".00">10</fmt:formatNumber>'

        assertOutputEquals '10.00', template
    }

    void testSpringJSPTags() {
        def template ='''<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<form:form commandName="address" action="do">
<b>Zip: </b><form:input path="zip"/>
</form:form>'''

        request.setAttribute "address", new TestJspTagAddress(zip:"342343")
        assertOutputEquals '''<form id="address" action="do" method="post">\n<b>Zip: </b><input id="zip" name="zip" type="text" value="342343"/>\n</form>''',
                template, [:], { it.toString().trim() }
    }
}
class TestJspTagAddress {
    String zip
}
