package org.grails.cli.gradle

import groovy.transform.Immutable
import groovy.transform.ToString

@ToString
@Immutable(knownImmutables=['input','out','err'])
class SystemInOutErrRedirect {
    InputStream input
    PrintStream out
    PrintStream err
    
    SystemInOutErrRedirect redirect() {
        InputStream prevInput = null
        PrintStream prevOut = null
        PrintStream prevErr = null
        if(input != null) {
            prevInput = System.in
            System.setIn(input)
        }
        if(out != null) {
            prevOut = System.out
            System.setOut(out)
        }
        if(err != null) {
            prevErr = System.err
            System.setErr(err)
        }
        new SystemInOutErrRedirect(prevInput, prevOut, prevErr)
    }
}
