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

import com.io7m.boxwood.api.EPUBManifest;
import com.io7m.boxwood.api.EPUBManifestItem;
import com.io7m.boxwood.api.EPUBMetadata;
import com.io7m.boxwood.api.EPUBMetadataLegacyProperty;
import com.io7m.boxwood.api.EPUBMetadataProperty;
import com.io7m.boxwood.api.EPUBPackage;
import com.io7m.boxwood.api.EPUBSpine;
import com.io7m.boxwood.api.EPUBSpineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EPUBPackageParser
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EPUBPackageParser.class);

  private static final Pattern WHITESPACE =
    Pattern.compile("\\s+");

  private EPUBPackageParser()
  {

  }

  public static EPUBPackage parse(
    final EPUBErrorLogger errors,
    final URI sourceFile,
    final String packageFileName,
    final InputStream stream)
    throws IOException, EPUBXMLException
  {
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(sourceFile, "sourceFile");

    try {
      final var document = EPUBPositionalXML.readXML(sourceFile, stream);
      final var root = document.getDocumentElement();
      final var rootName = root.getNodeName();
      if (!Objects.equals(rootName, "package")) {
        errors.formattedXMLError(
          root, "epub.error.package.rootNotPackage", rootName
        );
        throw new EPUBXMLExceptionMissingElement();
      }

      final var uniqueIdRef =
        requireUniqueIdRef(errors, root, rootName);
      final var metadataNodeOpt =
        EPUBXMLHelpers.requireChildNodeOpt(errors, root, "metadata");
      final var manifestNodeOpt =
        EPUBXMLHelpers.requireChildNodeOpt(errors, root, "manifest");
      final var spineNodeOpt =
        EPUBXMLHelpers.requireChildNodeOpt(errors, root, "spine");

      final var metadata =
        parseMetadata(errors, root, uniqueIdRef, metadataNodeOpt);
      final var manifest =
        parseManifest(errors, packageFileName, manifestNodeOpt);
      final var spine =
        parseSpine(errors, spineNodeOpt);

      metadataNodeOpt.orElseThrow(
        EPUBXMLExceptionMissingElement::new);
      manifestNodeOpt.orElseThrow(
        EPUBXMLExceptionMissingElement::new);
      spineNodeOpt.orElseThrow(
        EPUBXMLExceptionMissingElement::new);

      return EPUBPackage.builder()
        .setUniqueIdentifierReference(uniqueIdRef)
        .setMetadata(metadata)
        .setManifest(manifest)
        .setSpine(spine)
        .build();
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

  private static String requireUniqueIdRef(
    final EPUBErrorLogger errors,
    final Element root,
    final String rootName)
  {
    if (!root.hasAttribute("unique-identifier")) {
      errors.formattedXMLError(
        root,
        "epub.error.xml.requireAttribute",
        "unique-identifier",
        rootName
      );
      return null;
    }
    return root.getAttribute("unique-identifier");
  }

  private static EPUBSpine parseSpine(
    final EPUBErrorLogger errors,
    final Optional<Element> spineNodeOpt)
  {
    if (spineNodeOpt.isEmpty()) {
      return null;
    }

    final var spineNode = spineNodeOpt.get();
    final var childNodes = spineNode.getChildNodes();
    final var items = new ArrayList<EPUBSpineItem>(childNodes.getLength());

    for (int index = 0, size = childNodes.getLength(); index < size; ++index) {
      final var childNode = childNodes.item(index);
      if (!(childNode instanceof Element)) {
        continue;
      }

      final var elem = (Element) childNode;
      final var tagName = elem.getTagName();

      switch (tagName) {
        case "itemref": {

          /*
           * The itemref value has a number of required parameters.
           *
           * See "https://www.w3.org/publishing/epub3/epub-packages.html#sec-itemref-elem"
           */

          if (!elem.hasAttribute("idref")) {
            errors.formattedXMLError(
              elem,
              "epub.error.xml.requireAttribute",
              "idref",
              tagName
            );
            continue;
          }

          final var idRef =
            elem.getAttribute("idref");

          final var item =
            EPUBSpineItem.builder()
              .setReference(idRef)
              .build();

          LOG.trace("spine: item: {}", item);
          items.add(item);
          break;
        }

        default: {
          errors.formattedXMLError(
            elem,
            "epub.error.xml.unexpectedElement",
            "spine",
            tagName
          );
          break;
        }
      }
    }

    final var spineBuilder = EPUBSpine.builder();
    spineBuilder.setItems(items);
    return spineBuilder.build();
  }

  private static EPUBManifest parseManifest(
    final EPUBErrorLogger errors,
    final String packageFileName,
    final Optional<Element> manifestNodeOpt)
  {
    if (manifestNodeOpt.isEmpty()) {
      return null;
    }

    final var baseURI = URI.create(packageFileName);
    final var manifestNode = manifestNodeOpt.get();
    final var childNodes = manifestNode.getChildNodes();
    final var manifestItems =
      new ArrayList<EPUBManifestItem>(childNodes.getLength());

    for (int index = 0, size = childNodes.getLength(); index < size; ++index) {
      final var childNode = childNodes.item(index);
      if (!(childNode instanceof Element)) {
        continue;
      }

      final var elem = (Element) childNode;
      final var tagName = elem.getTagName();

      switch (tagName) {
        case "item": {

          /*
           * The item attribute has a number of mandatory parameters.
           *
           * See: "https://www.w3.org/publishing/epub3/epub-packages.html#sec-item-elem"
           */

          if (!elem.hasAttribute("href")) {
            errors.formattedXMLError(
              elem,
              "epub.error.xml.requireAttribute",
              "href",
              tagName
            );
            continue;
          }

          if (!elem.hasAttribute("id")) {
            errors.formattedXMLError(
              elem,
              "epub.error.xml.requireAttribute",
              "id",
              tagName
            );
            continue;
          }

          if (!elem.hasAttribute("media-type")) {
            errors.formattedXMLError(
              elem,
              "epub.error.xml.requireAttribute",
              "media-type",
              tagName
            );
            continue;
          }

          final var baseFile =
            elem.getAttribute("href");
          final var realPath =
            baseURI.resolve(baseFile).toString();
          final var idValue =
            elem.getAttribute("id");
          final var mediaType =
            elem.getAttribute("media-type");
          final var propertyList =
            spaceSeparatedListOf(elem.getAttribute("properties"));

          final var item =
            EPUBManifestItem.builder()
              .setRealPath(realPath)
              .setId(idValue)
              .setHref(baseFile)
              .setMediaType(mediaType)
              .setProperties(propertyList)
              .build();

          LOG.trace("manifest: item: {}", item);
          manifestItems.add(item);
          break;
        }

        default: {

          /*
           * No other manifest items are allowed.
           */

          errors.formattedXMLError(
            elem,
            "epub.error.xml.unexpectedElement",
            "manifest",
            tagName
          );
          break;
        }
      }
    }

    final var manifestBuilder = EPUBManifest.builder();
    manifestBuilder.setItems(manifestItems);
    return manifestBuilder.build();
  }

  private static Iterable<String> spaceSeparatedListOf(
    final String properties)
  {
    if (properties == null) {
      return List.of();
    }

    return Stream.of(WHITESPACE.split(properties))
      .map(String::trim)
      .collect(Collectors.toList());
  }

  private static EPUBMetadata parseMetadata(
    final EPUBErrorLogger errors,
    final Element packageNode,
    final String uniqueIdRef,
    final Optional<Element> metadataNodeOpt)
  {
    if (metadataNodeOpt.isEmpty()) {
      return null;
    }

    final var metadataNode = metadataNodeOpt.get();
    final var childNodes = metadataNode.getChildNodes();

    final var legacyProperties =
      new ArrayList<EPUBMetadataLegacyProperty>(childNodes.getLength());
    final var properties =
      new ArrayList<EPUBMetadataProperty>(childNodes.getLength());

    for (int index = 0, size = childNodes.getLength(); index < size; ++index) {
      final var childNode = childNodes.item(index);
      if (!(childNode instanceof Element)) {
        continue;
      }

      final var elem = (Element) childNode;
      final var tagName = elem.getTagName();

      switch (tagName) {
        case "meta": {

          /*
           * The meta property might be a legacy meta property.
           *
           * See "http://idpf.org/epub/20/spec/OPF_2.0.1_draft.htm#Section2.2"
           */

          if (elem.hasAttribute("content")
            && elem.hasAttribute("name")) {
            legacyProperties.add(
              EPUBMetadataLegacyProperty.builder()
                .setName(elem.getAttribute("name").trim())
                .setContent(elem.getAttribute("content").trim())
                .build()
            );
            continue;
          }

          /*
           * Otherwise, the meta property has a required attribute.
           */

          if (!elem.hasAttribute("property")) {
            errors.formattedXMLError(
              elem,
              "epub.error.xml.requireAttribute",
              "property",
              tagName
            );
            continue;
          }

          final var name =
            elem.getAttribute("property");
          final var refines =
            Optional.ofNullable(elem.getAttribute("refines"));
          final var id =
            Optional.ofNullable(elem.getAttribute("id"));
          final var scheme =
            Optional.ofNullable(elem.getAttribute("scheme"));
          final var propertyValue =
            elem.getTextContent();

          final var property =
            EPUBMetadataProperty.builder()
              .setName(name)
              .setRefines(refines)
              .setId(id)
              .setScheme(scheme)
              .setValue(propertyValue)
              .build();

          LOG.trace("metadata: property: {}", property);
          properties.add(property);
          break;
        }

        case "link": {
          break;
        }

        default: {

          /*
           * If it's not a link and not a meta property, then behave as if
           * the property is a Dublin Core property, and ingest the various
           * bits of data into the property.
           */

          final var idProp = Optional.ofNullable(elem.getAttribute("id"));
          final var propertyBuilder = EPUBMetadataProperty.builder();
          propertyBuilder.setName(tagName);
          propertyBuilder.setId(idProp);
          propertyBuilder.setValue(elem.getTextContent());
          final var property = propertyBuilder.build();
          LOG.trace("metadata: property: {}", property);
          properties.add(property);
          break;
        }
      }
    }

    /*
     * If the unique-identifier attribute specifies a metadata property
     * that doesn't exist, then fail the metadata as a whole (and therefore
     * the entire package).
     */

    if (!findRealUniqueIdentifierProperty(
      errors,
      packageNode,
      uniqueIdRef,
      properties)) {
      return null;
    }

    final var metadataBuilder = EPUBMetadata.builder();
    metadataBuilder.setLegacyProperties(legacyProperties);
    metadataBuilder.setProperties(properties);
    return metadataBuilder.build();
  }

  private static boolean findRealUniqueIdentifierProperty(
    final EPUBErrorLogger errors,
    final Node packageNode,
    final String uniqueIdRef,
    final Collection<EPUBMetadataProperty> properties)
  {
    if (uniqueIdRef == null) {
      return false;
    }

    final var uniqueIdRefOpt = Optional.of(uniqueIdRef);
    final var uniqueIdPropertyOpt =
      properties.stream()
        .filter(property -> Objects.equals(property.id(), uniqueIdRefOpt))
        .findFirst();

    if (uniqueIdPropertyOpt.isEmpty()) {
      errors.formattedXMLError(
        packageNode,
        "epub.error.package.uniqueIdPropertyMissing",
        uniqueIdRef
      );
      return false;
    }
    return true;
  }
}
