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

import com.io7m.boxwood.vanilla.EPUBStringsType;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The default string provider.
 */

public final class EPUBStrings implements EPUBStringsType
{
  private final ResourceBundle resources;

  private EPUBStrings(
    final ResourceBundle inResources)
  {
    this.resources = Objects.requireNonNull(inResources, "resources");
  }

  /**
   * @return The string resources
   */

  public static EPUBStringsType create()
  {
    return new EPUBStrings(
      ResourceBundle.getBundle("com.io7m.boxwood.vanilla.internal.EPUBStrings")
    );
  }

  @Override
  public ResourceBundle resourceBundle()
  {
    return this.resources;
  }

  @Override
  public String format(
    final String id,
    final Object... args)
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(args, "args");
    return MessageFormat.format(this.resources.getString(id), args);
  }
}
