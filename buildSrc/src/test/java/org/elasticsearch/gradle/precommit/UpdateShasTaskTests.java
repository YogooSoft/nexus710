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

import org.apache.commons.io.FileUtils;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

public class UpdateShasTaskTests extends GradleUnitTestCase {

    private static final String DEP_NAME = "dummy";
    private static final String DEP_JAR = DEP_NAME + "-1.0.jar";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private UpdateShasTask task;

    private Project project;

    private File dummyJar;

    @Before
    public void prepare() throws IOException {
        project = createProject();

        dummyJar = new File(project.getProjectDir(), DEP_JAR);
        Files.write(dummyJar.toPath(), "fake jar content".getBytes(StandardCharsets.UTF_8));

        task = createUpdateShasTask(project);
    }

    @Test
    public void whenDependencyDoesntExistThenShouldDeleteDependencySha() throws IOException, NoSuchAlgorithmException {

        File unusedSha = createFileIn(getLicensesDir(project), "test.sha1", "");
        task.updateShas();

        assertFalse(unusedSha.exists());
    }

    @Test
    public void whenDependencyExistsButShaNotThenShouldCreateNewShaFile() throws IOException, NoSuchAlgorithmException {
        task.getParentTask().setDependencies(project.files(dummyJar));

        getLicensesDir(project).mkdir();
        task.updateShas();

        Path sha = Files.list(getLicensesDir(project).toPath()).findFirst().get();

        assertTrue(sha.toFile().getName().startsWith(DEP_NAME));
    }

    @Test
    public void whenDependencyAndWrongShaExistsThenShouldNotOverwriteShaFile() throws IOException, NoSuchAlgorithmException {
        task.getParentTask().setDependencies(project.files(dummyJar));

        String shaName = DEP_JAR + ".sha1";

        File sha = createFileIn(getLicensesDir(project), shaName, "content");
        task.updateShas();

        assertThat(FileUtils.readFileToString(sha), equalTo("content"));
    }

    @Test
    public void whenLicensesDirDoesntExistThenShouldThrowException() throws IOException, NoSuchAlgorithmException {
        expectedException.expect(GradleException.class);
        expectedException.expectMessage(containsString("isn't a valid directory"));

        task.updateShas();
    }

    private Project createProject() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply(JavaPlugin.class);

        return project;
    }

    private File getLicensesDir(Project project) {
        return getFile(project, "licenses");
    }

    private File getFile(Project project, String fileName) {
        return project.getProjectDir().toPath().resolve(fileName).toFile();
    }

    private File createFileIn(File parent, String name, String content) throws IOException {
        parent.mkdir();

        Path path = parent.toPath().resolve(name);
        File file = path.toFile();

        Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

        return file;
    }

    private UpdateShasTask createUpdateShasTask(Project project) {
        UpdateShasTask task = project.getTasks().register("updateShas", UpdateShasTask.class).get();

        task.setParentTask(createDependencyLicensesTask(project));
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
