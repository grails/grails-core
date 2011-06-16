/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.exceptions

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.grails.core.io.ResourceLocator
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.web.util.NestedServletException

/**
 * Default implementation of the {@link StackTracePrinter} interface.
 *
 * @since 1.4
 *
 * @author Graeme Rocher
 * @author Marc Palmer
 */
class DefaultStackTracePrinter implements StackTracePrinter {

    ResourceLocator resourceLocator

    DefaultStackTracePrinter() {}

    DefaultStackTracePrinter(ResourceLocator resourceLocator) {
        this.resourceLocator = resourceLocator
    }

    String prettyPrint(Throwable t) {
        if (t == null) return ''
        final sw = new StringWriter()
        def sb = new PrintWriter(sw)
        def mln = Math.max(4, t.stackTrace.lineNumber.max())
        def lineNumWidth = mln.toString().size()
        def methodNameBaseWidth = t.stackTrace.methodName*.size().max() + 1

        def lh = "Line".padLeft(lineNumWidth + 4)
        String header = "$lh | Method"
        printHeader(sb, header)

        boolean first = true
        Throwable e = t
        while (e != null) {
            if (e instanceof NestedServletException) {
                e = e.cause
                continue
            }
            def last = e.stackTrace.size()
            def prevFn
            def prevLn
            def evenRow = false
            if (!first) {
                printCausedByMessage(sb, e)
            }
            if (e instanceof MultipleCompilationErrorsException) break
            e.stackTrace[0..-1].eachWithIndex { te, idx ->
                def fileName = getFileName(te)
                def lineNumber
                if (e instanceof SourceCodeAware) {
                    if (e.lineNumber && e.lineNumber > -1) {
                        lineNumber = e.lineNumber.toString().padLeft(lineNumWidth)
                    }
                    else {
                        lineNumber = te.lineNumber.toString().padLeft(lineNumWidth)
                    }
                    if (e.fileName) {
                        fileName = e.fileName
                        fileName = makeRelativeIfPossible(fileName)
                    }
                }
                else {
                    lineNumber = te.lineNumber.toString().padLeft(lineNumWidth)
                }

                if (prevLn == lineNumber && idx != last) return // no point duplicating lines
                if ((idx == 0) || fileName) {
                    prevLn = lineNumber
                    if (prevFn && (prevFn == fileName)) {
                        fileName = "    ''"
                    } else {
                        prevFn = fileName
                    }
                    if (!fileName) {
                        fileName = "Unknown source"
                    }

                    def padChar = (evenRow || idx == 0) ? ' ' : ' .'
                    evenRow = evenRow ? false : true

                    def methodName = te.methodName
                    if (methodName.size() < methodNameBaseWidth) {
                        methodName = methodName.padRight(methodNameBaseWidth - 1, padChar)
                    }


                    if (idx == 0) {
                        printFailureLocation(sb, lineNumber, methodName, fileName)
                    } else if (idx < last - 1) {
                        printStackLine(sb, lineNumber, methodName, fileName)
                    } else {
                        printLastEntry(sb, lineNumber, methodName, fileName)
                    }
                }
            }
            first = false
            if (shouldSkipNextCause(e)) break
            e = e.cause
        }

        return sw.toString()
    }

    public static String makeRelativeIfPossible(String fileName) {
        final base = System.getProperty("base.dir")
        if (base) {
            fileName = fileName - base
        }
        return fileName
    }

    protected boolean shouldSkipNextCause(Throwable e) {
        return e.cause == null || e == e.cause
    }

    protected def printCausedByMessage(PrintWriter sb, Throwable e) {
        sb.println()
        sb.println "Caused by ${e.class.simpleName}: ${e.message}"
    }

    protected def printHeader(PrintWriter sb, String header) {
        sb.println header
    }

    protected printLastEntry(PrintWriter sb, String lineNumber, String methodName, String fileName) {
        sb.println "^   $lineNumber | $methodName in $fileName"
    }

    protected printStackLine(PrintWriter sb, String lineNumber, String methodName, String fileName) {
        sb.println "|   $lineNumber | $methodName in $fileName"
    }

    protected printFailureLocation(PrintWriter sb, String lineNumber, String methodName, String fileName) {
        sb.println "->> $lineNumber | $methodName in $fileName"
        sb << "- " * 36
        sb.println()
    }

    protected String getFileName(StackTraceElement te) {
        final res = resourceLocator?.findResourceForClassName(te.className)
        res == null ? te.fileName : res.getFilename()
    }

