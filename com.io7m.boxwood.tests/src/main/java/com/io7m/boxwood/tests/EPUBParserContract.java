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

import com.io7m.boxwood.parser.api.EPUBParseError;
import com.io7m.boxwood.parser.api.EPUBParseEvent;
import com.io7m.boxwood.parser.api.EPUBParseRequest;
import com.io7m.boxwood.parser.api.EPUBParserFactoryType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

public abstract class EPUBParserContract
{
  private Path directory;
  private ArrayList<EPUBParseEvent> events;

  protected abstract Logger logger();

  protected abstract EPUBParserFactoryType parsers();

  @BeforeEach
  public void testSetup()
    throws IOException
  {
    this.directory = EPUBTestDirectories.createTempDirectory();
    this.events = new ArrayList<EPUBParseEvent>();
  }

  @Test
  public void testCharlesDickens()
    throws Exception
  {
    final var epubFile =
      EPUBTestDirectories.resourceOf(
        EPUBParserContract.class,
        this.directory,
        "charles-dickens_great-expectations.epub"
      );

    try (var channel = Files.newByteChannel(epubFile)) {
      final var request =
        EPUBParseRequest.builder()
          .setChannel(channel)
          .setUri(epubFile.toUri())
          .setEvents(this::onParseEvent)
          .build();

      final var parser = this.parsers().createParser(request);
      final var result = parser.execute();
      parser.errors().forEach(this::logError);
      try (var epub = result.get()) {
        try (var fileStream = epub.openFile("META-INF/container.xml")) {

        }
      }
      Assertions.assertEquals(0, parser.errors().size());
    }
  }

  @Test
  public void testNotAZipFile()
    throws Exception
  {
    final var epubFile =
      EPUBTestDirectories.resourceOf(
        EPUBParserContract.class,
        this.directory,
        "empty.epub"
      );

    try (var channel = Files.newByteChannel(epubFile)) {
      final var request =
        EPUBParseRequest.builder()
          .setChannel(channel)
          .setUri(epubFile.toUri())
          .setEvents(this::onParseEvent)
          .build();

      final var parser = this.parsers().createParser(request);
      final var result = parser.execute();
      parser.errors().forEach(this::logError);
      Assertions.assertEquals(Optional.empty(), result);
      Assertions.assertEquals(1, parser.errors().size());
    }
  }

  @Test
  public void testNoContainer()
    throws Exception
  {
    final var epubFile =
      EPUBTestDirectories.resourceOf(
        EPUBParserContract.class,
        this.directory,
        "no-container.epub"
      );

    try (var channel = Files.newByteChannel(epubFile)) {
      final var request =
        EPUBParseRequest.builder()
          .setChannel(channel)
          .setUri(epubFile.toUri())
          .setEvents(this::onParseEvent)
          .build();

      final var parser = this.parsers().createParser(request);
      final var result = parser.execute();
      parser.errors().forEach(this::logError);
      Assertions.assertEquals(Optional.empty(), result);
      Assertions.assertEquals(1, parser.errors().size());
    }
  }

  private void logError(
    final EPUBParseError error)
  {
    this.logger().debug("error: {}", error);
    this.logger().debug("error: {}", error.show());
  }

  private void onParseEvent(
    final EPUBParseEvent event)
  {
    this.logger().debug("event: {}", event);
    this.events.add(event);
  }
}
