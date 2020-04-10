package grails.plugins

import spock.lang.Specification
import spock.lang.Unroll

class VersionComparatorSpec extends Specification {

    @Unroll
    def "should compare #version1 and #version2 and return #expectedResult"() {
        given:
        def comparator = new VersionComparator();

        when:
        int actualResult = comparator.compare(version1, version2)

        then:
        actualResult == expectedResult

        where:
        version1               | version2               || expectedResult
        "3.1.0"                | "4.0.1"                || -1
        "3.1.10"               | "4.0.1"                || -1
        "3.0.0.BUILD-SNAPSHOT" | "4.0"                  || -1
        "3.1.110"              | "4.0.1"                || -1
        "3.0.0.BUILD-SNAPSHOT" | "3.0.0.BUILD-SNAPSHOT" || 0
        "3.0.0"                | "3.0.0"                || 0
        "4.0.1"                | "3.1.110"              || 1
        "4.0.1"                | "3.0.0.BUILD-SNAPSHOT" || 1
    }
}
