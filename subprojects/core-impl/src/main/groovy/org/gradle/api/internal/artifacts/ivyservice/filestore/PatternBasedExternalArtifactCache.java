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
package org.gradle.api.internal.artifacts.ivyservice.filestore;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.internal.file.collections.DirectoryFileTree;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import java.io.File;
import java.util.List;

public class PatternBasedExternalArtifactCache implements ExternalArtifactCache {
    private final DirectoryFileTree fileTree;
    private final String pattern;

    public PatternBasedExternalArtifactCache(File baseDir, String pattern) {
        fileTree = new DirectoryFileTree(baseDir);
        this.pattern = pattern;
    }

    public void addMatchingCachedArtifacts(ArtifactRevisionId artifactId, final List<CachedArtifact> cachedArtifactList) {
        if (artifactId == null) {
            return;
        }
        getMatchingFiles(artifactId).visit(new EmptyFileVisitor() {
            public void visitFile(FileVisitDetails fileDetails) {
                cachedArtifactList.add(new DefaultCachedArtifact(fileDetails.getFile()));
            }
        });
    }

    private DirectoryFileTree getMatchingFiles(ArtifactRevisionId artifact) {
        String patternString = getArtifactPattern(artifact);
        PatternFilterable pattern = new PatternSet();
        pattern.include(patternString);
        return fileTree.filter(pattern);
    }

    private String getArtifactPattern(ArtifactRevisionId artifactId) {
        Artifact dummyArtifact = new DefaultArtifact(artifactId, null, null, false);
        String substitute = IvyPatternHelper.substitute(pattern, dummyArtifact);
        String organisationPath = artifactId.getModuleRevisionId().getOrganisation().replace('.', '/');
        substitute = IvyPatternHelper.substituteToken(substitute, "organisation-path", organisationPath);
        return substitute;
    }
    
}
