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
package org.gradle.api.internal.artifacts.repositories.transport;

import org.apache.ivy.plugins.repository.TransferListener;
import org.apache.ivy.plugins.resolver.AbstractResolver;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.internal.artifacts.ivyservice.filestore.ExternalArtifactCache;
import org.gradle.api.internal.artifacts.repositories.ProgressLoggingTransferListener;
import org.gradle.api.internal.artifacts.repositories.transport.file.FileTransport;
import org.gradle.api.internal.artifacts.repositories.transport.http.HttpTransport;
import org.gradle.logging.ProgressLoggerFactory;

import java.net.URI;

public class RepositoryTransportFactory {
    private final ExternalArtifactCache externalArtifactCache;
    private final TransferListener transferListener;

    public RepositoryTransportFactory(ExternalArtifactCache externalArtifactCache, ProgressLoggerFactory progressLoggerFactory) {
        this.externalArtifactCache = externalArtifactCache;
        this.transferListener = new ProgressLoggingTransferListener(progressLoggerFactory, RepositoryTransport.class);
    }

    public RepositoryTransport createHttpTransport(String name, PasswordCredentials credentials) {
        return decorate(new HttpTransport(name, credentials, externalArtifactCache));
    }

    public RepositoryTransport createFileTransport(String name) {
        // TODO:DAZ Might not want a transfer listener here
        return decorate(new FileTransport(name));
    }
    
    private RepositoryTransport decorate(RepositoryTransport original) {
        return new ListeningRepositoryTransport(original);
    }
    
    private class ListeningRepositoryTransport implements RepositoryTransport {
        private final RepositoryTransport delegate;

        private ListeningRepositoryTransport(RepositoryTransport delegate) {
            this.delegate = delegate;
        }

        public void configureCacheManager(AbstractResolver resolver) {
            delegate.configureCacheManager(resolver);
        }

        public ResourceCollection getRepositoryAccessor() {
            ResourceCollection resourceCollection = delegate.getRepositoryAccessor();
            if (!resourceCollection.hasTransferListener(transferListener)) {
                resourceCollection.addTransferListener(transferListener);
            }
            return resourceCollection;
        }

        public String convertToPath(URI uri) {
            return delegate.convertToPath(uri);
        }
    }
}
