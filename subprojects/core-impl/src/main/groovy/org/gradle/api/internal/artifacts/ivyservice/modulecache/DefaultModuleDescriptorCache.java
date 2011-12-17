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
package org.gradle.api.internal.artifacts.ivyservice.modulecache;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.gradle.api.internal.artifacts.ivyservice.ArtifactCacheMetaData;
import org.gradle.api.internal.artifacts.ivyservice.CacheLockingManager;
import org.gradle.api.internal.artifacts.ivyservice.artifactcache.ArtifactResolutionCache;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleVersionRepository;
import org.gradle.cache.PersistentIndexedCache;
import org.gradle.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;

public class DefaultModuleDescriptorCache implements ModuleDescriptorCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModuleDescriptorCache.class);

    private final TimeProvider timeProvider;
    private final ArtifactCacheMetaData cacheMetadata;
    private final CacheLockingManager cacheLockingManager;

    private final ArtifactResolutionCache artifactResolutionCache;
    private final ModuleDescriptorStore moduleDescriptorStore;
    private PersistentIndexedCache<RevisionKey, ModuleDescriptorCacheEntry> cache;

    private IvySettings ivySettings;

    public DefaultModuleDescriptorCache(ArtifactCacheMetaData cacheMetadata, TimeProvider timeProvider, CacheLockingManager cacheLockingManager, ArtifactResolutionCache artifactResolutionCache) {
        this.timeProvider = timeProvider;
        this.cacheLockingManager = cacheLockingManager;
        this.cacheMetadata = cacheMetadata;
        this.artifactResolutionCache = artifactResolutionCache;

        // TODO:DAZ inject this
        moduleDescriptorStore = new ModuleDescriptorStore(new ModuleDescriptorFileStore(cacheMetadata));
    }

    // TODO:DAZ This is a bit nasty
    public void setSettings(IvySettings settings) {
        this.ivySettings = settings;
    }

    private PersistentIndexedCache<RevisionKey, ModuleDescriptorCacheEntry> getCache() {
        if (cache == null) {
            cache = initCache();
        }
        return cache;
    }

    private PersistentIndexedCache<RevisionKey, ModuleDescriptorCacheEntry> initCache() {
        File artifactResolutionCacheFile = new File(cacheMetadata.getCacheDir(), "module-metadata.bin");
        return cacheLockingManager.createCache(artifactResolutionCacheFile, RevisionKey.class, ModuleDescriptorCacheEntry.class);
    }

    public CachedModuleDescriptor getCachedModuleDescriptor(ModuleVersionRepository repository, ModuleRevisionId moduleRevisionId) {
        ModuleDescriptorCacheEntry moduleDescriptorCacheEntry = getCache().get(createKey(repository, moduleRevisionId));
        if (moduleDescriptorCacheEntry == null) {
            return null;
        }
        ModuleDescriptor descriptor = null;
        if (!moduleDescriptorCacheEntry.isMissing) {
            descriptor = moduleDescriptorStore.getModuleDescriptor(repository, moduleRevisionId, ivySettings);
        }
        return new DefaultCachedModuleDescriptor(moduleDescriptorCacheEntry, descriptor, timeProvider);
    }

    public void cacheModuleDescriptor(ModuleVersionRepository repository, ModuleRevisionId moduleRevisionId, ModuleDescriptor moduleDescriptor, boolean isChanging) {
        if (moduleDescriptor == null) {
            LOGGER.debug("Recording absence of module descriptor in cache: {} [changing = {}]", moduleRevisionId, isChanging);
            getCache().put(createKey(repository, moduleRevisionId), createMissingEntry(isChanging));
        } else {
            LOGGER.debug("Recording module descriptor in cache: {} [changing = {}]", moduleDescriptor.getModuleRevisionId(), isChanging);
            expireArtifactsForChangingModuleIfRequired(repository, moduleDescriptor, isChanging);
    
            // TODO:DAZ Cache will already be locked, due to prior call to getCachedModuleDescriptor. This locking should be more explicit
            moduleDescriptorStore.putModuleDescriptor(repository, moduleDescriptor);
            getCache().put(createKey(repository, moduleRevisionId), createEntry(isChanging));
        }
    }

    private void expireArtifactsForChangingModuleIfRequired(ModuleVersionRepository repository, ModuleDescriptor newDescriptor, boolean newDescriptorIsChanging) {
        // Expire all cached artifacts if either the cached module descriptor was changing, or the new module descriptor is changing
        CachedModuleDescriptor cachedModuleDescriptor = getCachedModuleDescriptor(repository, newDescriptor.getModuleRevisionId());
        if (cachedModuleDescriptor == null) {
            return;
        }
        if (cachedModuleDescriptor.isChangingModule() || newDescriptorIsChanging) {
            ModuleDescriptor oldDescriptor = cachedModuleDescriptor.getModuleDescriptor();
            // Only do this if the publication date has changed
            // TODO:DAZ Get rid of this and rely on sha1 files to prevent re-download.
            // Will then be able to do before resolving the module, rather than waiting until we have the new descriptor to compare
            if (oldDescriptor.getResolvedPublicationDate().getTime() != newDescriptor.getResolvedPublicationDate().getTime()) {
                expireArtifacts(repository, oldDescriptor);
            }
        }
    }

    private void expireArtifacts(ModuleVersionRepository repository, ModuleDescriptor descriptor) {
        for (Artifact artifact : descriptor.getAllArtifacts()) {
            artifactResolutionCache.expireCachedArtifactResolution(repository, artifact.getId());
        }
    }

    private RevisionKey createKey(ModuleVersionRepository resolver, ModuleRevisionId moduleRevisionId) {
        return new RevisionKey(resolver, moduleRevisionId);
    }

    private ModuleDescriptorCacheEntry createMissingEntry(boolean changing) {
        return new ModuleDescriptorCacheEntry(changing, true, timeProvider);
    }

    private ModuleDescriptorCacheEntry createEntry(boolean changing) {
        return new ModuleDescriptorCacheEntry(changing, false, timeProvider);
    }

    private static class RevisionKey implements Serializable {
        private final String resolverId;
        private final String moduleRevisionId;

        private RevisionKey(ModuleVersionRepository repository, ModuleRevisionId moduleRevisionId) {
            this.resolverId = repository.getId();
            this.moduleRevisionId = moduleRevisionId == null ? null : moduleRevisionId.encodeToString();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof RevisionKey)) {
                return false;
            }
            RevisionKey other = (RevisionKey) o;
            return resolverId.equals(other.resolverId) && moduleRevisionId.equals(other.moduleRevisionId);
        }

        @Override
        public int hashCode() {
            return resolverId.hashCode() ^ moduleRevisionId.hashCode();
        }
    }

}
