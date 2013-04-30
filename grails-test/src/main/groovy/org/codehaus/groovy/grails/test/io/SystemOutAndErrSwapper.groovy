/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.test.io

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic

/**
 * Convenience class to temporarily swap in an output stream
 * for standard error and standard out.
 */
@CompileStatic
class SystemOutAndErrSwapper {

    final boolean echoOut
    final boolean echoErr

    protected PrintStream swappedOutOut
    protected PrintStream swappedOutErr

    protected PrintStream swappedInOut
    protected PrintStream swappedInErr

    protected OutputStream swappedInOutStream
    protected OutputStream swappedInErrStream

    protected boolean swapped = false

    SystemOutAndErrSwapper(boolean echoOut = false, boolean echoErr = false) {
        this.echoOut = echoOut
        this.echoErr = echoErr
    }

    boolean isSwapped() {
        return swapped
    }
/**
     * Replaces System.out and System.err with PrintStream's wrapping outStream and errStream
     *
     * @return [outStream, errStream]
     * @throws IllegalStateException if a swap is already on
     */
    List<OutputStream> swapIn() {
        swapIn(new ByteArrayOutputStream(), new ByteArrayOutputStream())
    }

    /**
     * Replaces System.out and System.err with PrintStream's wrapping outStream and errStream
     *
     * @return [outStream, errStream]
     * @throws IllegalStateException if a swap is already on
     */
    List<OutputStream> swapIn(OutputStream outStream, OutputStream errStream) {
        if (swapped) throw new IllegalStateException("swapIn() called during a swap")

        swappedOutOut = System.out
        swappedOutErr = System.err

        swappedInOutStream = echoOut ? new MultiplexingOutputStream(swappedOutOut, outStream) : outStream
        swappedInErrStream = echoErr ? new MultiplexingOutputStream(swappedOutErr, errStream) : errStream

        swappedInOut = new PrintStream(swappedInOutStream)
        swappedInErr = new PrintStream(swappedInErrStream)

        System.out = swappedInOut
        System.err = swappedInErr

        swapped = true

        [swappedInOutStream, swappedInErrStream]
    }

    /**
     * Restores System.out and System.err to what they were before swappedIn() was called.
     *
     * @return the underlying output streams for the swap ([out, err])
     * @throws IllegalStateException if not in a swap
     */
    List<OutputStream> swapOut() {
        if (!swapped) throw new IllegalStateException("swapOut() called while not during a swap")

        System.out = swappedOutOut
        System.err = swappedOutErr

        swappedOutOut = null
        swappedOutErr = null

        swappedInOut = null
        swappedInErr = null

        def streams = []
        streams << (echoOut ? ((MultiplexingOutputStream)swappedInOutStream).streams.last() : swappedInOutStream)
        streams << (echoErr ? ((MultiplexingOutputStream)swappedInErrStream).streams.last() : swappedInErrStream)

        swappedInOutStream = null
        swappedInErrStream = null

        swapped = false

        streams
    }
}