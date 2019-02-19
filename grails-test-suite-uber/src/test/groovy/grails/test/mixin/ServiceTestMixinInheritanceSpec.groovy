package grails.test.mixin

import grails.artefact.Artefact
import org.grails.core.artefact.ServiceArtefactHandler
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.Specification

/**
 * Created by graemerocher on 04/10/2016.
 */
class ServiceTestMixinInheritanceSpec extends TestSuperClass {

    void "Test inherit transaction manager"() {
        expect:
        transactionManager.is(TestSuperClass.manager)
    }
}
@Artefact(ServiceArtefactHandler.TYPE)
class TestService {

}
class TestSuperClass extends Specification {
    public static final DatastoreTransactionManager manager = new DatastoreTransactionManager()

    PlatformTransactionManager getTransactionManager() {
        return manager
    }
}
