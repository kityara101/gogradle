/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.blindpirate.gogradle.task;

import com.github.blindpirate.gogradle.crossplatform.GoBinaryManager;
import com.google.inject.Inject;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Arrays;

import static com.github.blindpirate.gogradle.task.GolangTaskContainer.INSTALL_BUILD_DEPENDENCIES_TASK_NAME;
import static com.github.blindpirate.gogradle.task.GolangTaskContainer.INSTALL_TEST_DEPENDENCIES_TASK_NAME;
import static com.github.blindpirate.gogradle.util.StringUtils.toUnixString;

public class ShowGopathGorootTask extends AbstractGolangTask {
    private static final Logger LOGGER = Logging.getLogger(ShowGopathGorootTask.class);

    @Inject
    private GoBinaryManager goBinaryManager;

    public ShowGopathGorootTask() {
        dependsOn(INSTALL_BUILD_DEPENDENCIES_TASK_NAME, INSTALL_TEST_DEPENDENCIES_TASK_NAME);
    }

    @TaskAction
    public void showGopathGoroot() {
        File projectRoot = getProject().getRootDir();
        String projectGopath = toUnixString(projectRoot.toPath().resolve(".gogradle/project_gopath").toAbsolutePath());
        String buildGopath = toUnixString(projectRoot.toPath().resolve(".gogradle/build_gopath").toAbsolutePath());
        String testGopath = toUnixString(projectRoot.toPath().resolve(".gogradle/test_gopath").toAbsolutePath());

        String gopath = String.join(File.pathSeparator, Arrays.asList(projectGopath, buildGopath, testGopath));

        LOGGER.quiet("GOPATH: {}", gopath);
        LOGGER.quiet("GOROOT: {}", toUnixString(goBinaryManager.getGoroot()));
    }
}
