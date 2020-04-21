/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

import com.io7m.boxwood.parser.api.EPUBParseError;
import com.io7m.boxwood.vanilla.EPUBStringsType;
import com.io7m.jlexing.core.LexicalPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

import static com.io7m.boxwood.parser.api.EPUBParseErrorType.Severity.ERROR;
import static com.io7m.boxwood.parser.api.EPUBParseErrorType.Severity.WARNING;

public final class EPUBErrorLogger
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EPUBErrorLogger.class);

  private final EPUBStringsType strings;
  private final Consumer<EPUBParseError> errors;
  private URI source;

  public EPUBErrorLogger(
    final EPUBStringsType inStrings,
    final Consumer<EPUBParseError> inErrors)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.errors =
      Objects.requireNonNull(inErrors, "errors");
  }

  private void receive(
    final EPUBParseError error)
  {
    LOG.debug("error: {}", error);
    this.errors.accept(error);
  }

  public void setSource(
    final URI uri)
  {
    this.source = Objects.requireNonNull(uri, "uri");
  }

  public void formattedWarning(
    final LexicalPosition<URI> position,
    final String id,
    final Object... args)
  {
    this.receive(
      EPUBParseError.builder()
        .setSeverity(WARNING)
        .setMessage(this.strings.format(id, args))
        .setLexical(position)
        .build()
    );
  }

  public void formattedXMLWarning(
    final Node node,
    final String id,
    final Object... args)
  {
    this.formattedWarning(EPUBPositionalXML.lexicalOf(node), id, args);
  }

  public void formattedXMLError(
    final Node node,
    final String id,
    final Object... args)
  {
    this.formattedError(EPUBPositionalXML.lexicalOf(node), id, args);
  }

  public void formattedError(
    final LexicalPosition<URI> position,
    final String id,
    final Object... args)
  {
    this.receive(
      EPUBParseError.builder()
        .setSeverity(ERROR)
        .setMessage(this.strings.format(id, args))
        .setLexical(position)
        .build()
    );
  }

  public void exceptionError(
    final SAXParseException e)
  {
    this.receive(
      EPUBParseError.builder()
        .setSeverity(ERROR)
        .setMessage(e.getMessage())
        .setLexical(
          LexicalPosition.<URI>builder()
            .setLine(e.getLineNumber())
            .setColumn(e.getColumnNumber())
            .setFile(this.source)
            .build()
        ).build()
    );
  }

  public void exceptionError(
    final Exception e)
  {
    this.receive(
      EPUBParseError.builder()
        .setSeverity(ERROR)
        .setMessage(e.getMessage())
        .setLexical(
          LexicalPosition.<URI>builder()
            .setLine(0)
            .setColumn(0)
            .setFile(this.source)
            .build()
        ).build()
    );
  }
}
