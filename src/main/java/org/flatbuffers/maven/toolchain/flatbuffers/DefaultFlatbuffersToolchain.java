package org.flatbuffers.maven.toolchain.flatbuffers;

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

import org.apache.maven.toolchain.DefaultToolchain;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * Based on {@code org.apache.maven.toolchain.java.DefaultJavaToolChain}.
 *
 * @since 0.2.0
 */
public class DefaultFlatbuffersToolchain extends DefaultToolchain implements FlatbuffersToolchain {

    public static final String KEY_FLATC_EXECUTABLE = "flatcExecutable";
    public static final String KEY_REFLECTION_FBS = "reflectionFbs";

    protected DefaultFlatbuffersToolchain(ToolchainModel model, Logger logger) {
        super(model, "flatbuffers", logger);
    }

    private String flatcExecutable;

    private String reflectionFbs;

    @Override
    public String getReflectionFbs() {
        return reflectionFbs;
    }

    @Override
    public void setReflectionFbs(String reflectionFbs) {
        this.reflectionFbs = reflectionFbs;
    }

    @Override
    public String findTool(String toolName) {
        if ("flatc".equals(toolName)) {
            File protoc = new File(FileUtils.normalize(getFlatcExecutable()));
            if (protoc.exists()) {
                return protoc.getAbsolutePath();
            }
        }
        return null;
    }

    @Override
    public String getFlatcExecutable() {
        return this.flatcExecutable;
    }

    @Override
    public void setFlatcExecutable(String flatcExecutable) {
        this.flatcExecutable = flatcExecutable;
    }

    @Override
    public String toString() {
        return "FLATC[" + getFlatcExecutable() + "]";
    }
}
