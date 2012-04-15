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
package org.gradle.cache.internal

import org.gradle.cache.internal.FileLockManager.LockMode
import org.gradle.internal.Factory
import org.gradle.internal.nativeplatform.ProcessEnvironment
import org.gradle.internal.nativeplatform.services.NativeServices
import org.gradle.util.TemporaryFolder
import org.junit.Rule
import spock.lang.Specification

class OnDemandFileAccessTest extends Specification {
    final FileLockManager manager = Mock()
    final FileLock targetLock = Mock()

    @Rule TemporaryFolder dir = new TemporaryFolder()
    OnDemandFileAccess lock
    File file

    def setup() {
        file = dir.file("some-target-file")
        lock = new OnDemandFileAccess(file, "some-lock", manager)
    }

    def "acquires shared lock to read file"() {
        def action = {} as Factory

        when:
        lock.readFile(action)

        then:
        !file.exists()
        1 * manager.lock(file, LockMode.Shared, "some-lock") >> targetLock
        1 * targetLock.readFile(action)
        1 * targetLock.close()
        0 * targetLock._
    }

    def "acquires exclusive lock to update file"() {
        def action = {} as Runnable

        when:
        lock.updateFile(action)

        then:
        !file.exists()
        1 * manager.lock(file, LockMode.Exclusive, "some-lock") >> targetLock
        1 * targetLock.updateFile(action)
        1 * targetLock.close()
        0 * targetLock._
    }

    def "acquires exclusive lock to write file"() {
        def action = {} as Runnable

        when:
        lock.writeFile(action)

        then:
        !file.exists()
        1 * manager.lock(file, LockMode.Exclusive, "some-lock") >> targetLock
        1 * targetLock.writeFile(action)
        1 * targetLock.close()
        0 * targetLock._
    }

    def "can read from file"() {
        given:
        def access = access(file)

        expect:
        access.readFile { assert !file.exists(); true }

        when:
        access.updateFile { file << "aaa" }

        then:
        access.readFile { file.text } == "aaa"
    }

    def "can write and update"() {
        given:
        def access = access(file)

        when:
        access.writeFile { file << "1" }
        access.updateFile { file << "2" }

        then:
        access.readFile { file.text } == "12"
    }

    FileLockManager createManager() {
        new DefaultFileLockManager(new DefaultProcessMetaDataProvider(new NativeServices().get(ProcessEnvironment)))
    }

    FileAccess access(File file, FileLockManager manager = createManager()) {
        new OnDemandFileAccess(file, "some-lock", manager)
    }
}
