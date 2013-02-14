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

package org.gradle.api.internal.artifacts.ivyservice.ivyresolve

import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.gradle.api.internal.artifacts.ivyservice.ModuleVersionResolveException
import spock.lang.Specification

import static org.gradle.api.internal.artifacts.DefaultModuleVersionSelector.newSelector

class DefaultBuildableModuleVersionDescriptorTest extends Specification {
    final DefaultBuildableModuleVersionDescriptor descriptor = new DefaultBuildableModuleVersionDescriptor()
    ModuleSource moduleSource = Mock()

    def "has unknown state by default"() {
        expect:
        descriptor.state == BuildableModuleVersionDescriptor.State.Unknown
    }

    def "can mark as missing"() {
        when:
        descriptor.missing()

        then:
        descriptor.state == BuildableModuleVersionDescriptor.State.Missing
        descriptor.failure == null
    }

    def "can mark as probably missing"() {
        when:
        descriptor.probablyMissing()

        then:
        descriptor.state == BuildableModuleVersionDescriptor.State.ProbablyMissing
        descriptor.failure == null
    }

    def "can mark as failed"() {
        def failure = new ModuleVersionResolveException(newSelector("a", "b", "c"), "broken")

        when:
        descriptor.failed(failure)

        then:
        descriptor.state == BuildableModuleVersionDescriptor.State.Failed
        descriptor.failure == failure
    }

    def "can mark as resolved"() {
        def moduleDescriptor = Mock(ModuleDescriptor)
        ModuleRevisionId moduleRevisionId = Mock();
        1 * moduleRevisionId.organisation >> "group"
        1 * moduleRevisionId.name >> "project"
        1 * moduleRevisionId.revision >> "1.0"
        1 * moduleDescriptor.moduleRevisionId >> moduleRevisionId

        when:
        descriptor.resolved(moduleDescriptor, true, moduleSource)

        then:
        descriptor.state == BuildableModuleVersionDescriptor.State.Resolved
        descriptor.failure == null
        descriptor.descriptor == moduleDescriptor
        descriptor.changing
        descriptor.moduleSource == moduleSource
    }

    def "cannot get result when not resolved"() {
        when:
        descriptor.descriptor

        then:
        thrown(IllegalStateException)

        when:
        descriptor.failure

        then:
        thrown(IllegalStateException)
    }

    def "cannot get result when failed"() {
        given:
        def failure = new ModuleVersionResolveException(newSelector("a", "b", "c"), "broken")
        descriptor.failed(failure)

        when:
        descriptor.descriptor

        then:
        ModuleVersionResolveException e = thrown()
        e == failure
    }

    def "cannot get result when missing"() {
        given:
        descriptor.missing()

        when:
        descriptor.descriptor

        then:
        thrown(IllegalStateException)
    }

    def "cannot get result when probably missing"() {
        given:
        descriptor.probablyMissing()

        when:
        descriptor.descriptor

        then:
        thrown(IllegalStateException)
    }

    def "cannot get ModuleSource when failed"() {
        given:
        def failure = new ModuleVersionResolveException(newSelector("a", "b", "c"), "broken")
        descriptor.failed(failure)

        when:
        descriptor.getModuleSource()

        then:
        ModuleVersionResolveException e = thrown()
        e == failure
    }

    def "cannot get ModuleSource when missing"() {
        given:
        descriptor.missing()

        when:
        descriptor.getModuleSource()

        then:
        thrown(IllegalStateException)
    }

    def "cannot get ModuleSource when probably missing"() {
        given:
        descriptor.probablyMissing()

        when:
        descriptor.getModuleSource()

        then:
        thrown(IllegalStateException)
    }
}
