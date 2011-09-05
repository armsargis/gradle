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
package org.gradle.api.internal.changedetection

import spock.lang.Specification
import org.gradle.cache.CacheRepository
import org.gradle.cache.CacheBuilder
import org.gradle.cache.PersistentCache
import org.gradle.cache.PersistentIndexedCache
import org.gradle.cache.ObjectCacheBuilder
import org.gradle.api.invocation.Gradle

class CacheBackedFileSnapshotRepositoryTest extends Specification {
    final CacheRepository cacheRepository = Mock()
    final Gradle gradle = Mock()
    final PersistentIndexedCache<Object, Object> indexedCache = Mock()
    FileSnapshotRepository repository

    def setup() {
        ObjectCacheBuilder<Object, PersistentCache> builder = Mock()
        1 * cacheRepository.indexedCache(Object, Object, "fileSnapshots") >> builder
        1 * builder.forObject(gradle) >> builder
        1 * builder.open() >> indexedCache

        repository = new CacheBackedFileSnapshotRepository(cacheRepository, gradle)
    }

    def "assigns an id when a snapshot is added"() {
        FileCollectionSnapshot snapshot = Mock()

        when:
        def id = repository.add(snapshot)

        then:
        id == 4
        1 * indexedCache.get("nextId") >> (4 as Long)
        1 * indexedCache.put("nextId", 5)
        1 * indexedCache.put(4, snapshot)
        0 * _._
    }

    def "can fetch a snapshot by id"() {
        FileCollectionSnapshot snapshot = Mock()

        when:
        def result = repository.get(4)

        then:
        result == snapshot
        1 * indexedCache.get(4) >> snapshot
        0 * _._
    }

    def "can delete a snapshot by id"() {
        when:
        repository.remove(4)

        then:
        1 * indexedCache.remove(4)
        0 * _._
    }

    def expectCacheOpened() {
        ObjectCacheBuilder<Object, PersistentCache> builder = Mock()
        1 * cacheRepository.indexedCache(Object, Object, "fileSnapshots") >> builder
        1 * builder.open() >> indexedCache
    }
}
