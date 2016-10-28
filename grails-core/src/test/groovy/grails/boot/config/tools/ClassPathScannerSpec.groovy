package grails.boot.config.tools

import grails.persistence.Entity
import spock.lang.Specification

/**
 * Created by graemerocher on 06/10/2016.
 */
class ClassPathScannerSpec extends Specification {
    void "Test classpath scanner with package names"() {
        when:"the classpath is scanned"
        ClassPathScanner scanner = new ClassPathScanner()
        def results = scanner.scan(Application, ["grails.boot.config.tools"])

        then:"the results are correct"
        results.size() == 1
        results.contains(Foo)
        Foo.classLoader == Application.classLoader
    }

    void "Test classpath scanner with application"() {
        when:"the classpath is scanned"
        ClassPathScanner scanner = new ClassPathScanner()
        def results = scanner.scan(Application)

        then:"the results are correct"
        results.size() == 1
        results.contains(Foo)
    }
}

class Application {

}

@Entity
class Foo {}