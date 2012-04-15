/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.process.internal.child;

import org.gradle.api.internal.ClassPathRegistry;
import org.gradle.api.internal.file.TemporaryFileProvider;
import org.gradle.messaging.remote.Address;
import org.gradle.process.internal.WorkerProcessBuilder;
import org.gradle.process.internal.launcher.BootstrapClassLoaderWorker;
import org.gradle.util.GUtil;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * A factory for a worker process which loads the application classes using the JVM's system ClassLoader.
 *
 * <p>Class loader hierarchy:</p>
 * <pre>
 *                            jvm bootstrap
 *                                 |
 *                +----------------+--------------+
 *                |                               |
 *            jvm system                      worker bootstrap
 *  (GradleWorkerMain, application) (SystemApplicationClassLoaderWorker, logging)
 *                |                   (ImplementationClassLoaderWorker)
 *                |                               |
 *             filter                          filter
 *        (shared packages)                  (logging)
 *                |                              |
 *                +---------------+--------------+
 *                                |
 *                          implementation
 *         (ActionExecutionWorker + worker action implementation)
 * </pre>
 */
public class ApplicationClassesInSystemClassLoaderWorkerFactory implements WorkerFactory {
    private final TemporaryFileProvider temporaryFileProvider;
    private final Object workerId;
    private final String displayName;
    private final WorkerProcessBuilder processBuilder;
    private final List<URL> implementationClassPath;
    private final Address serverAddress;
    private final ClassPathRegistry classPathRegistry;
    private final File classpathJarFile;

    public ApplicationClassesInSystemClassLoaderWorkerFactory(Object workerId, String displayName, WorkerProcessBuilder processBuilder,
                                                              List<URL> implementationClassPath, Address serverAddress,
                                                              ClassPathRegistry classPathRegistry, TemporaryFileProvider temporaryFileProvider) {
        this.workerId = workerId;
        this.displayName = displayName;
        this.processBuilder = processBuilder;
        this.implementationClassPath = implementationClassPath;
        this.serverAddress = serverAddress;
        this.classPathRegistry = classPathRegistry;
        this.temporaryFileProvider = temporaryFileProvider;
        classpathJarFile = createClasspathJarFile(processBuilder);
    }

    private File createClasspathJarFile(WorkerProcessBuilder processBuilder) {
        File classpathJarFile = temporaryFileProvider.createTemporaryFile("GradleWorkerProcess", "classpath.jar");
        new UtilityJarFactory().createClasspathJarFile(classpathJarFile, processBuilder.getApplicationClasspath());
        classpathJarFile.deleteOnExit();
        return classpathJarFile;
    }

    public Collection<File> getSystemClasspath() {
        List<File> systemClasspath = new ArrayList<File>();
        systemClasspath.addAll(classPathRegistry.getClassPath("WORKER_MAIN").getAsFiles());
        systemClasspath.add(classpathJarFile);
        return systemClasspath;
    }

    public Callable<?> create() {
        // Serialize the bootstrap worker, so it can be transported through the system ClassLoader
        ActionExecutionWorker injectedWorker = new ActionExecutionWorker(processBuilder.getWorker(), workerId, displayName, serverAddress);
        ImplementationClassLoaderWorker worker = new ImplementationClassLoaderWorker(processBuilder.getLogLevel(), processBuilder.getSharedPackages(),
                implementationClassPath, injectedWorker);
        byte[] serializedWorker = GUtil.serialize(worker);

        return new BootstrapClassLoaderWorker(classPathRegistry.getClassPath("WORKER_PROCESS").getAsURLs(), serializedWorker);
    }

}
