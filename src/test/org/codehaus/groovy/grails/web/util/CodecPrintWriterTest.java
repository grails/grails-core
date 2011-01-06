package org.codehaus.groovy.grails.web.util;

import static org.junit.Assert.*;

import java.io.IOException;

import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec;
import org.codehaus.groovy.grails.web.pages.FastStringWriter;
import org.junit.Test;

public class CodecPrintWriterTest {

	@Test
	public void testPrintString() {
		FastStringWriter stringwriter=new FastStringWriter();
		CodecPrintWriter writer=new CodecPrintWriter(stringwriter, HTMLCodec.class);
		writer.print("&&");
		assertEquals("&amp;&amp;", stringwriter.getValue());
	}
	
	@Test
	public void testPrintStringWithClosure() {
		FastStringWriter stringwriter=new FastStringWriter();
		CodecPrintWriter writer=new CodecPrintWriter(stringwriter, CodecWithClosureProperties.class);
		writer.print("hello");
		assertEquals("-> hello <-", stringwriter.getValue());
	}
	
	@Test
	public void testPrintStreamCharBuffer() throws IOException {
		FastStringWriter stringwriter=new FastStringWriter();
		CodecPrintWriter writer=new CodecPrintWriter(stringwriter, HTMLCodec.class);
		StreamCharBuffer buf=new StreamCharBuffer();
		buf.getWriter().write("&&");
		writer.write(buf);
		assertEquals("&amp;&amp;", stringwriter.getValue());
	}
	
	@Test
	public void testPrintStreamCharBufferWithClosure() throws IOException {
		FastStringWriter stringwriter=new FastStringWriter();
		CodecPrintWriter writer=new CodecPrintWriter(stringwriter, CodecWithClosureProperties.class);
		StreamCharBuffer buf=new StreamCharBuffer();
		buf.getWriter().write("hola");
		writer.write(buf);
		assertEquals("-> hola <-", stringwriter.getValue());
	}	

}
