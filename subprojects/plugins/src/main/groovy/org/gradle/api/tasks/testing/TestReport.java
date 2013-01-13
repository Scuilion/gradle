/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.tasks.testing;

import org.gradle.api.DefaultTask;
import org.gradle.api.Incubating;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.internal.tasks.testing.junit.report.DefaultTestReport;
import org.gradle.api.internal.tasks.testing.junit.result.AggregateTestResultsProvider;
import org.gradle.api.internal.tasks.testing.junit.result.TestResultsProvider;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates an HTML test report from the results of one or more {@link Test} tasks.
 */
@Incubating
public class TestReport extends DefaultTask {
    private File destinationDir;
    private List<Object> results = new ArrayList<Object>();

    /**
     * Returns the directory to write the HTML report to.
     */
    @OutputDirectory
    public File getDestinationDir() {
        return destinationDir;
    }

    /**
     * Sets the directory to write the HTML report to.
     */
    public void setDestinationDir(File destinationDir) {
        this.destinationDir = destinationDir;
    }

    /**
     * Returns the set of binary test results to include in the report.
     */
    @InputFiles @SkipWhenEmpty
    public FileCollection getTestResultDirs() {
        UnionFileCollection dirs = new UnionFileCollection();
        for (Object result : results) {
            addTo(result, dirs);
        }
        return dirs;
    }

    private void addTo(Object result, UnionFileCollection dirs) {
        if (result instanceof Test) {
            Test test = (Test) result;
            dirs.add(getProject().files(test.getBinResultsDir()).builtBy(test));
        } else if (result instanceof Iterable<?>) {
            Iterable<?> iterable = (Iterable<?>) result;
            for (Object nested : iterable) {
                addTo(nested, dirs);
            }
        } else {
            dirs.add(getProject().files(result));
        }
    }

    /**
     * Sets the binary test results to use to include in the report.
     */
    public void setTestResultDirs(Iterable<File> testResultDirs) {
        this.results.clear();
        reportOn(testResultDirs);
    }

    /**
     * Adds some results to include in the report. This method accepts any parameter with the given types:
     *
     * <ul>
     *
     * <li>A {@link Test} task instance. The results from the test task are included in the report. The test task is automatically added
     * as a dependency of this task.
     * </li>
     *
     * <li>Anything that can be converted to a set of {@code File} instances as per {@link org.gradle.api.Project#files(Object...)}. These must
     * point to the binary test results generated by a {@link Test} task instance.
     * </li>
     *
     * <li>A collection. The contents of the collection are converted recursively.</li>
     *
     * </ul>
     *
     * @param results The result objects.
     */
    public void reportOn(Object... results) {
        for (Object result : results) {
            this.results.add(result);
        }
    }

    @TaskAction
    void generateReport() {
        TestResultsProvider resultsProvider = new AggregateTestResultsProvider(getTestResultDirs().getFiles());
        DefaultTestReport testReport = new DefaultTestReport();
        testReport.generateReport(resultsProvider, getDestinationDir());
    }
}
