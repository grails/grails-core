/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.pages;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.sitemesh.GrailsContentBufferingResponse;
import org.codehaus.groovy.grails.web.util.BoundedCharsAsEncodedBytesCounter;
import org.codehaus.groovy.grails.web.util.GrailsPrintWriter;
import org.codehaus.groovy.grails.web.util.StreamCharBuffer;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * A buffered writer that won't commit the response until the buffer has reached the high
 * water mark, or until flush() or close() is called.
 *
 * Performance optimizations by Lari Hotari, 13.03.2009
 *
 * Calculating the Content-Length has been disabled by default since Jetty ignores it (uses Chunked mode anyways).
 * Content-Length mode can be enabled with -DGSPResponseWriter.enableContentLength=true system property.
 *
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 * @author Lari Hotari, Sagire Software Oy
 *
 * Date: Jan 10, 2004
 *
 */
public class GSPResponseWriter extends GrailsPrintWriter {
	private static final Log LOG = LogFactory.getLog(GSPResponseWriter.class);
	private ServletResponse response;
	private BoundedCharsAsEncodedBytesCounter bytesCounter;
	private static final boolean CONTENT_LENGTH_COUNTING_ENABLED = Boolean.getBoolean("GSPResponseWriter.enableContentLength");

	/**
	 * Static factory methdirectWritingod to create the writer.
	 * @param response
	 * @param max
	 * @return  A GSPResponseWriter instance
	 */
	public static GSPResponseWriter getInstance(final ServletResponse response, int max) {
		Writer target=null;
		StreamCharBuffer streamBuffer=null;
		BoundedCharsAsEncodedBytesCounter bytesCounter=null;

		if(!(response instanceof GrailsContentBufferingResponse)) {
			streamBuffer=new StreamCharBuffer(max, 0, max);
			target=streamBuffer.getWriter();
			if(CONTENT_LENGTH_COUNTING_ENABLED) {
				bytesCounter = new BoundedCharsAsEncodedBytesCounter(max * 2, response.getCharacterEncoding());
				streamBuffer.connectTo(bytesCounter.getCountingWriter(), true);
			}
			streamBuffer.connectTo(new StreamCharBuffer.LazyInitializingWriter() { public Writer getWriter() throws IOException { return response.getWriter(); }}, !CONTENT_LENGTH_COUNTING_ENABLED);
		} else {
			try {
				target=response.getWriter();
			} catch (IOException e) {
				LOG.error("Problem getting writer from response",e);
				throw new RuntimeException("Problem getting writer from response",e);
			}
		}
		return new GSPResponseWriter(target, response, bytesCounter);
	} // getInstance()

	/**
	 * Static factory method to create the writer.
	 * @param target The target writer to write too
	 * @param max
	 * @return  A GSPResponseWriter instance
	 */
	public static GSPResponseWriter getInstance(Writer target, int max) {
		StreamCharBuffer streamBuffer=new StreamCharBuffer(max, 0, max);
		streamBuffer.connectTo(target);
		Writer writer=streamBuffer.getWriter();
		return new GSPResponseWriter(writer);
	} // getInstance()

    /**
	 * Private constructor.  Use getInstance() instead.
	 * @param activeWriter buffered writer
	 * @param response
	 * @param streamBuffer StreamCharBuffer instance
	 * @param bytesCounter	Keeps count of encoded bytes count
	 */
	private GSPResponseWriter(Writer activeWriter, final ServletResponse response, BoundedCharsAsEncodedBytesCounter bytesCounter) {
		super(activeWriter);
		this.response = response;
		this.bytesCounter = bytesCounter;
	} // GSPResponseWriter

	/**
	 * Private constructor.  Use getInstance() instead.
	 * @param activeWriter buffered writer
	 */
	private GSPResponseWriter(Writer activeWriter) {
		super(activeWriter);
	}
	
	/**
	 * Close the stream.
	 * @see #checkError()
	 */
	public void close() {
		flush();
		if(CONTENT_LENGTH_COUNTING_ENABLED && bytesCounter != null && response != null && !response.isCommitted()) {
			int size = bytesCounter.size();
			if(size > 0) {
				response.setContentLength(size);
			}
			try {
				response.getWriter().flush();
			} catch (IOException e) {
				handleIOException(e);
			}
		}
	} // close()
} // GSPResponseWriter
