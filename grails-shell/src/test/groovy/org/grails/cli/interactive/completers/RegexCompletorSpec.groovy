package org.grails.cli.interactive.completers

import spock.lang.*

class RegexCompletorSpec extends Specification {
    @Unroll("String '#source' is not matching")
    def "Simple pattern matches"() {
        given: "a regex completor and an empty candidate list"
        def completor = new RegexCompletor(/!\w+/)
        def candidateList = []

        when: "the completor is invoked for a given string"
        def retval = completor.complete(source, 0, candidateList)

        then: "that string is the sole candidate and the return value is 0"
        candidateList.size() == 1
        candidateList[0] == source
        retval == 0

        where:
        source << [ "!ls", "!test_stuff" ]
    }

    @Unroll("String '#source' is incorrectly matching")
    def "Non matching strings"() {
        given: "a regex completor and an empty candidate list"
        def completor = new RegexCompletor(/!\w+/)
        def candidateList = []

        when: "the completor is invoked for a given (non-matching) string"
        def retval = completor.complete(source, 0, candidateList)

        then: "the candidate list is empty and the return value is -1"
        candidateList.size() == 0
        retval == -1

        where:
        source << [ "!ls ls", "!", "test", "" ]
    }
}
