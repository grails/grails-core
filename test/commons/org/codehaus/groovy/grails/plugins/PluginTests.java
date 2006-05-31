package org.codehaus.groovy.grails.plugins;

import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.aware.ClassLoaderAware;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Map;

public class PluginTests extends TestCase {
    GenericApplicationContext appCtx;

    protected void setUp() throws Exception {
        if (appCtx != null) {
            return;
        }
        appCtx = new GenericApplicationContext();

        Resource[] resources = new Resource[2];
        resources[0] = new ClassPathResource("org/codehaus/groovy/grails/plugins/grails-app/services/TestService.groovy");
        resources[1] = new ClassPathResource("org/codehaus/groovy/grails/plugins/grails-app/conf/PooledApplicationDataSource.groovy");

        GrailsApplication application = new DefaultGrailsApplication(resources);
        GrailsPluginLoader.loadPlugins(appCtx, application, "classpath*:org/codehaus/groovy/grails/plugins/*.xml");

        appCtx.refresh();
    }

    public void testAwareBeanPostProcessorsAreRegistered() {
        Map grailsApplicationAwares = appCtx.getBeansOfType(GrailsApplicationAware.class);
        assertEquals(2, grailsApplicationAwares.size());

        Map classLoaderAwares = appCtx.getBeansOfType(ClassLoaderAware.class);
        assertEquals(1, classLoaderAwares.size());
    }

    public void testTestBeanGetClass() {
        TestBean bean = (TestBean)appCtx.getBean("myTestBean1", TestBean.class);
        assertSame(GenericApplicationContext.class, bean.getMyClass());
    }

    public void testGrailsExceptionResolverRequired() {
        GrailsExceptionResolver exceptionResolver = (GrailsExceptionResolver)appCtx.getBean("exceptionHandler", GrailsExceptionResolver.class);
    }

    public void testCommonsMultipartResolverRequired() {
        CommonsMultipartResolver multipartResolver = (CommonsMultipartResolver)appCtx.getBean("multipartResolver", CommonsMultipartResolver.class);
    }

    public void testReloadableResourceBundleMessageSourceRequired() {
        ReloadableResourceBundleMessageSource messageSource = (ReloadableResourceBundleMessageSource)appCtx.getBean("messageSource", ReloadableResourceBundleMessageSource.class);
    }

    public void testLocaleChangeInterceptorRequired() {
        LocaleChangeInterceptor interceptor = (LocaleChangeInterceptor) appCtx.getBean("localeChangeInterceptor", LocaleChangeInterceptor.class);
    }

    public void testCookieLocaleResolverRequired() {
        CookieLocaleResolver localeResolver = (CookieLocaleResolver) appCtx.getBean("localeResolver", CookieLocaleResolver.class);
    }

    public void testTestServiceRequired() {
        Object testService = appCtx.getBean("TestServiceService");
        AopUtils.isAopProxy(testService);
    }

//    public void testHibernateDialectDetectorRequired() {
//        HibernateDialectDetectorFactoryBean dialectDetector = (HibernateDialectDetectorFactoryBean) appCtx.getBean("&dialectDetector", HibernateDialectDetectorFactoryBean.class);
//    }

    public static class MyGrailsApplicationAware implements GrailsApplicationAware {
        public void setGrailsApplication(GrailsApplication grailsApplication) {

        }
    }

    public static class MyClassLoaderAware implements ClassLoaderAware {
        public void setClassLoader(ClassLoader classLoader) {

        }
    }

    public static class MyAwarePlugin implements GrailsPlugin {
        public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
            RootBeanDefinition bd = new RootBeanDefinition(MyGrailsApplicationAware.class);

            applicationContext.registerBeanDefinition(MyGrailsApplicationAware.class.getName(), bd);

            RootBeanDefinition bd2 = new RootBeanDefinition(MyClassLoaderAware.class);

            applicationContext.registerBeanDefinition(MyClassLoaderAware.class.getName(), bd2);
        }
    }

    public static class TestBean {
        private Class myClass;

        public Class getMyClass() {
            return myClass;
        }

        public void setMyClass(Class myClass) {
            this.myClass = myClass;
        }
    }

    public static class MyClassEditorPlugin implements GrailsPlugin {
        public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
            RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
            MutablePropertyValues mpv = new MutablePropertyValues();
            mpv.addPropertyValue("myClass", "org.springframework.context.support.GenericApplicationContext");
            bd.setPropertyValues(mpv);

            applicationContext.registerBeanDefinition("myTestBean1", bd);
        }
    }
}
