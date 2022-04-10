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

package com.io7m.boxwood.vanilla.internal;

import com.io7m.boxwood.api.EPUBType;
import com.io7m.boxwood.parser.api.EPUBParseError;
import com.io7m.boxwood.parser.api.EPUBParseEvent;
import com.io7m.boxwood.parser.api.EPUBParseRequest;
import com.io7m.boxwood.parser.api.EPUBParserType;
import com.io7m.boxwood.vanilla.EPUBStringsType;
import com.io7m.jlexing.core.LexicalPosition;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The EPUB parser implementation.
 */

public final class EPUBParser implements EPUBParserType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EPUBParser.class);

  private static final String FILE_CONTAINER_XML =
    "META-INF/container.xml";

  private final ArrayList<EPUBParseError> errors;
  private final EPUBStringsType strings;
  private final EPUBErrorLogger errorLogger;
  private final EPUBParseRequest request;
  private EPUBContainer container;

  /**
   * The EPUB parser implementation.
   *
   * @param inRequest The parse request
   * @param inStrings The string resources
   */

  public EPUBParser(
    final EPUBStringsType inStrings,
    final EPUBParseRequest inRequest)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.request =
      Objects.requireNonNull(inRequest, "request");

    this.errors = new ArrayList<>();
    this.errorLogger = new EPUBErrorLogger(this.strings, this.errors::add);
  }

  private static InputStream lookupFileOrException(
    final ZipFile file,
    final String name)
    throws IOException
  {
    final var entry = file.getEntry(name);
    if (entry == null) {
      throw new FileNotFoundException(name);
    }
    return file.getInputStream(entry);
  }

  @Override
  public List<EPUBParseError> errors()
  {
    return this.errors;
  }

  @Override
  public Optional<EPUBType> execute()
    throws IOException
  {
    this.errors.clear();

    this.request.events().accept(
      EPUBParseEvent.builder()
        .setProgress(0.0)
        .setMessage(this.strings.format(
          "epub.parse.starting",
          this.request.uri()))
        .build()
    );

    try {
      final ZipFile file;
      try {
        file = new ZipFile(this.request.channel());
      } catch (final IOException e) {
        this.errorLogger.setSource(this.request.uri());
        this.errorLogger.exceptionError(e);
        return Optional.empty();
      }

      this.findContainer(file);
      if (this.container == null) {
        file.close();
        return Optional.empty();
      }

      final var fileCount =
        this.container.packages()
          .stream()
          .mapToInt(p -> p.manifest().items().size())
          .reduce(Integer::sum)
          .orElse(1);

      var index = 0;
      for (final var epubPackage : this.container.packages()) {
        for (final var item : epubPackage.manifest().items()) {
          final var progress = (double) index / (double) fileCount;
          final var fileName = item.realPath();
          this.request.events().accept(
            EPUBParseEvent.builder()
              .setProgress(progress)
              .setMessage(this.strings.format(
                "epub.parse.checkingManifestItem",
                fileName))
              .build()
          );

          final var entry = file.getEntry(fileName);
          if (entry == null) {
            this.errorLogger.formattedError(
              this.lexicalAtFile(fileName),
              "epub.error.requiredFileMissing",
              fileName
            );
          }
          ++index;
        }
      }

      return Optional.of(new EPUB(file, this.container));
    } finally {
      this.request.events().accept(
        EPUBParseEvent.builder()
          .setProgress(1.0)
          .setMessage(this.strings.format(
            "epub.parse.finishing",
            this.request.uri()))
          .build()
      );
    }
  }

  private void findContainer(
    final ZipFile file)
    throws IOException
  {
    final var entry = file.getEntry(FILE_CONTAINER_XML);
    if (entry == null) {
      this.errorLogger.formattedError(
        this.lexicalAtFile(FILE_CONTAINER_XML),
        "epub.error.requiredFileMissing",
        FILE_CONTAINER_XML
      );
      return;
    }

    try (var stream = file.getInputStream(entry)) {
      this.container =
        EPUBContainer.create(
          this.errorLogger,
          name -> lookupFileOrException(file, name),
          this.embeddedFile(FILE_CONTAINER_XML),
          stream
        );
    } catch (final EPUBXMLException e) {
      // Logged by the container
    }
  }

  private LexicalPosition<URI> lexicalAtFile(
    final String fileName)
  {
    return LexicalPosition.of(0, 0, this.embeddedFileOpt(fileName));
  }

  private Optional<URI> embeddedFileOpt(
    final String fileName)
  {
    return Optional.of(this.embeddedFile(fileName));
  }

  private URI embeddedFile(
    final String fileName)
  {
    return URI.create(String.format("%s/%s", this.request.uri(), fileName));
  }
}
