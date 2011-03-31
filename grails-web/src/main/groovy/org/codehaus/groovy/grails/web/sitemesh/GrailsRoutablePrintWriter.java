/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.sitemesh;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.codehaus.groovy.grails.web.util.GrailsPrintWriter;

public class GrailsRoutablePrintWriter extends GrailsPrintWriter {

    private PrintWriter destination;
    private DestinationFactory factory;
    private boolean blockFlush=true;
    private boolean blockClose=true;

    /**
     * Factory to lazily instantiate the destination.
     */
    public static interface DestinationFactory {
        PrintWriter activateDestination() throws IOException;
    }

    public GrailsRoutablePrintWriter(DestinationFactory factory) {
        super(new NullWriter());
        this.factory = factory;
    }

    /**
     * tell others if getOut() can be called to "unwrap" the actual target writer
     *
     * if the destination hasn't been activated, don't allow it.
     *
     * @see org.codehaus.groovy.grails.web.util.GrailsPrintWriter#isAllowUnwrappingOut()
     */
    @Override
    public boolean isAllowUnwrappingOut() {
        return destination != null;
    }

    @Override
    public Writer getOut() {
        return getDestination();
    }

    @Override
    public Writer getFinalTarget() {
        if (getDestination() instanceof GrailsPrintWriter) {
            return ((GrailsPrintWriter)getDestination()).getOut();
        }

        return getOut();
    }

    private PrintWriter getDestination() {
        if (destination == null) {
            try {
                destination = factory.activateDestination();
            }
            catch (IOException e) {
                setError();
            }
            super.out = destination;
        }
        return destination;
    }

    public void updateDestination(DestinationFactory f) {
        destination = null;
        super.out = destination;
        this.factory = f;
    }

    @Override
    public void close() {
    	if(!isBlockClose()) {
    		getDestination().close();
    	}
    }

    @Override
    public void println(Object x) {
        getDestination().println(x);
    }

    @Override
    public void println(String x) {
        getDestination().println(x);
    }

    @Override
    public void println(char x[]) {
        getDestination().println(x);
    }

    @Override
    public void println(double x) {
        getDestination().println(x);
    }

    @Override
    public void println(float x) {
        getDestination().println(x);
    }

    @Override
    public void println(long x) {
        getDestination().println(x);
    }

    @Override
    public void println(int x) {
        getDestination().println(x);
    }

    @Override
    public void println(char x) {
        getDestination().println(x);
    }

    @Override
    public void println(boolean x) {
        getDestination().println(x);
    }

    @Override
    public void println() {
        getDestination().println();
    }

    @Override
    public void print(Object obj) {
        getDestination().print(obj);
    }

    @Override
    public void print(String s) {
        getDestination().print(s);
    }

    @Override
    public void print(char s[]) {
        getDestination().print(s);
    }

    @Override
    public void print(double d) {
        getDestination().print(d);
    }

    @Override
    public void print(float f) {
        getDestination().print(f);
    }

    @Override
    public void print(long l) {
        getDestination().print(l);
    }

    @Override
    public void print(int i) {
        getDestination().print(i);
    }

    @Override
    public void print(char c) {
        getDestination().print(c);
    }

    @Override
    public void print(boolean b) {
        getDestination().print(b);
    }

    @Override
    public void write(String s) {
        getDestination().write(s);
    }

    @Override
    public void write(String s, int off, int len) {
        getDestination().write(s, off, len);
    }

    @Override
    public void write(char buf[]) {
        getDestination().write(buf);
    }

    @Override
    public void write(char buf[], int off, int len) {
        getDestination().write(buf, off, len);
    }

    @Override
    public void write(int c) {
        getDestination().write(c);
    }

    @Override
    public boolean checkError() {
        return getDestination().checkError();
    }

    @Override
    public void flush() {
    	if(!isBlockFlush()) {
    		getDestination().flush();
    	}
    }

    @Override
    public PrintWriter append(char c) {
        return getDestination().append(c);
    }

    @Override
    public PrintWriter append(CharSequence csq, int start, int end) {
        return getDestination().append(csq, start, end);
    }

    @Override
    public PrintWriter append(CharSequence csq) {
        return getDestination().append(csq);
    }

    /**
     * Just to keep super constructor for PrintWriter happy - it's never actually used.
     */
    private static class NullWriter extends Writer {

        protected NullWriter() {
            super();
        }

        @Override
        public void write(char cbuf[], int off, int len) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void flush() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }
    }

	public boolean isBlockFlush() {
		return blockFlush;
	}

	public void setBlockFlush(boolean blockFlush) {
		this.blockFlush = blockFlush;
	}

	public boolean isBlockClose() {
		return blockClose;
	}

	public void setBlockClose(boolean blockClose) {
		this.blockClose = blockClose;
	}
	
	public void unBlockFlushAndClose() {
		this.blockClose = false;
		this.blockFlush = false;
	}

	public void blockFlushAndClose() {
		this.blockClose = true;
		this.blockFlush = true;
	}
}
