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

package com.io7m.boxwood.api;

import com.io7m.junreachable.UnreachableCodeException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An EPUB file.
 */

public interface EPUBType extends Closeable
{
  /**
   * The non-empty list of packages present within the EPUB.
   *
   * @return The list of packages within the EPUB container
   */

  List<EPUBPackage> packages();

  /**
   * Open a file from the container.
   *
   * @param name The file name
   *
   * @return A stream representing the file
   *
   * @throws NoSuchFileException If the specified file does not exist
   * @throws IOException         On I/O errors
   */

  InputStream openFile(String name)
    throws IOException, NoSuchFileException;

  /**
   * @return The unique identifier of the EPUB
   */

  default String uniqueIdentifier()
  {
    for (final var epubPackage : this.packages()) {
      return epubPackage.uniqueIdentifier();
    }
    throw new UnreachableCodeException();
  }

  /**
   * @return A UUID derived from the EPUB unique identifier
   */

  default UUID uuid()
  {
    return UUID.nameUUIDFromBytes(
      this.uniqueIdentifier().getBytes(StandardCharsets.UTF_8)
    );
  }

  /**
   * @return The cover image for the EPUB, if any package defines one
   */

  default Optional<EPUBManifestItem> coverImage()
  {
    for (final var epubPackage : this.packages()) {
      final Optional<EPUBManifestItem> coverOpt = epubPackage.coverImage();
      if (coverOpt.isPresent()) {
        return coverOpt;
      }
    }
    return Optional.empty();
  }
}
