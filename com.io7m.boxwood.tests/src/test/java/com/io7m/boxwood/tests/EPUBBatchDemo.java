/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.boxwood.tests;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.io7m.boxwood.parser.api.EPUBParseError;
import com.io7m.boxwood.parser.api.EPUBParseEvent;
import com.io7m.boxwood.parser.api.EPUBParseRequest;
import com.io7m.boxwood.vanilla.EPUBParserFactory;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public final class EPUBBatchDemo
{
  private int progressBar;

  private EPUBBatchDemo()
  {

  }

  public void run(
    final String[] args)
    throws IOException
  {
    final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.DEBUG);

    final var directory = Paths.get(args[0]);
    final var parsers = new EPUBParserFactory();
    final var files = collectEPUBFiles(directory);

    for (final var file : files) {
      try (var channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
        final var request =
          EPUBParseRequest.builder()
            .setChannel(channel)
            .setUri(file.toUri())
            .setEvents(this::showProgress)
            .build();

        final var parser = parsers.createParser(request);
        this.progressBar = 0;

        final var result = parser.execute();
        for (final var error : parser.errors()) {
          showError(error);
        }
        result.ifPresent(epubType -> {
          try {
            System.out.printf("Epub: %s\n", epubType.uniqueIdentifier());
            System.out.printf("UUID: %s\n", epubType.uuid());

            epubType.coverImage()
              .ifPresentOrElse(
                coverFile -> {
                  System.out.printf("Cover: %s\n", coverFile);
                },
                () -> {
                  System.out.printf("No cover file.\n");
                }
              );
            epubType.close();
          } catch (final IOException e) {

          }
        });
      }
    }

    System.out.printf(
      "Processed %s epub files.\n",
      Integer.valueOf(files.size())
    );
  }

  public static void main(
    final String[] args)
    throws IOException
  {
    new EPUBBatchDemo().run(args);
  }

  private void showProgress(
    final EPUBParseEvent event)
  {
    if (event.progress() * 80 > this.progressBar) {
      System.out.printf("#");
      this.progressBar += 1;
    }

    if (event.progress() >= 1.0) {
      System.out.printf("\n");
      this.progressBar = 0;
    }
  }

  private static void showError(
    final EPUBParseError error)
  {
    System.err.println(error.show());
  }

  private static List<Path> collectEPUBFiles(
    final Path directory)
    throws IOException
  {
    try (var stream = Files.walk(directory)) {
      return stream
        .filter(file -> file.toString().toUpperCase().endsWith(".EPUB"))
        .map(Path::toAbsolutePath)
        .sorted()
        .collect(Collectors.toList());
    }
  }
}
