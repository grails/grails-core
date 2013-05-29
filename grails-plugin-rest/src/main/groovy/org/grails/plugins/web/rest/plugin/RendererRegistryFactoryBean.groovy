package org.grails.plugins.web.rest.plugin

import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import groovy.transform.CompileStatic
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * @author Graeme Rocher
 */
@CompileStatic
class RendererRegistryFactoryBean implements InitializingBean, FactoryBean<RendererRegistry>, ApplicationContextAware {

    RendererRegistry rendererRegistry = new DefaultRendererRegistry()
    ApplicationContext applicationContext

    @Override
    void afterPropertiesSet() throws Exception {
        final renderers = applicationContext.getBeansOfType(Renderer).values()
        for(Renderer r in renderers) {
            rendererRegistry.addRenderer(r)
        }
    }

    @Override
    RendererRegistry getObject() throws Exception {
        return rendererRegistry
    }

    @Override
    Class<?> getObjectType() {
        return RendererRegistry
    }

    @Override
    boolean isSingleton() {
        return true
    }
}
