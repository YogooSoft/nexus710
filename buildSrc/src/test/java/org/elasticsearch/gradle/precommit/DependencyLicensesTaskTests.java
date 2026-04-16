/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.gradle.precommit;

import org.elasticsearch.gradle.test.GradleUnitTestCase;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;

public class DependencyLicensesTaskTests extends GradleUnitTestCase {

    private static final String PERMISSIVE_LICENSE_TEXT = "Eclipse Public License - v 2.0";
    private static final String STRICT_LICENSE_TEXT = "GNU LESSER GENERAL PUBLIC LICENSE Version 3";
    private static final String DEP_NAME = "dummy";
    private static final String DEP_JAR = DEP_NAME + "-1.0.jar";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private UpdateShasTask updateShas;

    private TaskProvider<DependencyLicensesTask> task;

    private Project project;

    private File dummyJar;

    @Before
    public void prepare() throws IOException {
        project = createProject();

        dummyJar = new File(project.getProjectDir(), DEP_JAR);
        Files.write(dummyJar.toPath(), "fake jar content".getBytes(StandardCharsets.UTF_8));

        task = createDependencyLicensesTask(project);
        updateShas = createUpdateShasTask(project, task);
    }

    @Test
    public void givenProjectWithLicensesDirButNoDependenciesThenShouldThrowException() throws Exception {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("exists, but there are no dependencies"));

        getLicensesDir(project).mkdir();
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithoutLicensesDirButWithDependenciesThenShouldThrowException() throws Exception {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("does not exist, but there are dependencies"));

