/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.boxwood.tests;

import com.io7m.boxwood.api.EPUBManifest;
import com.io7m.boxwood.api.EPUBManifestItem;
import com.io7m.boxwood.api.EPUBMetadata;
import com.io7m.boxwood.api.EPUBMetadataProperty;
import com.io7m.boxwood.api.EPUBPackage;
import com.io7m.boxwood.api.EPUBSpine;
import com.io7m.boxwood.api.EPUBSpineItem;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EPUBBruteForceEqualityTest
{
  private static final Set<Class<?>> NO_COPY_CLASSES = Set.of(
    EPUBPackage.class
  );
  private static final Class<?> COLLECTION_CLASSES[] = {
    EPUBManifest.class,
    EPUBManifestItem.class,
    EPUBMetadata.class,
    EPUBMetadataProperty.class,
    EPUBPackage.class,
    EPUBSpine.class,
    EPUBSpineItem.class
  };
  private static final Class<?> OTHER_CLASSES[] = {

  };

  private static void checkClassEquality(
    final Class<?> clazz)
  {
    final var fields =
      Stream.of(clazz.getDeclaredFields())
        .filter(EPUBBruteForceEqualityTest::fieldIsOK)
        .map(Field::getName)
        .collect(Collectors.toList());

    final var fieldNames = new String[fields.size()];
    fields.toArray(fieldNames);

    EqualsVerifier.forClass(clazz)
      .withNonnullFields(fieldNames)
      .verify();
  }

  private static final class SensibleAnswers implements Answer<Object>
  {
    @Override
    public Object answer(final InvocationOnMock invocation)
      throws Throwable
    {
      final var returnType = invocation.getMethod().getReturnType();
      if (returnType.equals(String.class)) {
        return "xyz";
      }
      if (returnType.equals(URI.class)) {
        return URI.create("xyz");
      }
      if (returnType.equals(UUID.class)) {
        return UUID.fromString("3f8cf194-2de5-4ef6-a9ae-586303f83e0f");
      }
      return Mockito.RETURNS_DEFAULTS.answer(invocation);
    }
  }

  private static void checkCopyOf(
    final Class<?> clazz)
    throws Exception
  {
    final var interfaceType = clazz.getInterfaces()[0];
    final var mock = Mockito.mock(interfaceType, new SensibleAnswers());
    final var copyMethod = clazz.getMethod("copyOf", interfaceType);
    final var copy = copyMethod.invoke(clazz, mock);
    Assertions.assertTrue(interfaceType.isAssignableFrom(copy.getClass()));
  }

  private static boolean fieldIsOK(
    final Field field)
  {
    if (Objects.equals(field.getName(), "$jacocoData")) {
      return false;
    }

    return !field.getType().isPrimitive();
  }

  @TestFactory
  public Stream<DynamicTest> testEquals()
  {
    final var collectionClasses =
      Stream.of(COLLECTION_CLASSES);
    final var otherClasses =
      Stream.of(OTHER_CLASSES);
    final var allClasses =
      Stream.concat(collectionClasses, otherClasses);

    return allClasses
      .map(clazz -> DynamicTest.dynamicTest(
        "testEquals" + clazz.getSimpleName(),
        () -> checkClassEquality(clazz)));
  }

  @TestFactory
  public Stream<DynamicTest> testCopyOf()
  {
    final var collectionClasses =
      Stream.of(COLLECTION_CLASSES);
    final var otherClasses =
      Stream.of(OTHER_CLASSES);
    final var allClasses =
      Stream.concat(collectionClasses, otherClasses);

    return allClasses
      .filter(c -> !NO_COPY_CLASSES.contains(c))
      .map(clazz -> DynamicTest.dynamicTest(
        "testCopyOf" + clazz.getSimpleName(),
        () -> checkCopyOf(clazz)));
  }
}
