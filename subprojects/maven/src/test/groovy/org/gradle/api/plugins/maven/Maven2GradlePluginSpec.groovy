/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.plugins.maven

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * by Szczepan Faber, created at: 8/1/12
 */
class Maven2GradlePluginSpec extends Specification {

    def project = new ProjectBuilder().build()

    def "applies plugin"() {
        when:
        project.plugins.apply Maven2GradlePlugin

        then:
        project.tasks.maven2Gradle instanceof ConvertMaven2Gradle
    }
}