        addDummyDependency();
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithoutLicensesDirNorDependenciesThenShouldReturnSilently() throws Exception {
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithDependencyButNoShaFileThenShouldReturnException() throws Exception {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("Missing SHA for "));

        File licensesDir = getLicensesDir(project);
        createFileIn(licensesDir, DEP_NAME + "-LICENSE.txt", PERMISSIVE_LICENSE_TEXT);
        createFileIn(licensesDir, DEP_NAME + "-NOTICE.txt", "");

        addDummyDependency();
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithDependencyButNoLicenseFileThenShouldReturnException() throws Exception {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("Missing LICENSE for "));

        addDummyDependency();

        getLicensesDir(project).mkdir();
        updateShas.updateShas();
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithDependencyButNoNoticeFileThenShouldReturnException() throws Exception {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("Missing NOTICE for "));

        addDummyDependency();

        createFileIn(getLicensesDir(project), DEP_NAME + "-LICENSE.txt", PERMISSIVE_LICENSE_TEXT);

        updateShas.updateShas();
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithStrictDependencyButNoSourcesFileThenShouldReturnException() throws Exception {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("Missing SOURCES for "));

        addDummyDependency();

        createFileIn(getLicensesDir(project), DEP_NAME + "-LICENSE.txt", STRICT_LICENSE_TEXT);
        createFileIn(getLicensesDir(project), DEP_NAME + "-NOTICE.txt", "");

        updateShas.updateShas();
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithStrictDependencyAndEverythingInOrderThenShouldReturnSilently() throws Exception {
        addDummyDependency();

        createFileIn(getLicensesDir(project), DEP_NAME + "-LICENSE.txt", STRICT_LICENSE_TEXT);
        createFileIn(getLicensesDir(project), DEP_NAME + "-NOTICE.txt", "");
        createFileIn(getLicensesDir(project), DEP_NAME + "-SOURCES.txt", "");

        updateShas.updateShas();
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithDependencyAndEverythingInOrderThenShouldReturnSilently() throws Exception {
        addDummyDependency();

        File licensesDir = getLicensesDir(project);

        createAllDefaultDependencyFiles(licensesDir, DEP_NAME);
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithALicenseButWithoutTheDependencyThenShouldThrowException() throws Exception {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("Unused license "));

        addDummyDependency();

        File licensesDir = getLicensesDir(project);
        createAllDefaultDependencyFiles(licensesDir, DEP_NAME);
        createFileIn(licensesDir, "non-declared-LICENSE.txt", "");

        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithANoticeButWithoutTheDependencyThenShouldThrowException() throws Exception {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("Unused notice "));

        addDummyDependency();

        File licensesDir = getLicensesDir(project);
        createAllDefaultDependencyFiles(licensesDir, DEP_NAME);
        createFileIn(licensesDir, "non-declared-NOTICE.txt", "");

        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithAShaButWithoutTheDependencyThenShouldThrowException() throws Exception {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("Unused sha files found: \n"));

        addDummyDependency();

        File licensesDir = getLicensesDir(project);
        createAllDefaultDependencyFiles(licensesDir, DEP_NAME);
        createFileIn(licensesDir, "non-declared.sha1", "");

        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithADependencyWithWrongShaThenShouldThrowException() throws Exception {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("SHA has changed! Expected "));

        addDummyDependency();

        File licensesDir = getLicensesDir(project);
        createAllDefaultDependencyFiles(licensesDir, DEP_NAME);

        Path sha = Files.list(licensesDir.toPath()).filter(file -> file.toFile().getName().contains("sha")).findFirst().get();

        Files.write(sha, new byte[] { 1 }, StandardOpenOption.CREATE);

        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithADependencyMappingThenShouldReturnSilently() throws Exception {
        addDummyDependency();

        File licensesDir = getLicensesDir(project);
        createAllDefaultDependencyFiles(licensesDir, "mapped-name");

        Map<String, String> mappings = new HashMap<>();
        mappings.put("from", DEP_NAME);
        mappings.put("to", "mapped-name");

        task.get().mapping(mappings);
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithAIgnoreShaConfigurationAndNoShaFileThenShouldReturnSilently() throws Exception {
        addDummyDependency();

        File licensesDir = getLicensesDir(project);
        createFileIn(licensesDir, DEP_NAME + "-LICENSE.txt", PERMISSIVE_LICENSE_TEXT);
        createFileIn(licensesDir, DEP_NAME + "-NOTICE.txt", "");

        task.get().ignoreSha(DEP_NAME);
        task.get().checkDependencies();
    }

    @Test
    public void givenProjectWithoutLicensesDirWhenAskingForShaFilesThenShouldThrowException() {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("isn't a valid directory"));

        task.get().getShaFiles();
    }

    private void addDummyDependency() {
        task.get().setDependencies(project.files(dummyJar));
    }

    private Project createProject() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply(JavaPlugin.class);

        return project;
    }

    private void createAllDefaultDependencyFiles(File licensesDir, String dependencyName) throws IOException, NoSuchAlgorithmException {
        createFileIn(licensesDir, dependencyName + "-LICENSE.txt", PERMISSIVE_LICENSE_TEXT);
        createFileIn(licensesDir, dependencyName + "-NOTICE.txt", "");

        updateShas.updateShas();
    }

    private File getLicensesDir(Project project) {
        return getFile(project, "licenses");
    }

    private File getFile(Project project, String fileName) {
        return project.getProjectDir().toPath().resolve(fileName).toFile();
    }

    private void createFileIn(File parent, String name, String content) throws IOException {
        parent.mkdir();

        Path file = parent.toPath().resolve(name);
        file.toFile().createNewFile();

        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private UpdateShasTask createUpdateShasTask(Project project, TaskProvider<DependencyLicensesTask> dependencyLicensesTask) {
        UpdateShasTask task = project.getTasks().register("updateShas", UpdateShasTask.class).get();

        task.setParentTask(dependencyLicensesTask);
        return task;
    }

    private TaskProvider<DependencyLicensesTask> createDependencyLicensesTask(Project project) {
        TaskProvider<DependencyLicensesTask> task = project.getTasks()
            .register("dependencyLicenses", DependencyLicensesTask.class, new Action<DependencyLicensesTask>() {
                @Override
                public void execute(DependencyLicensesTask dependencyLicensesTask) {
                    dependencyLicensesTask.setDependencies(project.files());
                }
            });

        return task;
    }
}
