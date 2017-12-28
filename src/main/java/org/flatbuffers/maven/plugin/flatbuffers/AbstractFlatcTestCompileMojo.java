package org.flatbuffers.maven.plugin.flatbuffers;

/*
 * Copyright (c) 2016 Maven Flatbuffers Plugin Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

/**
 * An abstract base mojo configuration for using {@code flatc} compiler with the test sources.
 *
 * @since 0.3.3
 */
public abstract class AbstractFlatcTestCompileMojo extends AbstractFlatcMojo {

    /**
     * The source directories containing the test {@code .fbs} definitions to be compiled.
     */
    @Parameter(
            required = true,
            defaultValue = "${basedir}/src/test/flatbuffers"
    )
    private File fbsTestSourceRoot;

    /**
     * This is the directory into which the (optional) descriptor set file will be created.
     *
     * @since 0.3.0
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-test-resources/flatbuffers/descriptor-sets"
    )
    private File descriptorSetOutputDirectory;

    /**
     * If generated descriptor set is to be attached to the build, specifies an optional classifier.
     *
     * @since 0.4.1
     */
    @Parameter(
            required = false,
            defaultValue = "test"
    )
    protected String descriptorSetClassifier;

    @Override
    protected void doAttachFbsSources() {
        projectHelper.addTestResource(project, getFbsSourceRoot().getAbsolutePath(),
                ImmutableList.copyOf(getIncludes()), ImmutableList.copyOf(getExcludes()));
    }

    @Override
    protected void doAttachGeneratedFiles() {
        final File outputDirectory = getSchemaOutputDirectory();
        project.addTestCompileSourceRoot(outputDirectory.getAbsolutePath());
        if (writeBinarySchema) {
//            final File descriptorSetFile = new File(getDescriptorSetOutputDirectory(), descriptorSetFileName);
//            projectHelper.attachArtifact(project, "test-fbbin", descriptorSetClassifier, descriptorSetFile);
        }
        buildContext.refresh(outputDirectory);
    }

    @Override
    protected List<Artifact> getDependencyArtifacts() {
        return project.getTestArtifacts();
    }

    @Override
    protected File getFbsSourceRoot() {
        return fbsTestSourceRoot;
    }
}
