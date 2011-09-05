/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.reporting

import spock.lang.Specification
import org.w3c.dom.Element
import org.junit.Rule
import org.gradle.util.TemporaryFolder
import org.gradle.util.TextUtil

class HtmlReportRendererTest extends Specification {
    final DomReportRenderer<String> domRenderer = new DomReportRenderer<String>() {
        @Override
        void render(String model, Element parent) {
            parent.appendChild(parent.ownerDocument.createElement(model))
        }
    }
    @Rule final TemporaryFolder tmpDir = new TemporaryFolder()
    final HtmlReportRenderer renderer = new HtmlReportRenderer()

    def "renders report to stream"() {
        StringWriter writer = new StringWriter()

        when:
        renderer.renderer(domRenderer).writeTo("test", writer)

        then:
        writer.toString() == TextUtil.toPlatformLineSeparators('''<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<test></test>
</html>
''')
    }

    def "copies resources into output directory"() {
        File destFile = tmpDir.file('report.txt')

        given:
        renderer.requireResource(getClass().getResource("base-style.css"))

        when:
        renderer.renderer(domRenderer).writeTo("test", destFile)

        then:
        tmpDir.file("base-style.css").file
    }
}
