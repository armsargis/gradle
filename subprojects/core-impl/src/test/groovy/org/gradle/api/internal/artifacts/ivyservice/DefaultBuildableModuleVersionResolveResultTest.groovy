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

package org.gradle.api.internal.artifacts.ivyservice

import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.gradle.api.artifacts.ModuleVersionIdentifier
import spock.lang.Specification
import static org.gradle.api.internal.artifacts.DefaultModuleVersionSelector.newSelector

class DefaultBuildableModuleVersionResolveResultTest extends Specification {
    def result = new DefaultBuildableModuleVersionResolveResult()

    def "cannot get id when no result has been specified"() {
        when:
        result.id

        then:
        IllegalStateException e = thrown()
        e.message == 'No result has been specified.'
    }

    def "cannot get descriptor when no result has been specified"() {
        when:
        result.descriptor

        then:
        IllegalStateException e = thrown()
        e.message == 'No result has been specified.'
    }

    def "cannot get artifact resolver when no result has been specified"() {
        when:
        result.artifactResolver

        then:
        IllegalStateException e = thrown()
        e.message == 'No result has been specified.'
    }

    def "cannot get failure when no result has been specified"() {
        when:
        result.failure

        then:
        IllegalStateException e = thrown()
        e.message == 'No result has been specified.'
    }

    def "cannot get id when resolve failed"() {
        def failure = new ModuleVersionResolveException(newSelector("a", "b", "c"), "broken")

        when:
        result.failed(failure)
        result.id

        then:
        ModuleVersionResolveException e = thrown()
        e == failure
    }

    def "cannot get descriptor when resolve failed"() {
        def failure = new ModuleVersionResolveException(newSelector("a", "b", "c"), "broken")

        when:
        result.failed(failure)
        result.descriptor

        then:
        ModuleVersionResolveException e = thrown()
        e == failure
    }

    def "cannot get artifact resolver when resolve failed"() {
        def failure = new ModuleVersionResolveException(newSelector("a", "b", "c"), "broken")

        when:
        result.failed(failure)
        result.artifactResolver

        then:
        ModuleVersionResolveException e = thrown()
        e == failure
    }

    def "failure is null when successfully resolved"() {
        when:
        result.resolved(Mock(ModuleVersionIdentifier), Mock(ModuleDescriptor), Mock(ArtifactResolver))

        then:
        result.failure == null
    }

    def "fails with a not found exception when not found"() {
        when:
        result.notFound(Mock(ModuleVersionIdentifier))

        then:
        result.failure instanceof ModuleVersionNotFoundException
    }
}
