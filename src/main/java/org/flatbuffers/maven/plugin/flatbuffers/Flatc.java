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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils.StringStreamConsumer;
import org.codehaus.plexus.util.cli.Commandline;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.newLinkedList;

/**
 * This class represents an invokable configuration of the {@code flatc} compiler.
 * The actual executable is invoked using the plexus {@link Commandline}.
 */
@Value.Immutable
abstract class Flatc {

    /**
     * Prefix for logging the debug messages.
     */
    private static final String LOG_PREFIX = "[FLATC] ";

    /**
     * Path to the {@code flatc} executable.
     */
    @Value.Parameter
    abstract String executable();

    /**
     * A set of directories in which to search for definition imports.
     */
    abstract ImmutableSet<File> fbPathElements();

    /**
     * A set of flatbuffers definitions to process.
     */
    abstract ImmutableSet<File> fbsFiles();

    /**
     * A directory into which source files will be generated.
     */
    @Nullable
    abstract File javaOutputDirectory();

    /**
     * A directory into which binary schema files will be generated.
     */
    @Nullable
    abstract File schemaOutputDirectory();

    @Value.Default
    boolean genJava() { return true; }

    @Value.Default
    boolean genGrpc() { return false; }

    @Value.Default
    boolean genSchema() { return true; }

    /**
     * A buffer to consume standard output from the {@code flatc} executable.
     */
    private final StringStreamConsumer output = new StringStreamConsumer();

    /**
     * A buffer to consume error output from the {@code flatc} executable.
     */
    private final StringStreamConsumer error = new StringStreamConsumer();

    /**
     * @return the output
     */
    public String getStdOut() {
        return output.getOutput();
    }

    /**
     * @return the error
     */
    public String getStdErr() {
        return error.getOutput();
    }

    @Value.Check
    protected void check() {
//        Preconditions.checkState(!nonEmptyNumbers().isEmpty(),
//                "'nonEmptyNumbers' should have at least one number");

        if (javaOutputDirectory() != null)
            checkArgument(
                javaOutputDirectory().isDirectory(),
                "'javaOutputDirectory' is not a directory: %s", javaOutputDirectory());

        if (schemaOutputDirectory() != null)
            checkArgument(
                schemaOutputDirectory().isDirectory(),
                "'schemaOutputDirectory' is not a directory: %s", schemaOutputDirectory());

    }

    @Value.Check
    protected void checkFbsDirectories() {
        for (File fbPathElement : fbPathElements()) {
            checkNotNull(fbPathElement);
            checkArgument (fbPathElement.isDirectory());
        }
    }

    @Value.Check
    protected void checkFilesInPath() {
        for(File fbsFile : fbsFiles()) {
            checkNotNull(fbsFile);
            checkArgument(fbsFile.isFile());
            checkArgument(fbsFile.getName().endsWith(".fbs"));
            checkFbsFileIsInFBpath(fbsFile);
        }
    }

    /**
     * Validates the internal state for consistency and completeness.
     */
    @Value.Check
    protected void validateState() {
        checkState(!fbsFiles().isEmpty());
        checkState(javaOutputDirectory() != null || schemaOutputDirectory() != null,
                "At least one of these properties must be set: " +
                        "'javaOutputDirectory', 'schemaOutputDirectory'");
    }

    protected void checkFbsFileIsInFBpath(final File fbsFile) {
        checkArgument(fbsFile.isFile());
        checkState(checkFbsFileIsInFBpathHelper(fbsFile.getParentFile()));
    }

    protected boolean checkFbsFileIsInFBpathHelper(final File directory) {
        assert directory.isDirectory();
        if (fbPathElements().contains(directory)) {
            return true;
        } else {
            final File parentDirectory = directory.getParentFile();
            return parentDirectory != null && checkFbsFileIsInFBpathHelper(parentDirectory);
        }
    }

    /**
     * Invokes the {@code flatc} compiler using the configuration specified at construction.
     *
     * @return The exit status of {@code flatc}.
     * @throws CommandLineException if command line environment cannot be set up.
     */
    public int execute(final Log log) throws CommandLineException, InterruptedException {
        final Commandline cl = new Commandline();
        cl.setExecutable(executable());
        cl.addArguments(buildFlatcCommand().toArray(new String[] {}));
        // There is a race condition in JDK that may sporadically prevent process creation on Linux
        // https://bugs.openjdk.java.net/browse/JDK-8068370
        // In order to mitigate that, retry up to 2 more times before giving up
        int attemptsLeft = 3;
        while (true) {
            try {
                return CommandLineUtils.executeCommandLine(cl, null, output, error);
            } catch (CommandLineException e) {
                if (--attemptsLeft == 0 || e.getCause() == null) {
                    throw e;
                }
                log.warn(LOG_PREFIX + "Unable to invoke flatc, will retry " + attemptsLeft + " time(s)", e);
                Thread.sleep(1000L);
            }
        }
    }

    /**
     * Creates the command line arguments.
     *
     * <p>This method has been made visible for testing only.</p>
     *
     * @return A list consisting of the executable followed by any arguments.
     */
    public ImmutableList<String> buildFlatcCommand() {
        ImmutableList.Builder<String> command = ImmutableList.builder();

        command.add("--java");

        // add include path
        for (final File fbPathElement : fbPathElements()) {
            command.add("-I", fbPathElement.getAbsolutePath());
        }
        if (javaOutputDirectory() != null) {
            command.add("-o", javaOutputDirectory().getAbsolutePath());
//
//            // For now we assume all custom plugins produce Java output
//            for (final FlatcPlugin plugin : plugins) {
//                final File pluginExecutable = plugin.getPluginExecutableFile(pluginDirectory);
//                command.add("--plugin=protoc-gen-" + plugin.getId() + '=' + pluginExecutable);
//                command.add("--" + plugin.getId() + "_out=" + javaOutputDirectory);
//            }
        }
        for (final File fbsFile : fbsFiles()) {
            command.add(fbsFile.toString());
        }
        return command.build();
    }

    /**
     * Logs execution parameters on debug level to the specified logger.
     * All log messages will be prefixed with "{@value #LOG_PREFIX}".
     *
     * @param log a logger.
     */
    public void logExecutionParameters(final Log log) {
        if (log.isDebugEnabled()) {
            log.debug(LOG_PREFIX + "Executable: ");
            log.debug(LOG_PREFIX + ' ' + executable());

            if (fbPathElements() != null && !fbPathElements().isEmpty()) {
                log.debug(LOG_PREFIX + "Flatbuffers import paths:");
                for (final File fbPathElement : fbPathElements()) {
                    log.debug(LOG_PREFIX + ' ' + fbPathElement);
                }
            }

            if (javaOutputDirectory() != null) {
                log.debug(LOG_PREFIX + "Java output directory:");
                log.debug(LOG_PREFIX + ' ' + javaOutputDirectory());

            }

            log.debug(LOG_PREFIX + "Flatbuffers descriptors:");
            for (final File fbsFile : fbsFiles()) {
                log.debug(LOG_PREFIX + ' ' + fbsFile);
            }

            final List<String> cl = buildFlatcCommand();
            if (cl != null && !cl.isEmpty()) {
                log.debug(LOG_PREFIX + "Command line options:");
                log.debug(LOG_PREFIX + Joiner.on(' ').join(cl));
            }
        }
    }
}
