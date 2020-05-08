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

package com.io7m.boxwood.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.util.Objects;
import java.util.Optional;

/**
 * An EPUB package.
 *
 * See: "https://www.w3.org/publishing/epub3/epub-packages.html#sec-package-elem"
 */

@ImmutablesStyleType
@Value.Immutable
public interface EPUBPackageType
{
  /**
   * @return The metadata section
   */

  EPUBMetadata metadata();

  /**
   * @return The manifest section
   */

  EPUBManifest manifest();

  /**
   * @return The spine section
   */

  EPUBSpine spine();

  /**
   * @return The identifier of the property that declares a unique identifier for the package
   */

  String uniqueIdentifierReference();

  /**
   * @return The unique identifier of the package
   */

  default String uniqueIdentifier()
  {
    final var refOpt = Optional.of(this.uniqueIdentifierReference());
    return this.metadata()
      .properties()
      .stream()
      .filter(property -> Objects.equals(property.id(), refOpt))
      .findFirst()
      .map(EPUBMetadataProperty::value)
      .orElseThrow(IllegalStateException::new);
  }

  /**
   * @return The cover image of the package, if one is defined
   */

  default Optional<EPUBManifestItem> coverImage()
  {
    final var coverItem =
      this.manifest()
        .items()
        .stream()
        .filter(item -> item.properties().contains("cover-image"))
        .findFirst();

    if (coverItem.isPresent()) {
      return coverItem;
    }

    final var legacyProperty =
      this.metadata()
        .legacyProperties()
        .stream()
        .filter(property -> Objects.equals(property.name(), "cover"))
        .findFirst();

    return legacyProperty.flatMap(
      property ->
        this.manifest()
          .items()
          .stream()
          .filter(p -> Objects.equals(p.id(), property.content()))
          .findFirst()
    );
  }
}
