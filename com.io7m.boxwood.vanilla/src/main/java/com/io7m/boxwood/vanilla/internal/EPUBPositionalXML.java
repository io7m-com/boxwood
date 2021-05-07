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

import com.io7m.jlexing.core.LexicalPosition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedList;

/**
 * An XML DOM parser that preserves lexical information.
 */

public final class EPUBPositionalXML
{
  private final static String LEXICAL_KEY = "LEXICAL";

  private EPUBPositionalXML()
  {

  }

  /**
   * Retrieve lexical information for a node.
   *
   * @param node The node
   *
   * @return The lexical information
   */

  @SuppressWarnings("unchecked")
  public static LexicalPosition<URI> lexicalOf(
    final Node node)
  {
    return (LexicalPosition<URI>) node.getUserData(LEXICAL_KEY);
  }

  /**
   * Parse a document, preserving lexical information.
   *
   * @param source The source URI
   * @param stream The input stream
   *
   * @return A parsed document
   *
   * @throws IOException                  On I/O errors
   * @throws SAXException                 On parse errors
   * @throws ParserConfigurationException On configuration errors
   */

  public static Document readXML(
    final URI source,
    final InputStream stream)
    throws IOException, SAXException, ParserConfigurationException
  {
    final var factory =
      SAXParserFactory.newInstance();
    final var parser =
      factory.newSAXParser();

    final var docBuilderFactory =
      DocumentBuilderFactory.newInstance();

    docBuilderFactory.setValidating(false);
    docBuilderFactory.setNamespaceAware(true);
    docBuilderFactory.setXIncludeAware(false);
    docBuilderFactory.setExpandEntityReferences(false);

    final var docBuilder = docBuilderFactory.newDocumentBuilder();
    final var document = docBuilder.newDocument();
    final DefaultHandler handler = new PositionalXMLHandler(document);

    final var inputSource = new InputSource(stream);
    inputSource.setSystemId(source.toString());
    parser.parse(inputSource, handler);
    return document;
  }

  private static final class PositionalXMLHandler extends DefaultHandler
  {
    private final LinkedList<Element> elementStack;
    private final StringBuilder textBuffer;
    private final Document document;
    private Locator locator;

    PositionalXMLHandler(
      final Document inDocument)
    {
      this.document = inDocument;
      this.elementStack = new LinkedList<Element>();
      this.textBuffer = new StringBuilder(128);
    }

    @Override
    public void setDocumentLocator(
      final Locator inLocator)
    {
      this.locator = inLocator;
    }

    @Override
    public void startElement(
      final String uri,
      final String localName,
      final String qName,
      final Attributes attributes)
      throws SAXException
    {
      this.addTextIfNeeded();
      final var e = this.document.createElement(qName);
      for (var index = 0; index < attributes.getLength(); ++index) {
        e.setAttribute(
          attributes.getQName(index),
          attributes.getValue(index));
      }

      e.setUserData(
        LEXICAL_KEY,
        LexicalPosition.<URI>builder()
          .setFile(URI.create(this.locator.getSystemId()))
          .setLine(this.locator.getLineNumber())
          .setColumn(this.locator.getColumnNumber())
          .build(),
        null
      );
      this.elementStack.push(e);
    }

    @Override
    public void endElement(
      final String uri,
      final String localName,
      final String qName)
    {
      this.addTextIfNeeded();
      final var closedEl = this.elementStack.pop();
      if (this.elementStack.isEmpty()) {
        this.document.appendChild(closedEl);
      } else {
        final var parentEl = this.elementStack.peek();
        parentEl.appendChild(closedEl);
      }
    }

    @Override
    public void characters(
      final char[] ch,
      final int start,
      final int length)
      throws SAXException
    {
      this.textBuffer.append(ch, start, length);
    }

    // Outputs text accumulated under the current node
    private void addTextIfNeeded()
    {
      if (this.textBuffer.length() > 0) {
        final var el = this.elementStack.peek();
        final Node textNode = this.document.createTextNode(this.textBuffer.toString());
        el.appendChild(textNode);
        this.textBuffer.delete(0, this.textBuffer.length());
      }
    }
  }
}
