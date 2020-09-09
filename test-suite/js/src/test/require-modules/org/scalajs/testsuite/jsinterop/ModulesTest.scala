/*
 * Scala.js (https://www.scala-js.org/)
 *
 * Copyright EPFL.
 *
 * Licensed under Apache License 2.0
 * (https://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package org.scalajs.testsuite.jsinterop

import scala.scalajs.js
import scala.scalajs.js.annotation._

import org.junit.Assert._
import org.junit.Test

/* This is currently hard-coded for Node.js modules in particular.
 * We are importing built-in Node.js modules, because we do not have any
 * infrastructure to load non-built-in modules. In the future, we should use
 * our own user-defined ES6 modules written in JavaScript.
 *
 * !!! This is mostly copy-pasted in `ModulesWithGlobalFallbackTest.scala` in
 * `src/test/scala/`, with a version with global fallbacks.
 */
class ModulesTest {
  import ModulesTest._

  @Test def testImportModuleItself(): Unit = {
    val qs = QueryString
    assertEquals("object", js.typeOf(qs))

    val dict = js.Dictionary("foo" -> "bar", "baz" -> "qux")

    assertEquals("foo=bar&baz=qux", qs.stringify(dict))
    assertEquals("foo:bar;baz:qux", qs.stringify(dict, ";", ":"))

    /* Potentially, this could be "optimized" by importing `stringify` as a
     * global symbol if we are emitting ES2015 modules.
     */
    assertEquals("foo=bar&baz=qux", QueryString.stringify(dict))
    assertEquals("foo:bar;baz:qux", QueryString.stringify(dict, ";", ":"))
  }

  @Test def testImportLegacyModuleItselfAsDefault(): Unit = {
    val qs = QueryStringAsDefault
    assertEquals("object", js.typeOf(qs))

    val dict = js.Dictionary("foo" -> "bar", "baz" -> "qux")

    assertEquals("foo=bar&baz=qux", qs.stringify(dict))
    assertEquals("foo:bar;baz:qux", qs.stringify(dict, ";", ":"))

    /* Potentially, this could be "optimized" by importing `stringify` as a
     * global symbol if we are emitting ES2015 modules.
     */
    assertEquals("foo=bar&baz=qux", QueryStringAsDefault.stringify(dict))
    assertEquals("foo:bar;baz:qux", QueryStringAsDefault.stringify(dict, ";", ":"))
  }

  @Test def testImportFunctionInModule(): Unit = {
    val dict = js.Dictionary("foo" -> "bar", "baz" -> "qux")

    assertEquals("foo=bar&baz=qux", QueryStringWithNativeDef.stringify(dict))
    assertEquals("foo:bar;baz:qux", QueryStringWithNativeDef.stringify(dict, ";", ":"))
  }

  @Test def testImportFieldInModule(): Unit = {
    assertEquals("string", js.typeOf(OSWithNativeVal.EOL))
    assertEquals("string", js.typeOf(OSWithNativeVal.EOLAsDef))
  }

  @Test def testImportFunctionInModulePackageObject(): Unit = {
    val dict = js.Dictionary("foo" -> "bar", "baz" -> "qux")

    assertEquals("foo=bar&baz=qux", modulestestpackageobject.stringify(dict))
    assertEquals("foo:bar;baz:qux", modulestestpackageobject.stringify(dict, ";", ":"))
  }

  @Test def testImportFieldInModulePackageObject(): Unit = {
    assertEquals("string", js.typeOf(modulestestpackageobject.EOL))
    assertEquals("string", js.typeOf(modulestestpackageobject.EOLAsDef))
  }

  @Test def testImportObjectInModule(): Unit = {
    assertTrue((Buffer: Any).isInstanceOf[js.Object])
    assertFalse(Buffer.isBuffer(5))
  }

  @Test def testImportClassInModule(): Unit = {
    val b = Buffer.alloc(5)
    for (i <- 0 until 5)
      b(i) = (i * i).toShort

    for (i <- 0 until 5)
      assertEquals(i * i, b(i).toInt)
  }

  @Test def testImportIntegrated(): Unit = {
    val b = Buffer.from(js.Array[Short](0xe3, 0x81, 0x93, 0xe3, 0x82, 0x93,
        0xe3, 0x81, 0xab, 0xe3, 0x81, 0xa1, 0xe3, 0x81, 0xaf))
    val decoder = new StringDecoder()
    assertTrue(Buffer.isBuffer(b))
    assertFalse(Buffer.isBuffer(decoder))
    assertEquals("こんにちは", decoder.write(b))
    assertEquals("", decoder.end())
  }

  // #4001
  @Test def testNoImportUnusedSuperClass(): Unit = {
    new ExistentSubClass
    ExistentSubObject
  }
}

package object modulestestpackageobject {
  @js.native
  @JSImport("querystring", "stringify")
  def stringify(obj: js.Dictionary[String], sep: String = "&",
      eq: String = "="): String = js.native

  @js.native
  @JSImport("os", "EOL")
  val EOL: String = js.native

  @js.native
  @JSImport("os", "EOL")
  def EOLAsDef: String = js.native
}

object ModulesTest {
  @js.native
  @JSImport("querystring", JSImport.Namespace)
  object QueryString extends js.Object {
    def stringify(obj: js.Dictionary[String], sep: String = "&",
        eq: String = "="): String = js.native
  }

  @js.native
  @JSImport("querystring", JSImport.Default)
  object QueryStringAsDefault extends js.Object {
    def stringify(obj: js.Dictionary[String], sep: String = "&",
        eq: String = "="): String = js.native
  }

  object QueryStringWithNativeDef {
    @js.native
    @JSImport("querystring", "stringify")
    def stringify(obj: js.Dictionary[String], sep: String = "&",
        eq: String = "="): String = js.native
  }

  object OSWithNativeVal {
    @js.native
    @JSImport("os", "EOL")
    val EOL: String = js.native

    @js.native
    @JSImport("os", "EOL")
    def EOLAsDef: String = js.native
  }

  @js.native
  @JSImport("string_decoder", "StringDecoder")
  class StringDecoder(encoding: String = "utf8") extends js.Object {
    def write(buffer: Buffer): String = js.native
    def end(buffer: Buffer): String = js.native
    def end(): String = js.native
  }

  @js.native
  @JSImport("buffer", "Buffer")
  class Buffer private[this] () extends js.typedarray.Uint8Array(0)

  // This API requires Node.js >= v5.10.0
  @js.native
  @JSImport("buffer", "Buffer")
  object Buffer extends js.Object {
    def alloc(size: Int): Buffer = js.native
    def from(array: js.Array[Short]): Buffer = js.native

    def isBuffer(x: Any): Boolean = js.native
  }

  // #4001 - Test that unused super-classes are not imported.
  @js.native
  @JSImport("non-existent", "Foo")
  class NonExistentSuperClass extends js.Object

  @js.native
  @JSImport("string_decoder", "StringDecoder")
  class ExistentSubClass extends NonExistentSuperClass

  @js.native
  @JSImport("querystring", JSImport.Namespace)
  object ExistentSubObject extends NonExistentSuperClass
}
