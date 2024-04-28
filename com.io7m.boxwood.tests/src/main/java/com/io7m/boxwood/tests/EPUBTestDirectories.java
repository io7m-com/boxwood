/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.io7m.boxwood.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;

public final class EPUBTestDirectories
{
  private static final Logger LOGGER =
    LoggerFactory.getLogger(EPUBTestDirectories.class);

  private EPUBTestDirectories()
  {

  }

  public static Path createBaseDirectory()
    throws IOException
  {
    final var path =
      Path.of(System.getProperty("java.io.tmpdir")).resolve("boxwood");
    Files.createDirectories(path);
    return path;
  }

  public static Path createTempDirectory()
    throws IOException
  {
    final var path = createBaseDirectory();
    final var temp = path.resolve(UUID.randomUUID().toString());
    Files.createDirectories(temp);
    return temp;
  }

  public static Path resourceOf(
    final Class<?> clazz,
    final Path output,
    final String name)
    throws IOException
  {
    final var internal = String.format("/com/io7m/boxwood/tests/%s", name);
    final var url = clazz.getResource(internal);
    if (url == null) {
      throw new NoSuchFileException(internal);
    }

    final var target = output.resolve(name);
    LOGGER.debug("copy {} {}", name, target);

    try (var stream = url.openStream()) {
      Files.copy(stream, target);
    }
    return target;
  }

  public static InputStream resourceStreamOf(
    final Class<?> clazz,
    final Path output,
    final String name)
    throws IOException
  {
    return Files.newInputStream(resourceOf(clazz, output, name));
  }
}
