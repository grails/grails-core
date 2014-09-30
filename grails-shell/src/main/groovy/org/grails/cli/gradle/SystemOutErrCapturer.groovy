package org.grails.cli.gradle

class SystemOutErrCapturer {
    ByteArrayOutputStream out
    ByteArrayOutputStream err
    SystemInOutErrRedirect previousState
    
    SystemOutErrCapturer capture() {
        out = new ByteArrayOutputStream()
        err = new ByteArrayOutputStream()
        previousState = new SystemInOutErrRedirect(null, new PrintStream(out, true), new PrintStream(err, true)).redirect()
        this
    }
    
    void close() {
        if(previousState != null) {
            previousState.redirect()
            previousState = null
        }
    }
    
    public static <T> T doWithCapturer(Closure<T> closure) {
        SystemOutErrCapturer capturer = new SystemOutErrCapturer().capture()
        try {
            return closure.call(capturer)
        } finally {
            capturer.close()
        }
    }
}
