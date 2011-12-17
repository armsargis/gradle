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

package org.gradle.api.internal.artifacts.ivyservice;

import org.apache.ivy.core.module.descriptor.Artifact;

import java.io.File;

/**
 * Resolver which looks for artifacts first in the Project Resolver, before delegating to the user-defined resolver chain.
 */
public class ArtifactToFileResolverChain implements ArtifactToFileResolver {
    private final ArtifactToFileResolver projectResolver;
    private final ArtifactToFileResolver ivyArtifactResolver;

    public ArtifactToFileResolverChain(ArtifactToFileResolver projectResolver, ArtifactToFileResolver ivyArtifactResolver) {
        this.projectResolver = projectResolver;
        this.ivyArtifactResolver = ivyArtifactResolver;
    }

    public File resolve(Artifact artifact) {
        File projectFile = projectResolver.resolve(artifact);
        if (projectFile != null) {
            return projectFile;
        }

        return ivyArtifactResolver.resolve(artifact);
    }
}
