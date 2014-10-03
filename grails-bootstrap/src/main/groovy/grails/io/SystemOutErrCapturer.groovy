package grails.io

class SystemOutErrCapturer {
    ByteArrayOutputStream out
    ByteArrayOutputStream err
    SystemStreamsRedirector previousState
    
    SystemOutErrCapturer capture() {
        out = new ByteArrayOutputStream()
        err = new ByteArrayOutputStream()
        previousState = SystemStreamsRedirector.create(null, new PrintStream(out, true), new PrintStream(err, true)).redirect()
        this
    }
    
    void close() {
        if(previousState != null) {
            previousState.redirect()
            previousState = null
        }
    }
    
    public static <T> T withCapturedOutput(Closure<T> closure) {
        SystemOutErrCapturer capturer = new SystemOutErrCapturer().capture()
        try {
            return closure.call(capturer)
        } finally {
            capturer.close()
        }
    }
}
