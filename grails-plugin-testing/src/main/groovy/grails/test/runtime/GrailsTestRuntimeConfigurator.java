package grails.test.runtime;

import grails.core.GrailsApplication;
import org.grails.web.servlet.context.support.GrailsRuntimeConfigurator;
import org.springframework.boot.test.ConfigFileApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class GrailsTestRuntimeConfigurator extends GrailsRuntimeConfigurator {
    public GrailsTestRuntimeConfigurator(GrailsApplication application, ApplicationContext parent) {
        super(application, parent);
    }

    @Override
    protected void initializeContext(ApplicationContext mainContext) {
        ConfigFileApplicationContextInitializer contextInitializer = new ConfigFileApplicationContextInitializer();
        contextInitializer.initialize((ConfigurableApplicationContext)mainContext);
        super.initializeContext(mainContext);
    }

    @Override
    protected void initializePluginManager() {
        this.pluginManager = new NoOpGrailsPluginManager();
    }
}
