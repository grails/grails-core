package org.codehaus.groovy.grails.web.sitemesh;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.codehaus.groovy.grails.web.util.GrailsPrintWriter;

public class GrailsRoutablePrintWriter extends GrailsPrintWriter {
    private PrintWriter destination;
    private DestinationFactory factory;

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

	public Writer getOut() {
		return getDestination();
	}

    @Override
	public Writer getFinalTarget() {
    	if(getDestination() instanceof GrailsPrintWriter) {
    		return ((GrailsPrintWriter)getDestination()).getOut();
    	} else {
    		return getOut();
    	}
	}

	private PrintWriter getDestination() {
        if (destination == null) {
            try {
                destination = factory.activateDestination();
            } catch (IOException e) {
                setError();
            }
        }
        return destination;
    }

    public void updateDestination(DestinationFactory factory) {
        destination = null;
        this.factory = factory;
    }

    public void close() {
        getDestination().close();
    }

    public void println(Object x) {
        getDestination().println(x);
    }

    public void println(String x) {
        getDestination().println(x);
    }

    public void println(char x[]) {
        getDestination().println(x);
    }

    public void println(double x) {
        getDestination().println(x);
    }

    public void println(float x) {
        getDestination().println(x);
    }

    public void println(long x) {
        getDestination().println(x);
    }

    public void println(int x) {
        getDestination().println(x);
    }

    public void println(char x) {
        getDestination().println(x);
    }

    public void println(boolean x) {
        getDestination().println(x);
    }

    public void println() {
        getDestination().println();
    }

    public void print(Object obj) {
        getDestination().print(obj);
    }

    public void print(String s) {
        getDestination().print(s);
    }

    public void print(char s[]) {
        getDestination().print(s);
    }

    public void print(double d) {
        getDestination().print(d);
    }

    public void print(float f) {
        getDestination().print(f);
    }

    public void print(long l) {
        getDestination().print(l);
    }

    public void print(int i) {
        getDestination().print(i);
    }

    public void print(char c) {
        getDestination().print(c);
    }

    public void print(boolean b) {
        getDestination().print(b);
    }

    public void write(String s) {
        getDestination().write(s);
    }

    public void write(String s, int off, int len) {
        getDestination().write(s, off, len);
    }

    public void write(char buf[]) {
        getDestination().write(buf);
    }

    public void write(char buf[], int off, int len) {
        getDestination().write(buf, off, len);
    }

    public void write(int c) {
        getDestination().write(c);
    }

    public boolean checkError() {
        return getDestination().checkError();
    }

    public void flush() {
        getDestination().flush();
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

        public void write(char cbuf[], int off, int len) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void flush() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }

    }

}
