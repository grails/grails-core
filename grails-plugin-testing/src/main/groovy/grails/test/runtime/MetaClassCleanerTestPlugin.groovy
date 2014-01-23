package grails.test.runtime;

import groovy.transform.CompileStatic;

import org.codehaus.groovy.grails.cli.support.MetaClassRegistryCleaner

@CompileStatic
public class MetaClassCleanerTestPlugin implements TestPlugin {
    String[] requiredFeatures = []
    String[] providedFeatures = ['metaClassCleaner']
    int ordinal = 0

    protected MetaClassRegistryCleaner metaClassRegistryListener

    protected void registerMetaClassRegistryWatcher() {
        metaClassRegistryListener = MetaClassRegistryCleaner.createAndRegister()
    }

    void cleanupModifiedMetaClasses() {
        metaClassRegistryListener.clean()
    }

    void deregisterMetaClassCleaner() {
        MetaClassRegistryCleaner.cleanAndRemove(metaClassRegistryListener)
    }

    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'beforeClass':
                registerMetaClassRegistryWatcher()
                break
            case 'afterClass':
                deregisterMetaClassCleaner()
                break
        }
    }
}
