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

package com.io7m.boxwood.vanilla;

import com.io7m.boxwood.parser.api.EPUBParseRequest;
import com.io7m.boxwood.parser.api.EPUBParserFactoryType;
import com.io7m.boxwood.parser.api.EPUBParserType;
import com.io7m.boxwood.vanilla.internal.EPUBParser;
import com.io7m.boxwood.vanilla.internal.EPUBStrings;
import org.osgi.service.component.annotations.Component;

import java.util.Objects;

/**
 * The default parser factory implementation.
 */

@Component(service = EPUBParserFactoryType.class)
public final class EPUBParserFactory implements EPUBParserFactoryType
{
  private final EPUBStringsType strings;

  /**
   * Construct a parser factory.
   *
   * @param inStrings The custom string resources
   */

  public EPUBParserFactory(
    final EPUBStringsType inStrings)
  {
    this.strings = Objects.requireNonNull(inStrings, "inStrings");
  }

  /**
   * Construct a parser factory.
   */

  public EPUBParserFactory()
  {
    this(EPUBStrings.create());
  }

  @Override
  public EPUBParserType createParser(
    final EPUBParseRequest request)
  {
    Objects.requireNonNull(request, "request");
    return new EPUBParser(this.strings, request);
  }
}
