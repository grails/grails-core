package org.codehaus.groovy.grails.web.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * Counts chars encoded as bytes up to a certain limit (capacity of byte buffer).
 *
 * size() returns the number of bytes, it will return -1 if the capacity was
 * reached or an error occurred.
 *
 * this class is useful for calculating the content length of a
 * HttpServletResponse before the response has been committed
 *
 * @author Lari Hotari, Sagire Software Oy
 *
 */
public class BoundedCharsAsEncodedBytesCounter {
	private ByteBuffer bb;
	private Charset charset;
	private CharsetEncoder ce;
	private boolean calculationActive = true;
	private BoundedCharsAsEncodedBytesCounterWriter writer=new BoundedCharsAsEncodedBytesCounterWriter();

	public BoundedCharsAsEncodedBytesCounter(int capacity, String encoding) {
		charset = Charset.forName(encoding);
		ce = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE);
		bb = ByteBuffer.allocate(capacity);
	}

	public void update(String str) {
		if (str.length() == 0)
			return;
		if (calculationActive) {
			update(str.toCharArray());
		}
	}

	public void update(char[] buf) {
		update(buf, 0, buf.length);
	}

	public void update(char[] buf, int off, int len) {
		if (calculationActive && len > 0) {
			try {
				CharBuffer cb = CharBuffer.wrap(buf, off, len);
				ce.reset();
				CoderResult cr = ce.encode(cb, bb, true);
				if (!cr.isUnderflow()) {
					terminateCalculation();
					return;
				}
				cr = ce.flush(bb);
				if (!cr.isUnderflow()) {
					terminateCalculation();
					return;
				}
			} catch (BufferOverflowException e) {
				terminateCalculation();
			} catch (Exception x) {
				terminateCalculation();
			}
		}
	}

	private void terminateCalculation() {
		calculationActive = false;
		bb.clear();
		bb = null;
	}

	public int size() {
		if (calculationActive) {
			return bb.position();
		} else {
			return -1;
		}
	}

	public Writer getCountingWriter() {
		return writer;
	}

	class BoundedCharsAsEncodedBytesCounterWriter extends Writer {
		char[] writeBuffer = new char[8192];

		@Override
		public void write(char[] b, int off, int len) throws IOException {
			update(b, off, len);
		}

		@Override
		public void close() throws IOException {

		}

		@Override
		public void write(int b) throws IOException {
			if(!calculationActive) return;
			writeBuffer[0] = (char) b;
			update(writeBuffer, 0, 1);
		}

		@Override
		public Writer append(CharSequence csq, int start, int end)
				throws IOException {
			if(!calculationActive) return this;
			if (csq instanceof StringBuilder || csq instanceof StringBuffer) {
				int len = end - start;
				char cbuf[];
				if (len <= writeBuffer.length) {
					cbuf = writeBuffer;
				} else {
					cbuf = new char[len];
				}
				if (csq instanceof StringBuilder) {
					((StringBuilder) csq).getChars(start, end, cbuf, 0);
				} else {
					((StringBuffer) csq).getChars(start, end, cbuf, 0);
				}
				write(cbuf, 0, len);
			} else {
				write(csq.subSequence(start, end).toString());
			}
			return this;
		}

		@Override
		public Writer append(CharSequence csq) throws IOException {
			if(!calculationActive) return this;
			if (csq == null) {
				write("null");
			} else {
				append(csq, 0, csq.length());

			}
			return this;
		}

		@Override
		public void write(String str, int off, int len) throws IOException {
			if(!calculationActive) return;
			StringCharArrayAccessor.writeStringAsCharArray(this, str, off, len);
		}

		@Override
		public void write(String str) throws IOException {
			if(!calculationActive) return;
			StringCharArrayAccessor.writeStringAsCharArray(this, str);
		}

		@Override
		public void flush() throws IOException {

		}
	}
}
