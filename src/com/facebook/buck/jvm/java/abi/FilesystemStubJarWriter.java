/*
 * Copyright 2017-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.java.abi;

import com.facebook.buck.io.MorePaths;
import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.zip.CustomJarOutputStream;
import com.facebook.buck.zip.ZipOutputStreams;
import com.google.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import org.objectweb.asm.ClassWriter;

/** A {@link StubJarWriter} that writes to a file through {@link ProjectFilesystem}. */
class FilesystemStubJarWriter implements StubJarWriter {
  private final CustomJarOutputStream jar;
  private boolean closed = false;

  public FilesystemStubJarWriter(ProjectFilesystem filesystem, Path outputPath) throws IOException {
    Preconditions.checkState(
        !filesystem.exists(outputPath), "Output file already exists: %s)", outputPath);

    if (outputPath.getParent() != null && !filesystem.exists(outputPath.getParent())) {
      filesystem.createParentDirs(outputPath);
    }

    jar = ZipOutputStreams.newJarOutputStream(filesystem.newFileOutputStream(outputPath));
    jar.setEntryHashingEnabled(true);
    jar.getManifest().getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
  }

  @Override
  public void writeResource(Path relativePath, InputStream resourceContents) throws IOException {
    jar.writeEntry(MorePaths.pathWithUnixSeparators(relativePath), resourceContents);
  }

  @Override
  public void writeClass(Path relativePath, Consumer<ClassWriter> stubber) throws IOException {
    ClassWriter writer = new ClassWriter(0);
    stubber.accept(writer);
    try (InputStream contents = new ByteArrayInputStream(writer.toByteArray())) {
      jar.writeEntry(MorePaths.pathWithUnixSeparators(relativePath), contents);
    }
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      jar.close();
    }
    closed = true;
  }
}
