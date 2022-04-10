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

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility functions for consuming XML.
 */

public final class EPUBXMLHelpers
{
  private EPUBXMLHelpers()
  {

  }

  /**
   * Require that the given element has at least one child element with the given name.
   *
   * @param errors  The error consumer
   * @param element The element
   * @param name    The child element name
   *
   * @return A non-empty list of child elements
   *
   * @throws EPUBXMLExceptionMissingElement If no child elements have the given name
   */

  public static List<Element> requireChildNodes(
    final EPUBErrorLogger errors,
    final Element element,
    final String name)
    throws EPUBXMLExceptionMissingElement
  {
    final var nodes = element.getElementsByTagName(name);
    final int receivedCount = nodes.getLength();
    if (receivedCount < 1) {
      errors.formattedXMLError(
        element,
        "epub.error.xml.requireNodes",
        name,
        element.getTagName(),
        Integer.valueOf(receivedCount)
      );
      throw new EPUBXMLExceptionMissingElement();
    }

    final var results = new ArrayList<Element>(receivedCount);
    for (var index = 0; index < receivedCount; ++index) {
      results.add((Element) nodes.item(index));
    }
    return results;
  }

  /**
   * Require that the given element has at least one child element with the given name.
   *
   * @param errors  The error consumer
   * @param element The element
   * @param name    The child element name
   *
   * @return A child element
   *
   * @throws EPUBXMLExceptionMissingElement If no child elements have the given name
   */

  public static Element requireChildNode(
    final EPUBErrorLogger errors,
    final Element element,
    final String name)
    throws EPUBXMLExceptionMissingElement
  {
    return requireChildNodeOpt(errors, element, name)
      .orElseThrow(EPUBXMLExceptionMissingElement::new);
  }

  /**
   * Require that the given element has at least one child element with the given name.
   *
   * @param errors  The error consumer
   * @param element The element
   * @param name    The child element name
   *
   * @return A child element
   */

  public static Optional<Element> requireChildNodeOpt(
    final EPUBErrorLogger errors,
    final Element element,
    final String name)
  {
    final var nodes = element.getElementsByTagName(name);
    final int receivedCount = nodes.getLength();
    if (receivedCount != 1) {
      errors.formattedXMLError(
        element,
        "epub.error.xml.requireNode",
        name,
        element.getTagName(),
        Integer.valueOf(receivedCount)
      );
      return Optional.empty();
    }
    return Optional.of((Element) nodes.item(0));
  }
}
