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
package org.gradle.api.internal.artifacts;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.util.JUnit4GroovyMockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.newHashSet;
import static org.gradle.api.artifacts.ArtifactsTestUtils.createResolvedArtifact;
import static org.gradle.util.Matchers.strictlyEqual;
import static org.gradle.util.WrapUtil.toSet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Hans Dockter
 */
public class DefaultResolvedDependencyTest {
    private JUnit4Mockery context = new JUnit4GroovyMockery();

    @Test
    public void init() {
        String someGroup = "someGroup";
        String someName = "someName";
        String someVersion = "someVersion";
        String someConfiguration = "someConfiguration";
        DefaultResolvedDependency resolvedDependency = new DefaultResolvedDependency(someGroup, someName, someVersion, someConfiguration);
        assertThat(resolvedDependency.getName(), equalTo(someGroup + ":" + someName + ":" + someVersion));
        assertThat(resolvedDependency.getModuleGroup(), equalTo(someGroup));
        assertThat(resolvedDependency.getModuleName(), equalTo(someName));
        assertThat(resolvedDependency.getModuleVersion(), equalTo(someVersion));
        assertThat(resolvedDependency.getConfiguration(), equalTo(someConfiguration));
        assertThat(resolvedDependency.getModuleArtifacts(), equalTo(Collections.<ResolvedArtifact>emptySet()));
        assertThat(resolvedDependency.getChildren(), equalTo(Collections.<ResolvedDependency>emptySet()));
        assertThat(resolvedDependency.getParents(), equalTo(Collections.<ResolvedDependency>emptySet()));
    }

    @Test
    public void getAllModuleArtifacts() {
        ResolvedArtifact moduleArtifact = createArtifact("moduleArtifact");
        ResolvedArtifact childModuleArtifact = createArtifact("childModuleArtifact");
        DefaultResolvedDependency resolvedDependency = new DefaultResolvedDependency("someGroup", "someName", "someVersion", "someConfiguration");
        resolvedDependency.addModuleArtifact(moduleArtifact);
        DefaultResolvedDependency childDependency = new DefaultResolvedDependency("someGroup", "someChild", "someVersion", "someChildConfiguration");
        childDependency.addModuleArtifact(childModuleArtifact);
        resolvedDependency.getChildren().add(childDependency);
        assertThat(resolvedDependency.getAllModuleArtifacts(), equalTo(toSet(moduleArtifact, childModuleArtifact)));
    }

    @Test
    public void getParentArtifacts() {
        DefaultResolvedDependency resolvedDependency = createResolvedDependency();

        Set<ResolvedArtifact> parent1SpecificArtifacts = toSet(createArtifact("parent1Specific"));
        DefaultResolvedDependency parentResolvedDependency1 = createAndAddParent("parent1", resolvedDependency, parent1SpecificArtifacts);

        Set<ResolvedArtifact> parent2SpecificArtifacts = toSet(createArtifact("parent2Specific"));
        DefaultResolvedDependency parentResolvedDependency2 = createAndAddParent("parent2", resolvedDependency, parent2SpecificArtifacts);

        assertThat(resolvedDependency.getParentArtifacts(parentResolvedDependency1), equalTo(parent1SpecificArtifacts));
        assertThat(resolvedDependency.getParentArtifacts(parentResolvedDependency2), equalTo(parent2SpecificArtifacts));
    }

    private ResolvedArtifact createArtifact(String name) {
        return createResolvedArtifact(context, name, "someType", "someExt", new File("pathTo" + name));
    }

    private DefaultResolvedDependency createResolvedDependency() {
        return new DefaultResolvedDependency("someGroup", "someName", "someVersion", "someConfiguration");
    }

    @Test
    public void getArtifacts() {
        DefaultResolvedDependency resolvedDependency = createResolvedDependency();

        Set<ResolvedArtifact> parent1SpecificArtifacts = toSet(createArtifact("parent1Specific"));
        DefaultResolvedDependency parentResolvedDependency1 = createAndAddParent("parent1", resolvedDependency, parent1SpecificArtifacts);

        assertThat(resolvedDependency.getArtifacts(parentResolvedDependency1), equalTo(parent1SpecificArtifacts));
    }

    @Test
    public void getArtifactsWithParentWithoutParentArtifacts() {
        DefaultResolvedDependency resolvedDependency = createResolvedDependency();

        DefaultResolvedDependency parent = new DefaultResolvedDependency("someGroup", "parent", "someVersion", "someConfiguration");
        resolvedDependency.getParents().add(parent);
        assertThat(resolvedDependency.getArtifacts(parent), equalTo(Collections.<ResolvedArtifact>emptySet()));
    }

