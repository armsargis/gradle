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
package org.gradle.api.internal.artifacts.ivyservice.ivyresolve;

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.ResolverSettings;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.gradle.api.internal.Factory;
import org.gradle.api.internal.artifacts.ivyservice.CacheLockingManager;
import org.gradle.util.UncheckedException;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;

/**
 * The main entry point for a {@link DependencyResolver} to call back into the dependency
 * resolution mechanism.
 */
public class LoopbackDependencyResolver extends DelegatingDependencyResolver {
    private final String name;
    private final CacheLockingManager cacheLockingManager;

    public LoopbackDependencyResolver(String name, DependencyResolver resolver, CacheLockingManager cacheLockingManager) {
        super(resolver);
        this.name = name;
        this.cacheLockingManager = cacheLockingManager;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setSettings(ResolverSettings settings) {
        // Ignore
    }

    @Override
    public ResolvedModuleRevision getDependency(final DependencyDescriptor dd, final ResolveData data) throws ParseException {
        return cacheLockingManager.useCache(String.format("Resolve %s", dd), new Factory<ResolvedModuleRevision>() {
            public ResolvedModuleRevision create() {
                try {
                    return getResolver().getDependency(dd, data);
                } catch (ParseException e) {
                    throw UncheckedException.asUncheckedException(e);
                }
            }
        });
    }

    @Override
    public ArtifactDownloadReport download(final ArtifactOrigin artifact, final DownloadOptions options) {
        return cacheLockingManager.useCache(String.format("download %s", artifact), new Factory<ArtifactDownloadReport>() {
            public ArtifactDownloadReport create() {
                return getResolver().download(artifact, options);
            }
        });
    }

    @Override
    public DownloadReport download(final Artifact[] artifacts, final DownloadOptions options) {
        return cacheLockingManager.useCache(String.format("Download %s", Arrays.toString(artifacts)), new Factory<DownloadReport>() {
            public DownloadReport create() {
                return getResolver().download(artifacts, options);
            }
        });
    }

    @Override
    public ResolvedResource findIvyFileRef(final DependencyDescriptor dd, final ResolveData data) {
        return cacheLockingManager.useCache(String.format("Locate ivy file for %s", dd), new Factory<ResolvedResource>() {
            public ResolvedResource create() {
                return getResolver().findIvyFileRef(dd, data);
            }
        });
    }

    @Override
    public boolean exists(final Artifact artifact) {
        return cacheLockingManager.useCache(String.format("Locate %s", artifact), new Factory<Boolean>() {
            public Boolean create() {
                return getResolver().exists(artifact);
            }
        });
    }

    @Override
    public ArtifactOrigin locate(final Artifact artifact) {
        return cacheLockingManager.useCache(String.format("Locate %s", artifact), new Factory<ArtifactOrigin>() {
            public ArtifactOrigin create() {
                return getResolver().locate(artifact);
            }
        });
    }

    @Override
    public ModuleEntry[] listModules(OrganisationEntry org) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OrganisationEntry[] listOrganisations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RevisionEntry[] listRevisions(ModuleEntry module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] listTokenValues(String token, Map otherTokenValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map[] listTokenValues(String[] tokens, Map criteria) {
        throw new UnsupportedOperationException();
    }
}
