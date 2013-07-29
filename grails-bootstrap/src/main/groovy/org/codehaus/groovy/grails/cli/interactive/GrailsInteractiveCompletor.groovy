/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.cli.interactive

import grails.util.BuildSettings
import grails.util.GrailsNameUtils
import grails.build.interactive.completors.EscapingFileNameCompletor
import grails.build.interactive.completors.RegexCompletor

import java.util.concurrent.ConcurrentHashMap

import jline.ArgumentCompletor
import jline.SimpleCompletor

import org.codehaus.groovy.grails.cli.support.BuildSettingsAware
import groovy.transform.CompileStatic
import jline.Completor

/**
 * A JLine completor for Grails' interactive mode.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
class GrailsInteractiveCompletor extends SimpleCompletor {
    BuildSettings settings
    Map completorCache = new ConcurrentHashMap()

    private ArgumentCompletor bangCompletor = new ArgumentCompletor(
        new RegexCompletor("!\\w+"), new EscapingFileNameCompletor())

    GrailsInteractiveCompletor(BuildSettings settings, List<File> scriptResources) throws IOException{
        super( getScriptNames(scriptResources))
        this.settings = settings
    }

    @Override
    int complete(String buffer, int cursor, List clist) {
        final String trimmedBuffer = buffer.trim()
        if (!trimmedBuffer) {
            return super.complete(buffer, cursor, clist)
        }

        if (trimmedBuffer.contains(' ')) {
            trimmedBuffer = trimmedBuffer.split(' ')[0]
        }

        Completor completor = (Completor)trimmedBuffer[0] == '!' ? bangCompletor : completorCache.get(trimmedBuffer)
        if (completor == null) {
            def className = GrailsNameUtils.getNameFromScript(trimmedBuffer)
            className = "grails.build.interactive.completors.$className"

            try {
                def completorClass = getClass().classLoader.loadClass(className)
                completor = (Completor)completorClass.newInstance()
                if (completor instanceof BuildSettingsAware) {
                    completor.buildSettings = settings
                }
                completorCache.put(trimmedBuffer, completor)
            } catch (e) {
                return super.complete(buffer, cursor, clist)
            }
        }

        try {
            return completor.complete(buffer, cursor, clist)
        } catch (e) {
            return super.complete(buffer, cursor, clist)
        }
    }

    static String[] getScriptNames(List<File> scriptResources) {
        final scriptNames = scriptResources.collect {  File f -> GrailsNameUtils.getScriptName(f.name) }
        scriptNames.remove('create-app')
        scriptNames.remove('install-plugin')
        scriptNames.remove('uninstall-plugin')
        scriptNames << "open"
        scriptNames << "exit"
        scriptNames << "restart-daemon"
        scriptNames << "start-daemon"
        scriptNames << "quit"
        scriptNames << "stop-app"
        scriptNames as String[]
    }
}