    @Test
    public void getParentArtifactsWithParentWithoutParentArtifacts() {
        DefaultResolvedDependency resolvedDependency = createResolvedDependency();

        DefaultResolvedDependency parent = new DefaultResolvedDependency("someGroup", "parent", "someVersion", "someConfiguration");
        resolvedDependency.getParents().add(parent);
        assertThat(resolvedDependency.getParentArtifacts(parent), equalTo(Collections.<ResolvedArtifact>emptySet()));
    }

    @Test(expected = InvalidUserDataException.class)
    public void getParentArtifactsWithUnknownParent() {
        DefaultResolvedDependency resolvedDependency = createResolvedDependency();
        DefaultResolvedDependency unknownParent = new DefaultResolvedDependency("someGroup", "parent2", "someVersion", "someConfiguration");
        assertThat(resolvedDependency.getParentArtifacts(unknownParent),
                equalTo(Collections.<ResolvedArtifact>emptySet()));
    }

    @Test(expected = InvalidUserDataException.class)
    public void getArtifactsWithUnknownParent() {
        Set<ResolvedArtifact> someModuleArtifacts = toSet(createArtifact("someModuleResolvedArtifact"));
        DefaultResolvedDependency resolvedDependency = createResolvedDependency();

        DefaultResolvedDependency unknownParent = new DefaultResolvedDependency("someGroup", "parent2", "someVersion", "someConfiguration");
        assertThat(resolvedDependency.getParentArtifacts(unknownParent),
                equalTo(someModuleArtifacts));
    }

    @Test
    public void getAllArtifacts() {
        DefaultResolvedDependency resolvedDependency = createResolvedDependency();

        Set<ResolvedArtifact> parent1SpecificArtifacts = newHashSet(createArtifact("parent1Specific"));
        DefaultResolvedDependency parentResolvedDependency1 = createAndAddParent("parent1", resolvedDependency, parent1SpecificArtifacts);

        createAndAddParent("parent2", resolvedDependency, newHashSet(createArtifact("parent2Specific")));

        DefaultResolvedDependency child = new DefaultResolvedDependency("someGroup", "someChild", "someVersion", "someChildConfiguration");
        resolvedDependency.getChildren().add(child);

        Set<ResolvedArtifact> childParent1SpecificArtifacts = newHashSet(createArtifact("childParent1Specific"));
        createAndAddParent("childParent1", child, childParent1SpecificArtifacts);

        Set<ResolvedArtifact> childParent2SpecificArtifacts = newHashSet(createArtifact("childParent2Specific"));
        createAndAddParent("childParent2", child, childParent2SpecificArtifacts);

        Iterable<ResolvedArtifact> allArtifacts = newHashSet(concat(parent1SpecificArtifacts, childParent1SpecificArtifacts, childParent2SpecificArtifacts));
        assertThat(resolvedDependency.getAllArtifacts(parentResolvedDependency1), equalTo(allArtifacts));
    }

    @Test
    public void equalsAndHashCode() {
        DefaultResolvedDependency dependency = new DefaultResolvedDependency("group", "name", "version", "config");
        DefaultResolvedDependency same = new DefaultResolvedDependency("group", "name", "version", "config");
        DefaultResolvedDependency differentGroup = new DefaultResolvedDependency("other", "name", "version", "config");
        DefaultResolvedDependency differentName = new DefaultResolvedDependency("group", "other", "version", "config");
        DefaultResolvedDependency differentVersion = new DefaultResolvedDependency("group", "name", "other", "config");
        DefaultResolvedDependency differentConfiguration = new DefaultResolvedDependency("group", "name", "version", "other");

        assertThat(dependency, strictlyEqual(same));
        assertThat(dependency, not(equalTo(differentGroup)));
        assertThat(dependency, not(equalTo(differentName)));
        assertThat(dependency, not(equalTo(differentVersion)));
        assertThat(dependency, not(equalTo(differentConfiguration)));
    }

    private DefaultResolvedDependency createAndAddParent(String parentName, DefaultResolvedDependency resolvedDependency, Set<ResolvedArtifact> parentSpecificArtifacts) {
        DefaultResolvedDependency parent = new DefaultResolvedDependency("someGroup", parentName, "someVersion", "someConfiguration");
        resolvedDependency.getParents().add(parent);
        resolvedDependency.addParentSpecificArtifacts(parent, parentSpecificArtifacts);
        return parent;
    }
}
