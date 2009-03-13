package grails.util;

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.runtime.InvokerInvocationException;

import junit.framework.TestCase;

public class JSonBuilderTests extends TestCase {

	private HttpServletResponse getResponse(Writer writer) {
		final PrintWriter printer = new PrintWriter(writer);
		return new MockHttpServletResponse() {
			public PrintWriter getWriter() throws UnsupportedEncodingException {
				return printer;
			}
		};
	}	
	
	private String parse(String groovy) throws Exception {
		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("import grails.util.*; class TestClass { List names = [\"Steven\", \"Hans\", \"Erwin\"]; Closure test = { response -> new JSonBuilder(response)." + groovy + "; } }");
		GroovyObject go = (GroovyObject)clazz.newInstance();
		Closure closure = (Closure)go.getProperty("test");
		StringWriter sw = new StringWriter();
		closure.call(getResponse(sw));
		System.out.println(sw.getBuffer().toString());
		return sw.getBuffer().toString();
	}
	
	public void testOpenRicoBuilderElement() throws Exception {
		
		assertEquals( "{\"message\":\"Hello World\"}",
						parse("json(){ message('Hello World') }"));
		
		assertEquals( "{\"integer\":10}",
				parse("json{ integer 5+5 }"));
		
		assertEquals( "{\"message\":5.1}",
				parse("json{ message 5.1 }"));
		
		assertEquals( "{\"names\":[{\"firstName\":\"Steven\"},{\"firstName\":\"Hans\"},{\"firstName\":\"Erwin\"}]}",
				parse("json(){ names{ for( cc in names ){ name( \"firstName\" : cc ) }  }  }"));
		
		assertEquals( "{\"names\":[\"Steven\",\"Hans\",\"Erwin\"]}",
				parse("json(){ names(names) }"));		
		
		assertEquals("{\"book\":{\"title\":\"test1\",\"title2\":\"test2\"}}",
			parse("json(){book(title:\"test1\",title2:\"test2\")}"));
		
		
		try {
			parse("json{ message( \"Hello World\" ){ item() } }");
			fail();
		} catch (InvokerInvocationException e) {
			// expected
		}

		try {
			parse("json{ message( \"Hello World\" ){ item(\"test\") } }");		
			fail();
		} catch (InvokerInvocationException e) {
			// expected
		}
		
	}
	
}
