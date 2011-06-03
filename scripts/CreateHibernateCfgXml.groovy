/*
 * Copyright 2004-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Gant script that creates a Hibernate cfg.xml file.
 *
 * @author Burt Beckwith
 */

import org.springframework.core.io.FileSystemResource

includeTargets << grailsScript('_GrailsInit')

target (createHibernateCfgXml: 'Creates a hibernate.cfg.xml file') {
    depends(checkVersion)

    cfgFile = new File("$basedir/grails-app/conf/hibernate/hibernate.cfg.xml")
    ant.mkdir dir: cfgFile.parent

    if (cfgFile.exists() && !confirmInput('hibernate.cfg.xml already exists. Overwrite? [y/n]', 'overwrite.hibernate_cfg_xml')) {
        return
    }

    // first check for presence of template in application
    templateFile = new FileSystemResource("$basedir/src/templates/artifacts/hibernate.cfg.xml")
    if (!templateFile.exists()) {
        // now check for template provided by plugins
        def pluginTemplateFiles = resolveResources("file:$pluginsHome/*/src/templates/artifacts/hibernate.cfg.xml")
        if (pluginTemplateFiles) {
            templateFile = pluginTemplateFiles[0]
        }
        else {
            // template not found in application, use default template
            templateFile = grailsResource('src/grails/templates/artifacts/hibernate.cfg.xml')
        }
    }

    copyGrailsResource cfgFile.path, templateFile

    event 'CreatedFile', [cfgFile.path]
    console.updateStatus "Created $cfgFile.path"
}

setDefaultTarget 'createHibernateCfgXml'
