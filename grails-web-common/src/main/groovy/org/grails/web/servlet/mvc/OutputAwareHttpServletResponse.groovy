package org.grails.web.servlet.mvc

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.http.HttpServletResponseWrapper

/**
 * Tracks whether getOutputStream() was called in order to prevent calls to getWriter() after
 * getOutputStream() has been called
 *
 *
 * @since 3.1.12
 * @author Graeme Rocher
 */
@CompileStatic
@InheritConstructors
class OutputAwareHttpServletResponse extends HttpServletResponseWrapper {

    /**
     * Whether the writer is available
     */
    boolean writerAvailable = true

    @Override
    PrintWriter getWriter() throws IOException {
        return super.getWriter()
    }

    @Override
    ServletOutputStream getOutputStream() throws IOException {
        writerAvailable = false
        return super.getOutputStream()
    }
}
