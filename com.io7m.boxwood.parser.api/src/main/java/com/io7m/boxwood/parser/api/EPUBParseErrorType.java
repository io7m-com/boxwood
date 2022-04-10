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

package com.io7m.boxwood.parser.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jlexing.core.LexicalType;
import org.immutables.value.Value;

import java.net.URI;
import java.util.Optional;

/**
 * A parse error encountered during EPUB parsing.
 */

@Value.Immutable
@ImmutablesStyleType
public interface EPUBParseErrorType extends LexicalType<URI>
{
  /**
   * @return The severity of the error
   */

  Severity severity();

  @Override
  LexicalPosition<URI> lexical();

  /**
   * @return The error message
   */

  String message();

  /**
   * @return The exception associated with the error, if any
   */

  Optional<Exception> exception();

  /**
   * @return A humanly-readable formatted message
   */

  default String show()
  {
    final var lexical = this.lexical();
    final var uri = lexical.file().orElse(URI.create("urn:unspecified"));
    final var line = lexical.line();
    final var column = lexical.column();

    switch (this.severity()) {
      case ERROR:
        return String.format(
          "error: %s:%s:%s: %s",
          uri,
          Integer.valueOf(line),
          Integer.valueOf(column),
          this.message()
        );
      case WARNING:
        return String.format(
          "warning: %s:%s:%s: %s",
          uri,
          Integer.valueOf(line),
          Integer.valueOf(column),
          this.message()
        );
    }
    throw new IllegalStateException();
  }

  /**
   * The severity of the parse error.
   */

  enum Severity
  {
    /**
     * The error is an error and should result in the parsed document being
     * rejected.
     */

    ERROR,

    /**
     * The error is a warning and should not result in the parsed document
     * being rejected.
     */

    WARNING
  }
}
