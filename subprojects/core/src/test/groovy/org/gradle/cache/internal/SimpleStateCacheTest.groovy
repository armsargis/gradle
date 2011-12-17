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
package org.gradle.cache.internal

import org.gradle.cache.DefaultSerializer
import org.gradle.cache.PersistentStateCache
import org.gradle.cache.Serializer
import org.gradle.util.TemporaryFolder
import org.junit.Rule
import spock.lang.Specification

class SimpleStateCacheTest extends Specification {
    @Rule public TemporaryFolder tmpDir = new TemporaryFolder()
    final FileAccess fileAccess = Mock()
    final Serializer<String> serializer = new DefaultSerializer<String>()
    final SimpleStateCache<String> cache = new SimpleStateCache<String>(tmpDir.file("state.bin"), fileAccess, serializer)

    def "returns null when file does not exist"() {
        when:
        def result = cache.get()

        then:
        result == null
        1 * fileAccess.readFromFile(!null) >> { it[0].create() }
    }
    
    def "get returns last value written to file"() {
        when:
        cache.set('some value')

        then:
        1 * fileAccess.writeToFile(!null) >> { it[0].run() }
        tmpDir.file('state.bin').assertIsFile()

        when:
        def result = cache.get()

        then:
        result == 'some value'
        1 * fileAccess.readFromFile(!null) >> { it[0].create() }
    }

    def "update provides access to cached value"() {
        when:
        cache.set("foo")

        then:
        1 * fileAccess.writeToFile(!null) >> { it[0].run() }

        when:
        cache.update({ value ->
            assert value == "foo"
            return "foo bar"
        } as PersistentStateCache.UpdateAction)

        then:
        1 * fileAccess.writeToFile(!null) >> { it[0].run() }

        when:
        def result = cache.get()

        then:
        result == "foo bar"
        1 * fileAccess.readFromFile(!null) >> { it[0].create() }
    }

    def "update does not explode when no existing value"() {
        when:
        cache.update({ value ->
            assert value == null
            return "bar"
        } as PersistentStateCache.UpdateAction)

        then:
        1 * fileAccess.writeToFile(!null) >> { it[0].run() }

        when:
        def result = cache.get()

        then:
        result == "bar"
        1 * fileAccess.readFromFile(!null) >> { it[0].create() }
    }
}
