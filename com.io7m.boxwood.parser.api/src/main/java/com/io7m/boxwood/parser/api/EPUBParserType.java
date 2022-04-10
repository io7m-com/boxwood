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

import com.io7m.boxwood.api.EPUBType;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The type of parsers.
 *
 * A parser must be executed with {@link #execute()}. After the execute method
 * has been called, the {@link #errors()} method returns the parse errors
 * encountered during the most recent parser execution. If the {@link #execute()}
 * method returns a non-empty value, the returned value should be closed with
 * {@link EPUBType#close()} in order to free any internal resources.
 */

public interface EPUBParserType
{
  /**
   * @return The list of parse errors encountered during the most recent execution
   */

  List<EPUBParseError> errors();

  /**
   * Execute the parser.
   *
   * @return A parsed EPUB, if enough data was present to produce one
   *
   * @throws IOException On I/O errors
   */

  Optional<EPUBType> execute()
    throws IOException;
}
