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

import com.io7m.boxwood.api.EPUBPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An EPUB container.
 */

public final class EPUBContainer
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EPUBContainer.class);

  private final List<EPUBPackage> epubPackages;

  private EPUBContainer(
    final List<EPUBPackage> inEpubPackages)
  {
    this.epubPackages =
      Objects.requireNonNull(inEpubPackages, "epubPackages");
  }

  /**
   * Create an EPUB container.
   *
   * @param stream     The input stream
   * @param errors     The error consumer
   * @param sourceFile The source file name
   * @param subFiles   The sub file retriever
   *
   * @return An EPUB container
   *
   * @throws IOException      On I/O errors
   * @throws EPUBXMLException On XML parser errors
   */

  public static EPUBContainer create(
    final EPUBErrorLogger errors,
    final EPUBSubFileRetrieverType subFiles,
    final URI sourceFile,
    final InputStream stream)
    throws IOException, EPUBXMLException
  {
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(subFiles, "subFiles");
    Objects.requireNonNull(sourceFile, "sourceFile");

    try {
      final var document = EPUBPositionalXML.readXML(sourceFile, stream);
      final var root = document.getDocumentElement();
      final var rootName = root.getNodeName();
      if (!Objects.equals(rootName, "container")) {
        errors.formattedXMLError(
          root, "epub.error.container.rootNotContainer", rootName
        );
        throw new EPUBXMLExceptionMissingElement();
      }

      final var rootFiles =
        EPUBXMLHelpers.requireChildNode(errors, root, "rootfiles");
      if (rootFiles == null) {
        throw new EPUBXMLExceptionMissingElement();
      }

      final var rootFileList =
        EPUBXMLHelpers.requireChildNodes(errors, rootFiles, "rootfile");
      if (rootFileList.isEmpty()) {
        throw new EPUBXMLExceptionMissingElement();
      }

      final var epubPackages = new ArrayList<EPUBPackage>(rootFileList.size());
      for (final var rootFile : rootFileList) {
        if (!rootFile.hasAttribute("full-path")) {
          errors.formattedXMLError(
            rootFile,
            "epub.error.xml.requireAttribute",
            "full-path",
            rootFile.getTagName()
          );
          continue;
        }

        final var subFileName = rootFile.getAttribute("full-path");
        try (var subStream = subFiles.retrieveSubFile(subFileName)) {
          epubPackages.add(
            EPUBPackageParser.parse(
              errors,
              sourceFile,
              subFileName,
              subStream
            )
          );
        } catch (final FileNotFoundException e) {
          errors.formattedXMLError(
            rootFile,
            "epub.error.container.rootFileNonexistent",
            subFileName
          );
        }
      }

      return new EPUBContainer(List.copyOf(epubPackages));
    } catch (final SAXParseException e) {
      LOG.debug("parse exception: ", e);
      errors.exceptionError(e);
      throw new EPUBXMLExceptionParsing();
    } catch (final SAXException e) {
      LOG.debug("parse exception: ", e);
      errors.exceptionError(e);
      throw new EPUBXMLExceptionParsing();
    } catch (final ParserConfigurationException e) {
      LOG.debug("parser exception: ", e);
      throw new IllegalStateException(e);
    }
  }

  /**
   * @return The list of packages in the EPUB
   */

  public List<EPUBPackage> packages()
  {
    return this.epubPackages;
  }
}
