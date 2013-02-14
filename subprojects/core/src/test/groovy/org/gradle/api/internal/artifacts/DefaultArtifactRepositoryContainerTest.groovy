/*
 * Copyright 2007 the original author or authors.
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

package org.gradle.api.internal.artifacts

import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.gradle.api.Action
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.UnknownRepositoryException
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.internal.artifacts.repositories.ArtifactRepositoryInternal
import org.gradle.api.internal.artifacts.repositories.FixedResolverArtifactRepository
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.internal.reflect.Instantiator
import spock.lang.Specification

class DefaultArtifactRepositoryContainerTest extends Specification {

    BaseRepositoryFactory repositoryFactory
    DefaultArtifactRepositoryContainer container

    def setup() {
        repositoryFactory = Mock(BaseRepositoryFactory)
        container = createResolverContainer()
    }

    ArtifactRepositoryContainer createResolverContainer(
            BaseRepositoryFactory repositoryFactory = repositoryFactory,
            Instantiator instantiator = new DirectInstantiator()
    ) {
        new DefaultArtifactRepositoryContainer(repositoryFactory, instantiator)
    }

    List setupNotation(int i, repositoryFactory = repositoryFactory) {
        setupNotation("repoNotation$i", "repo$i", "resolver$i", repositoryFactory)
    }

    List setupNotation(notation, repoName, resolverName, repositoryFactory = repositoryFactory) {
        def repo = Mock(ArtifactRepositoryInternal) { getName() >> repoName }
        def resolver = new FileSystemResolver()
        def resolverRepo = Spy(FixedResolverArtifactRepository, constructorArgs: [resolver])

        interaction {
            1 * repositoryFactory.createRepository(notation) >> repo
            1 * repositoryFactory.toResolver(repo) >> resolver
            1 * repositoryFactory.createResolverBackedRepository(resolver) >> resolverRepo
            1 * resolverRepo.onAddToContainer(container)
        }

        [notation, repo, resolver, resolverRepo]
    }

    def "can add resolver"() {
        given:
        def (repo1Notation, repo1, resolver1, resolverRepo1) = setupNotation(1)
        def (repo2Notation, repo2, resolver2, resolverRepo2) = setupNotation(2)

        expect:
        container.addLast(repo1Notation).is resolver1
        assert container.findByName(resolver1.name) != null
        container.addLast(repo2Notation)
        container == [resolverRepo1, resolverRepo2]
    }

    def "can add repositories with duplicate names"() {
        given:
        def (repo1Notation, repo1, resolver1, resolverRepo1) = setupNotation(1)
        def (repo2Notation, repo2, resolver2, resolverRepo2) = setupNotation(2)

        when:
        container.addLast(repo1Notation)
        container.addLast(repo2Notation)

        then:
        container*.name == ["repository", "repository2"]
    }

    def testAddResolverWithClosure() {
        given:
        def repo = Mock(ArtifactRepositoryInternal) { getName() >> "name" }
        def resolver = new FileSystemResolver()
        def resolverRepo = Spy(FixedResolverArtifactRepository, constructorArgs: [resolver])

        interaction {
            1 * repositoryFactory.createRepository(resolver) >> repo
            1 * repositoryFactory.toResolver(repo) >> resolver
            1 * repositoryFactory.createResolverBackedRepository(resolver) >> resolverRepo
            1 * resolverRepo.onAddToContainer(container)
        }

        when:
        container.add(resolver) {
            transactional = "foo"
            name = "bar"
        }

        then:
        resolver.transactional == "foo"
        resolverRepo.name == "bar"
    }

    def testAddBefore() {
        given:
        def (repo1Notation, repo1, resolver1, resolverRepo1) = setupNotation(1)
        def (repo2Notation, repo2, resolver2, resolverRepo2) = setupNotation(2)

        when:
        container.addLast(repo1Notation)
        container.addBefore(repo2Notation, "repository")

        then:
        container == [resolverRepo2, resolverRepo1]
    }

    def testAddAfter() {
        given:
        def (repo1Notation, repo1, resolver1, resolverRepo1) = setupNotation(1)
        def (repo2Notation, repo2, resolver2, resolverRepo2) = setupNotation(2)
        def (repo3Notation, repo3, resolver3, resolverRepo3) = setupNotation(3)

        when:
        container.addLast(repo1Notation)
        container.addAfter(repo2Notation, "repository")
        container.addAfter(repo3Notation, "repository")

        then:
        container == [resolverRepo1, resolverRepo3, resolverRepo2]
    }


    def testAddBeforeWithUnknownResolver() {
        when:
        container.addBefore("asdfasd", 'unknownName')

        then:
        thrown(UnknownRepositoryException)
    }

    def testAddAfterWithUnknownResolver() {
        when:
        container.addAfter("asdfasd", 'unknownName')

        then:
        thrown(UnknownRepositoryException)
    }

    def testAddFirst() {
        given:
        def repo1 = Mock(ArtifactRepository) { getName() >> "a" }
        def repo2 = Mock(ArtifactRepository) { getName() >> "b" }

        when:
        container.addFirst(repo1)
        container.addFirst(repo2)

        then:
        container == [repo2, repo1]
        container.collect { it } == [repo2, repo1]
        container.matching { true } == [repo2, repo1]
        container.matching { true }.collect { it } == [repo2, repo1]
    }

    def testAddLast() {
        given:
        def repo1 = Mock(ArtifactRepository) { getName() >> "a" }
        def repo2 = Mock(ArtifactRepository) { getName() >> "b" }

        when:
        container.addLast(repo1)
        container.addLast(repo2)

        then:
        container == [repo1, repo2]
    }

    def testAddFirstUsingUserDescription() {
        given:
        def (repo1Notation, repo1, resolver1, resolverRepo1) = setupNotation(1)
        def (repo2Notation, repo2, resolver2, resolverRepo2) = setupNotation(2)

        when:
        container.addFirst(repo1Notation)
        container.addFirst(repo2Notation)

        then:
        container == [resolverRepo2, resolverRepo1]
    }

    def testAddLastUsingUserDescription() {
        given:
        def (repo1Notation, repo1, resolver1, resolverRepo1) = setupNotation(1)
        def (repo2Notation, repo2, resolver2, resolverRepo2) = setupNotation(2)

        when:
        container.addLast(repo1Notation)
        container.addLast(repo2Notation)

        then:
        container == [resolverRepo1, resolverRepo2]
    }

    public void testAddWithUnnamedResolver() {
        given:
        def (repo1Notation, repo1, resolver1, resolverRepo1) = setupNotation(1)
        resolver1.name = null

        when:
        container.addLast(repo1Notation)

        then:
        resolver1.name == 'repository'
    }

    def testGetThrowsExceptionForUnknownResolver() {
        when:
        container.getByName("unknown")

        then:
        def e = thrown(UnknownRepositoryException)
        e.message == "Repository with name 'unknown' not found."
    }

    def notificationsAreFiredWhenRepositoryIsAdded() {
        Action<ArtifactRepository> action = Mock(Action)
        ArtifactRepository repository = Mock(ArtifactRepository)

        when:
        container.all(action)
        container.add(repository)

        then:
        1 * action.execute(repository)
    }

    def notificationsAreFiredWhenRepositoryIsAddedToTheHead() {
        Action<ArtifactRepository> action = Mock(Action)
        ArtifactRepository repository = Mock(ArtifactRepository)

        when:
        container.all(action)
        container.addFirst(repository)

        then:
        1 * action.execute(repository)
    }

    def notificationsAreFiredWhenRepositoryIsAddedToTheTail() {
        Action<ArtifactRepository> action = Mock(Action)
        ArtifactRepository repository = Mock(ArtifactRepository)

        when:
        container.all(action)
        container.addLast(repository)

        then:
        1 * action.execute(repository)
    }

}
