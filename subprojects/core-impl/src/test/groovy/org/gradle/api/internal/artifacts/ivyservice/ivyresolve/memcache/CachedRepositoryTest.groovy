/*
 * Copyright 2013 the original author or authors.
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



package org.gradle.api.internal.artifacts.ivyservice.ivyresolve.memcache

import org.apache.ivy.core.module.descriptor.Artifact
import org.gradle.api.artifacts.ArtifactIdentifier
import org.gradle.api.internal.artifacts.ivyservice.BuildableArtifactResolveResult
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.BuildableModuleVersionMetaData
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.DependencyMetaData
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.LocalAwareModuleVersionRepository
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleSource
import spock.lang.Specification

import static org.gradle.api.internal.artifacts.DefaultModuleVersionSelector.newSelector

/**
 * By Szczepan Faber on 4/19/13
 */
class CachedRepositoryTest extends Specification {

    def stats = new DependencyMetadataCacheStats()
    def cache = Mock(DependencyMetadataCache)
    def delegate = Mock(LocalAwareModuleVersionRepository)
    def repo = new CachedRepository(cache, delegate, stats)

    def lib = newSelector("org", "lib", "1.0")
    def dep = Stub(DependencyMetaData) { getRequested() >> lib }
    def result = Mock(BuildableModuleVersionMetaData)

    def "delegates"() {
        when:
        def id = repo.getId()
        def name = repo.getName()

        then:
        id == "x"
        name == "localRepo"
        1 * delegate.getId() >> "x"
        1 * delegate.getName() >> "localRepo"
    }

    def "retrieves and caches local dependencies"() {
        when:
        repo.getLocalDependency(dep, result)

        then:
        1 * cache.supplyLocalMetaData(lib, result) >> false
        1 * delegate.getLocalDependency(dep, result)
        1 * cache.newLocalDependencyResult(lib, result)
        0 * _
    }

    def "uses local dependencies from cache"() {
        when:
        repo.getLocalDependency(dep, result)

        then:
        1 * cache.supplyLocalMetaData(lib, result) >> true
        0 * _
    }

    def "retrieves and caches dependencies"() {
        when:
        repo.getDependency(dep, result)

        then:
        1 * cache.supplyMetaData(lib, result) >> false
        1 * delegate.getDependency(dep, result)
        1 * cache.newDependencyResult(lib, result)
        0 * _
    }

    def "uses dependencies from cache"() {
        when:
        repo.getDependency(dep, result)

        then:
        1 * cache.supplyMetaData(lib, result) >> true
        0 * _
    }

    def "retrieves and caches artifacts"() {
        def result = Mock(BuildableArtifactResolveResult)
        def artifact = Stub(Artifact)
        def source = Mock(ModuleSource)

        when:
        repo.resolve(artifact, result, source)

        then:
        1 * cache.supplyArtifact(_ as ArtifactIdentifier, result) >> false
        1 * delegate.resolve(artifact, result, source)
        1 * cache.newArtifact(_ as ArtifactIdentifier, result)
        0 * _
    }

    def "uses artifacts from cache"() {
        def result = Mock(BuildableArtifactResolveResult)

        when:
        repo.resolve(Stub(Artifact), result, Mock(ModuleSource))

        then:
        1 * cache.supplyArtifact(_ as ArtifactIdentifier, result) >> true
        0 * _
    }
}
