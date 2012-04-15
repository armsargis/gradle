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

import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.core.module.id.ArtifactRevisionId
import org.gradle.api.internal.artifacts.configurations.dynamicversion.CachePolicy

import org.gradle.api.internal.artifacts.ivyservice.dynamicversions.ModuleResolutionCache
import org.gradle.api.internal.artifacts.ivyservice.modulecache.ModuleDescriptorCache
import spock.lang.Specification
import spock.lang.Unroll

import org.gradle.api.internal.externalresource.ivy.ArtifactAtRepositoryKey
import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.module.id.ArtifactId
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.gradle.api.internal.externalresource.cached.CachedExternalResourceIndex
import org.gradle.api.internal.externalresource.metadata.ExternalResourceMetaData
import org.gradle.api.internal.externalresource.metadata.DefaultExternalResourceMetaData
import org.gradle.internal.TrueTimeProvider

class CachingModuleVersionRepositoryTest extends Specification {

    ModuleVersionRepository realRepo = Mock()
    ModuleResolutionCache moduleResolutionCache = Mock()
    ModuleDescriptorCache moduleDescriptorCache = Mock()
    CachedExternalResourceIndex artifactAtRepositoryCache = Mock()
    CachePolicy cachePolicy = Mock()

    CachingModuleVersionRepository repo = new CachingModuleVersionRepository(realRepo, moduleResolutionCache, moduleDescriptorCache, artifactAtRepositoryCache, cachePolicy, new TrueTimeProvider())
    
    @Unroll "last modified date is cached - lastModified = #lastModified"(Date lastModified) {
        given:
        ExternalResourceMetaData externalResourceMetaData = new DefaultExternalResourceMetaData("remote url", lastModified, -1, null, null)
        DownloadedArtifact downloadedArtifact = new DownloadedArtifact(new File("artifact"), externalResourceMetaData)
        Artifact artifact = Mock()
        ArtifactRevisionId id = arid()
        ArtifactAtRepositoryKey atRepositoryKey = new ArtifactAtRepositoryKey(realRepo, id)
        

        and:
        _ * realRepo.isLocal() >> false
        _ * artifactAtRepositoryCache.lookup(atRepositoryKey) >> null
        _ * realRepo.download(artifact) >> downloadedArtifact
        _ * artifact.getId() >> id

        when:
        repo.download(artifact)
        
        then:
        1 * artifactAtRepositoryCache.store(atRepositoryKey, downloadedArtifact.localFile, externalResourceMetaData)
        
        where:
        lastModified << [new Date(), null]
    }

    ArtifactRevisionId arid(Map attrs = [:]) {
        Map defaults = [
                org: "org", name: "name", revision: "1.0",
                type: "type", ext: "ext"
        ]

        attrs = defaults + attrs

        ModuleId mid = new ModuleId(attrs.org, attrs.name)
        new ArtifactRevisionId(
                new ArtifactId(mid, mid.name, attrs.type, attrs.ext),
                new ModuleRevisionId(mid, attrs.revision)
        )
    }

}
