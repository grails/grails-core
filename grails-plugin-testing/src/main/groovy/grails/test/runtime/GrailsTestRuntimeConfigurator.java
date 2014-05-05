package grails.test.runtime;

import java.io.File;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.TypeFilter;

public class GrailsTestRuntimeConfigurator extends GrailsRuntimeConfigurator {
    public GrailsTestRuntimeConfigurator(GrailsApplication application, ApplicationContext parent) {
        super(application, parent);
    }
    
    @Override
    protected void initializePluginManager() {
        this.pluginManager = new NoOpGrailsPluginManager();
    }
    
    private static final class NoOpGrailsPluginManager implements GrailsPluginManager {
        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            
        }

        @Override
        public GrailsPlugin[] getAllPlugins() {
            return null;
        }

        @Override
        public GrailsPlugin[] getUserPlugins() {
            return null;
        }

        @Override
        public GrailsPlugin[] getFailedLoadPlugins() {
            return null;
        }

        @Override
        public void loadPlugins() throws PluginException {
            
        }

        @Override
        public void doRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
            
        }

        @Override
        public void doPostProcessing(ApplicationContext applicationContext) {
            
        }

        @Override
        public void doWebDescriptor(Resource descriptor, Writer target) {
            
        }

        @Override
        public void doWebDescriptor(File descriptor, Writer target) {
            
        }

        @Override
        public void doDynamicMethods() {
            
        }

        @Override
        public GrailsPlugin getGrailsPlugin(String name) {
            return null;
        }

        @Override
        public GrailsPlugin getGrailsPluginForClassName(String name) {
            return null;
        }

        @Override
        public boolean hasGrailsPlugin(String name) {
            return false;
        }

        @Override
        public GrailsPlugin getFailedPlugin(String name) {
            return null;
        }

        @Override
        public GrailsPlugin getGrailsPlugin(String name, Object version) {
            return null;
        }

        @Override
        public void doRuntimeConfiguration(String pluginName, RuntimeSpringConfiguration springConfig) {
            
        }

        @Override
        public void checkForChanges() {
            
        }

        @Override
        public void setApplication(GrailsApplication application) {
            
        }

        @Override
        public boolean isInitialised() {
            return false;
        }

        @Override
        public void refreshPlugin(String name) {
            
        }

        @Override
        public Collection getPluginObservers(GrailsPlugin plugin) {
            return null;
        }

        @Override
        public void informObservers(String pluginName, Map event) {
            
        }

        @Override
        public void doArtefactConfiguration() {
            
        }

        @Override
        public void registerProvidedArtefacts(GrailsApplication application) {
            
        }

        @Override
        public void shutdown() {
            
        }

        @Override
        public boolean supportsCurrentBuildScope(String pluginName) {
            return false;
        }

        @Override
        public void setLoadCorePlugins(boolean shouldLoadCorePlugins) {
            
        }

        @Override
        public void informOfClassChange(Class<?> aClass) {
            
        }

        @Override
        public List<TypeFilter> getTypeFilters() {
            return null;
        }

        @Override
        public String getPluginPath(String name) {
            return null;
        }

        @Override
        public GrailsPlugin getPluginForInstance(Object instance) {
            return null;
        }

        @Override
        public String getPluginPathForInstance(Object instance) {
            return null;
        }

        @Override
        public String getPluginPathForClass(Class<? extends Object> theClass) {
            return null;
        }

        @Override
        public String getPluginViewsPathForInstance(Object instance) {
            return null;
        }

        @Override
        public String getPluginViewsPathForClass(Class<? extends Object> theClass) {
            return null;
        }

        @Override
        public GrailsPlugin getPluginForClass(Class<?> theClass) {
            return null;
        }

        @Override
        public void informOfFileChange(File file) {
            
        }

        @Override
        public void informOfClassChange(File file, Class cls) {
            
        }

        @Override
        public boolean isShutdown() {
            return false;
        }
        
    }
}
