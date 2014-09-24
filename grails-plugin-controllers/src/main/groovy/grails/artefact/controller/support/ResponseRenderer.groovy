/*
 * Copyright 2014 the original author or authors.
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
package grails.artefact.controller.support

/**
 * 
 * @author Jeff Brown
 * @since 3.0
 */
trait ResponseRenderer {
    private RenderHelper helper = new RenderHelper()
    
    def render(o) {
        helper.invokeRender this, o.inspect()
    }

    def render(String txt) {
        helper.invokeRender this, txt
    }

    def render(CharSequence txt) {
        helper.invokeRender this, txt
    }

    def render(Map args) {
        helper.invokeRender this, args
    }

    def render(Closure c) {
        helper.invokeRender this, c
    }

    def render(Map args, Closure c) {
        helper.invokeRender this, args, c
    }

    def render(Map args, CharSequence body) {
        helper.invokeRender this, args, body
    }
}