    String prettyPrintCodeSnippet(Throwable exception) {
        if (exception == null) {
            return ''
        }

        def className = null
        def lineNumber = null
        def sw = new StringWriter()
        def pw = new PrintWriter(sw)
        def lineNumbersShown = [:].withDefault { k -> [] }

        Throwable cause = exception
        while (cause != null) {

            if (!cause.stackTrace) break
            if (cause instanceof NestedServletException) {
                cause = cause.cause
                continue
            }

            boolean first = true
            for (entry in cause.stackTrace) {
                Resource res = null
                className = entry.className
                lineNumber = entry.lineNumber

                lineNumber = getLineNumberInfo(cause, lineNumber)
                if (first) {
                    res = getFileNameInfo(cause, res)
                    if (res != null) {
                        first = false
                    }
                }

                if (!className || !lineNumber) {
                    continue
                }

                res = res ?: resourceLocator.findResourceForClassName(className)
                if (res != null) {
                    if (lineNumbersShown[res.filename].contains(lineNumber)) continue // don't repeat the same lines twice

                    lineNumbersShown[res.filename] << lineNumber
                    pw.print formatCodeSnippetStart(res, lineNumber)
                    final input = null
                    try {
                        input = res.inputStream
                        input.withReader { fileIn ->
                            def reader = new LineNumberReader(fileIn)
                            def last = lineNumber + 3
                            def range = (lineNumber - 3..last)
                            def currentLine = reader.readLine()

                            while (currentLine != null) {
                                Integer currentLineNumber = reader.lineNumber
                                if (currentLineNumber in range) {
                                    boolean isErrorLine = currentLineNumber == lineNumber
                                    if (isErrorLine) {
                                        pw.print formatCodeSnippetErrorLine(currentLineNumber, currentLine)
                                    }
                                    else {
                                        pw.print formatCodeSnippetLine(currentLineNumber, currentLine)
                                    }
                                }
                                else if (currentLineNumber > last) {
                                    break
                                }

                                currentLine = reader.readLine()
                            }
                        }
                    }
                    catch (e) {
                        // ignore
                    }
                    finally {
                        try {
                            input?.close()
                        } catch (e) {
                            // ignore
                        }
                        pw.print formatCodeSnippetEnd(res, lineNumber)
                    }
                }
                else {
                    if (!first) {
                        break
                    }
                }
            }

            if (shouldSkipNextCause(cause)) break
            cause = cause.cause
        }

        return sw.toString()
    }

    protected Resource getFileNameInfo(Throwable cause, Resource res) {
        Throwable start = cause

        while ((start instanceof SourceCodeAware) || (start instanceof MultipleCompilationErrorsException)) {
            if (start instanceof SourceCodeAware) {
                try {
                    if (start.fileName) {
                        final tmp = new FileSystemResource(start.fileName)
                        if (tmp.exists()) {
                            res = tmp
                            break
                        }
                    }
                } catch (e) {
                    // ignore
                }
            }
            else if (start instanceof MultipleCompilationErrorsException) {
                MultipleCompilationErrorsException mcee = start
                Object message = mcee.getErrorCollector().getErrors().iterator().next()
                if (message instanceof SyntaxErrorMessage) {
                    SyntaxErrorMessage sem = (SyntaxErrorMessage)message
                    final tmp = new FileSystemResource(sem.getCause().getSourceLocator())
                    if (tmp.exists()) {
                        res = tmp
                        break
                    }
                }
            }

            start = start.cause
            if (start == null || start == start.cause) break
        }
        return res
    }

    protected int getLineNumberInfo(Throwable cause, int defaultInfo) {
        int lineNumber = defaultInfo
        if (cause instanceof SourceCodeAware) {
            SourceCodeAware sca = cause
            lineNumber = sca.lineNumber
        }
        else if (cause instanceof MultipleCompilationErrorsException) {
            MultipleCompilationErrorsException mcee = cause
            Object message = mcee.getErrorCollector().getErrors().iterator().next()
            if (message instanceof SyntaxErrorMessage) {
                SyntaxErrorMessage sem = (SyntaxErrorMessage)message
                lineNumber = sem.getCause().getLine()
            }
        }
        return lineNumber
    }

    String formatCodeSnippetEnd(Resource resource, int lineNumber) {
        ""
    }

    String formatCodeSnippetStart(Resource resource, int lineNumber) {
        """Around line $lineNumber of $resource.filename
"""

    }

    protected String formatCodeSnippetLine(int currentLineNumber, currentLine) {
        return """${currentLineNumber}: ${currentLine}
"""
    }

    protected String formatCodeSnippetErrorLine(int currentLineNumber, currentLine) {
        return """${currentLineNumber}: ${currentLine}
"""
    }

    /**
     * Obtains the root cause of the given exception
     * @param ex The exception
     * @return The root cause
     */
    protected Throwable getRootCause(Throwable ex) {
        while (ex.getCause() != null && !ex.equals(ex.getCause())) {
            ex = ex.getCause()
        }
        return ex
    }
}
