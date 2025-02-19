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
import scala.scalajs.js.Dynamic.global

import org.scalajs.testsuite.utils.AssertThrows.assertThrows
import org.scalajs.testsuite.utils.JSAssert._
import org.scalajs.testsuite.utils.JSUtils
import org.scalajs.testsuite.utils.Platform._

import scala.annotation.meta

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.{global => globalEc}

import org.junit.Assert._
import org.junit.Assume._
import org.junit.Test

import org.scalajs.junit.async._

class ExportsTest {

  /** The namespace in which top-level exports are stored. */
  private lazy val exportsNamespace: Future[js.Dynamic] =
    ExportLoopback.exportsNamespace

  // @JSExport

  @Test def exportsForMethodsWithImplicitName(): Unit = {
    class Foo {
      @JSExport
      def bar(): Int = 42
      @JSExport
      def double(x: Int): Int = x*2
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(foo.bar))
    assertEquals(42, foo.bar())
    assertEquals(6, foo.double(3))
  }

  @Test def exportsForMethodsWithExplicitName(): Unit = {
    class Foo {
      @JSExport("theAnswer")
      def bar(): Int = 42
      @JSExport("doubleTheParam")
      def double(x: Int): Int = x*2
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertJSUndefined(foo.bar)
    assertEquals("function", js.typeOf(foo.theAnswer))
    assertEquals(42, foo.theAnswer())
    assertEquals(6, foo.doubleTheParam(3))
  }

  @Test def exportsForMethodsWithConstantFoldedName(): Unit = {
    class Foo {
      @JSExport(ExportNameHolder.methodName)
      def bar(): Int = 42
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertJSUndefined(foo.bar)
    assertEquals(42, foo.myMethod())
  }

  @Test def exportsForMethodsWhoseEncodedNameStartsWithDollar_Issue3219(): Unit = {
    class ExportsForMethodsWhoseEncodedNameStartsWithDollar {
      @JSExport("$a")
      def f(x: Int): Int = x + 1

      @JSExport
      def +(x: Int): Int = x + 2

      @JSExport("-")
      def plus(x: Int): Int = x + 3

      @JSExport("plus")
      def ++(x: Int): Int = x + 4
    }

    val fns = new ExportsForMethodsWhoseEncodedNameStartsWithDollar()
      .asInstanceOf[js.Dynamic]

    assertEquals(6, fns.applyDynamic("$a")(5))
    assertEquals(7, fns.applyDynamic("+")(5))
    assertEquals(8, fns.applyDynamic("-")(5))
    assertEquals(9, fns.applyDynamic("plus")(5))
  }

  @Test def exportsForProtectedMethods(): Unit = {
    class Foo {
      @JSExport
      protected def bar(): Int = 42

      @JSExport
      protected[testsuite] def foo(): Int = 100
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(foo.bar))
    assertEquals(42, foo.bar())
    assertEquals("function", js.typeOf(foo.foo))
    assertEquals(100, foo.foo())
  }

  @Test def exportsForPropertiesWithImplicitName(): Unit = {
    class Foo {
      private[this] var myY: String = "hello"
      @JSExport
      val answer: Int = 42
      @JSExport
      var x: Int = 3
      @JSExport
      def doubleX: Int = x*2
      @JSExport
      def y: String = myY + " get"
      @JSExport
      def y_=(v: String): Unit = myY = v + " set"
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("number", js.typeOf(foo.answer))
    assertEquals(42, foo.answer)
    assertEquals(3, foo.x)
    assertEquals(6, foo.doubleX)
    foo.x = 23
    assertEquals(23, foo.x)
    assertEquals(46, foo.doubleX)
    assertEquals("hello get", foo.y)
    foo.y = "world"
    assertEquals("world set get", foo.y)
  }

  @Test def exportsForPropertiesWithExplicitName(): Unit = {
    class Foo {
      private[this] var myY: String = "hello"
      @JSExport("answer")
      val answerScala: Int = 42
      @JSExport("x")
      var xScala: Int = 3
      @JSExport("doubleX")
      def doubleXScala: Int = xScala*2
      @JSExport("y")
      def yGetter: String = myY + " get"
      @JSExport("y")
      def ySetter_=(v: String): Unit = myY = v + " set"
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertJSUndefined(foo.answerScala)
    assertEquals("number", js.typeOf(foo.answer))
    assertEquals(42, foo.answer)
    assertEquals(3, foo.x)
    assertEquals(6, foo.doubleX)
    foo.x = 23
    assertEquals(23, foo.x)
    assertEquals(46, foo.doubleX)
    assertEquals("hello get", foo.y)
    foo.y = "world"
    assertEquals("world set get", foo.y)
  }

  @Test def exportsForPropertiesWhoseEncodedNameStartsWithDollar_Issue3219(): Unit = {
    class ExportsForPropertiesWhoseEncodedNameStartsWithDollar {
      @JSExport("$a")
      def f: Int = 6

      @JSExport
      def + : Int = 7 // scalastyle:ignore

      @JSExport("-")
      def plus: Int = 8

      @JSExport("plus")
      def ++ : Int = 9 // scalastyle:ignore
    }

    val fns = new ExportsForPropertiesWhoseEncodedNameStartsWithDollar()
      .asInstanceOf[js.Dynamic]

    assertEquals(6, fns.selectDynamic("$a"))
    assertEquals(7, fns.selectDynamic("+"))
    assertEquals(8, fns.selectDynamic("-"))
    assertEquals(9, fns.selectDynamic("plus"))
  }

  @Test def exportsForProtectedProperties(): Unit = {
    class Foo {
      @JSExport
      protected val x: Int = 42
      @JSExport
      protected[testsuite] val y: Int = 43
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals(42, foo.x)
    assertEquals(43, foo.y)
  }

  @Test def exportsForAbstractPropertiesInClass_Issue2513(): Unit = {
    abstract class Foo {
      @JSExport
      val x: Int
      @JSExport
      var y: Int
    }

    class Bar extends Foo {
      val x: Int = 5
      var y: Int = 6
    }

    val bar = (new Bar).asInstanceOf[js.Dynamic]
    assertEquals(5, bar.x)
    assertEquals(6, bar.y)
    bar.y = 7
    assertEquals(7, bar.y)
  }

  @Test def exportsForAbstractPropertiesInTrait_Issue2513(): Unit = {
    trait Foo {
      @JSExport
      val x: Int
      @JSExport
      var y: Int
    }

    class Bar extends Foo {
      val x: Int = 5
      var y: Int = 6
    }

    val bar = (new Bar).asInstanceOf[js.Dynamic]
    assertEquals(5, bar.x)
    assertEquals(6, bar.y)
    bar.y = 7
    assertEquals(7, bar.y)
  }

  @Test def readonlyProperties(): Unit = {
    class Foo {
      @JSExport
      val foo: Int = 1
      @JSExport
      def bar: Int = 1
    }

    val x: js.Dynamic = (new Foo()).asInstanceOf[js.Dynamic]

    assertThrows(classOf[js.JavaScriptException], {
      x.foo = 2
    })
    assertThrows(classOf[js.JavaScriptException], {
      x.bar = 2
    })

    // Read the properties to trick GCC into not discarding the writes.
    assertEquals(1, x.foo)
    assertEquals(1, x.bar)
  }

  @Test def propertiesAreNotEnumerable(): Unit = {
    class Foo {
      @JSExport
      def myProp: Int = 1
    }

    val x: js.Any = (new Foo()).asInstanceOf[js.Any]
    assertFalse(js.Object.properties(x).contains("myProp"))
  }

  @Test def overloadedExportsForMethods(): Unit = {
    class Foo {
      @JSExport("foobar")
      def foo(): Int = 42
      @JSExport("foobar")
      def bar(x: Int): Int = x*2
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(foo.foobar))
    assertEquals(42, foo.foobar())
    assertEquals(6, foo.foobar(3))
  }

  @Test def multipleExportsForTheSameMethod(): Unit = {
    class Foo {
      @JSExport
      @JSExport("b")
      @JSExport("c")
      def a(): Int = 1
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(foo.a))
    assertEquals("function", js.typeOf(foo.b))
    assertEquals("function", js.typeOf(foo.c))

    assertEquals(1, foo.a())
    assertEquals(1, foo.b())
    assertEquals(1, foo.c())
  }

  @Test def inheritExportsFromTraits(): Unit = {
    trait Foo {
      @JSExport
      def x: Int

      @JSExport
      def y: Int = 42

      @JSExport
      def method(x: Int): Int

      @JSExport
      def otherMethod(x: Int): Int = 3 * x
    }

    class Bar extends Foo {
      val x = 1
      def method(x: Int): Int = 2 * x
    }

    val bar = (new Bar).asInstanceOf[js.Dynamic]
    assertEquals(1, bar.x)
    assertEquals(42, bar.y)
    assertEquals("function", js.typeOf(bar.method))
    assertEquals(4, bar.method(2))
    assertEquals("function", js.typeOf(bar.otherMethod))
    assertEquals(6, bar.otherMethod(2))
  }

  @Test def inheritExportsFromTraitsWithValueClasses(): Unit = {
    trait Foo {
      @JSExport
      def x: SomeValueClass = new SomeValueClass(5)

      @JSExport
      def method(x: SomeValueClass): Int = x.i
    }

    class Bar extends Foo

    val bar = (new Bar).asInstanceOf[js.Dynamic]
    assertEquals(new SomeValueClass(5), bar.x)
    val vc = new SomeValueClass(4)
    assertEquals(4, bar.method(vc.asInstanceOf[js.Any]))
  }

  @Test def inheritExportsFromTraitsWithVarargs_Issue3538(): Unit = {
    trait Foo {
      @JSExport
      def method(args: Int*): Int = args.sum
    }

    class Bar extends Foo

    val bar = (new Bar).asInstanceOf[js.Dynamic]
    assertEquals(18, bar.method(5, 6, 7))
  }

  @Test def overloadingWithInheritedExports(): Unit = {
    class A {
      @JSExport
      def foo(x: Int): Int = 2*x
    }

    class B extends A{
      @JSExport("foo")
      def bar(x: String): String = s"Hello $x"
    }

    val b = (new B).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(b.foo))
    assertEquals(2, b.foo(1))
    assertEquals("Hello World", b.foo("World"))
  }

  @Test def exportsForGenericMethods(): Unit = {
    class Foo {
      @JSExport
      def gen[T <: AnyRef](x: T): T = x
    }

    val x = (new Object).asInstanceOf[js.Any]

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(foo.gen))
    assertSame(x, foo.gen(x))
  }

  @Test def exportsForLambdaReturnTypes(): Unit = {
    class Foo {
      @JSExport
      def lambda(x: Int): Int => Int = (y: Int) => x + y
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(foo.lambda))

    val lambda = foo.lambda(5).asInstanceOf[Function1[Int,Int]]

    assertEquals(9, lambda(4))
  }

  @Test def exportsForMultiParameterLists(): Unit = {
    class Foo {
      @JSExport
      def multiParam(x: Int)(y: Int): Int = x + y
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(foo.multiParam))
    assertEquals(11, foo.multiParam(5,6))
  }

  @Test def exportsForDefaultArguments(): Unit = {
    class Foo {
      @JSExport
      def defArg(x: Int = 1): Int = x
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(foo.defArg))
    assertEquals(5, foo.defArg(5))
  }

  @Test def exportsForWeirdStuff(): Unit = {
    class UhOh {
      // Something no one should export
      @JSExport
      def ahem[T: Comparable](x: T)(implicit y: Int): Nothing = ???
    }

    val x = (new UhOh).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(x.ahem))
  }

  @Test def exportsWithValueClassReturnTypes(): Unit = {
    class Foo {
      @JSExport
      def vc(x: Int): SomeValueClass = new SomeValueClass(x)
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(foo.vc))

    // The result should be a boxed SomeValueClass
    val result = foo.vc(5)
    assertEquals("object", js.typeOf(result))
    assertTrue((result: Any).isInstanceOf[SomeValueClass])
    assertTrue((result: Any) == (new SomeValueClass(5)))
  }

  @Test def exportsWithAnyAsReturnType(): Unit = {
    class A
    class Foo {
      @JSExport
      def foo(switch: Boolean): Any =
        if (switch) 1 else new A
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertTrue((foo.foo(true): Any).isInstanceOf[Int])
    assertTrue((foo.foo(false): Any).isInstanceOf[A])
  }

  @Test def boxedValueClassesAsParameter(): Unit = {
    class Foo {
      @JSExport
      def vc(x: SomeValueClass): Int = x.i
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals("function", js.typeOf(foo.vc))

    // The parameter should be a boxed SomeValueClass
    val valueCls = new SomeValueClass(7)
    val result = foo.vc(valueCls.asInstanceOf[js.Any])
    assertEquals("number", js.typeOf(result))
    assertEquals(7, result)
  }

  @Test def overloadOnBoxedValueClassesAsParameters(): Unit = {
    class Foo {
      @JSExport
      def foo(x: String): Int = x.length
      @JSExport
      def foo(x: SomeValueClass): Int = x.i
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]
    val valueCls = new SomeValueClass(7)
    assertEquals(7, foo.foo(valueCls.asInstanceOf[js.Any]))
    assertEquals(5, foo.foo("hello"))
  }

  @Test def exportsForOverriddenMethodsWithRefinedReturnType(): Unit = {
    class A
    class B extends A

    class C1 {
      @JSExport
      def x: A = new A
    }

    class C2 extends C1 {
      override def x: B = new B
    }

    val c2 = (new C2).asInstanceOf[js.Dynamic]
    assertTrue((c2.x: Any).isInstanceOf[B])
  }

  @Test def exportsForMethodsWithRefinedTypesAsReturnType(): Unit = {
    class A {
      @JSExport
      def foo(x: String): js.Object with js.Dynamic =
        js.Dynamic.literal(arg = x)
    }

    val a = (new A).asInstanceOf[js.Dynamic]
    assertEquals(js.Dynamic.literal(arg = "hello").toMap, a.foo("hello").toMap)
  }

  @Test def exportsForPolytypeNullaryMethod_Issue2445(): Unit = {
    class ExportPolyTypeNullaryMethod {
      @JSExport def emptyArray[T]: js.Array[T] = js.Array()
    }

    val obj = (new ExportPolyTypeNullaryMethod).asInstanceOf[js.Dynamic]
    val a = obj.emptyArray
    assertTrue((a: Any).isInstanceOf[js.Array[_]])
    assertEquals(0, a.length)
  }

  @Test def exportsForVariableArgumentMethods_Issue393(): Unit = {
    class A {
      @JSExport
      def foo(i: String*): String = i.mkString("|")
    }

    val a = (new A).asInstanceOf[js.Dynamic]

    assertEquals("", a.foo())
    assertEquals("a|b|c", a.foo("a", "b", "c"))
    assertEquals("a|b|c|d", a.foo("a", "b", "c", "d"))
  }

  @Test def overloadInViewOfDifficultRepeatedParameterLists(): Unit = {
    class A {
      @JSExport
      def foo(a: String, b: String, i: Int, c: String): Int = 1

      @JSExport
      def foo(a: String*): Int = 2

      @JSExport
      def foo(x: Int)(a: Int*): Int = x * 100000 + a.sum
    }

    val a = (new A).asInstanceOf[js.Dynamic]

    assertEquals(2, a.foo())
    assertEquals(2, a.foo("asdf"))
    assertEquals(2, a.foo("asdf", "foo"))
    assertEquals(2, a.foo("asdf", "foo", "bar"))
    assertEquals(1, a.foo("asdf", "foo", 1, "bar"))
    assertEquals(2, a.foo("asdf", "foo", "foo", "bar"))
    assertEquals(500016, a.foo(5, 1, 2, 3, 10))
    assertEquals(100000, a.foo(1))
  }

  @Test def exportsWithDefaultArguments(): Unit = {
    class A {
      var oneCount: Int = 0
      def one: Int = {
        oneCount += 1
        1
      }
      @JSExport
      def foo(a: Int = one)(b: Int = a + one)(c: Int = b + one): Int =
        a + b + c
    }

    val a = new A
    val jsa = a.asInstanceOf[js.Dynamic]

    assertEquals(6, jsa.foo())
    assertEquals(3, a.oneCount)

    assertEquals(9, jsa.foo(2))
    assertEquals(5, a.oneCount)

    assertEquals(11, jsa.foo(2,4))
    assertEquals(6, a.oneCount)

    assertEquals(16, jsa.foo(2,4,10))
    assertEquals(6, a.oneCount)

    assertEquals(15, jsa.foo((),4,10))
    assertEquals(7, a.oneCount)

    assertEquals(10, jsa.foo((),4))
    assertEquals(9, a.oneCount)
  }

  @Test def overloadMethodsInPresenceOfDefaultParameters(): Unit = {
    class A {
      @JSExport
      def foo(a: Int)(b: Int = 5)(c: Int = 7): Int = 1000 + a + b + c

      @JSExport
      def foo(a: Int, b: String): Int = 2

      @JSExport
      def foo(a: Int, b: Int, c: String): Int = 3
    }

    val a = (new A).asInstanceOf[js.Dynamic]

    assertEquals(1013, a.foo(1))
    assertEquals(1012, a.foo(1, 4))
    assertEquals(1010, a.foo(1, 4, 5))
    assertEquals(2, a.foo(1, "foo"))
    assertEquals(3, a.foo(1, 2, "foo"))

  }

  @Test def preferOverloadsTakingUnitOverMethodsWithDefaultParameters(): Unit = {
    class A {
      @JSExport
      def foo(a: Int)(b: String = "asdf"): String = s"$a $b"

      @JSExport
      def foo(a: Int, b: Unit): String = "woot"
    }

    val a = (new A).asInstanceOf[js.Dynamic]

    assertEquals("1 asdf", a.foo(1))
    assertEquals("2 omg", a.foo(2, "omg"))
    assertEquals("woot", a.foo(1, ()))

  }

  @Test def overloadMethodsInPresenceOfDefaultParametersAndRepeatedParameters(): Unit = {
    class A {
      @JSExport
      def foo(x: Int, y: Int = 1): Int = x + y
      @JSExport
      def foo(x: String*): String = x.mkString("|")
    }

    val a = (new A).asInstanceOf[js.Dynamic]

    assertEquals(2, a.foo(1))
    assertEquals(3, a.foo(1, 2))
    assertEquals("", a.foo())
    assertEquals("foo", a.foo("foo"))
    assertEquals("foo|bar", a.foo("foo","bar"))

  }

  @Test def overloadExportsCalledToString(): Unit = {
    class A {
      override def toString(): String = "no arg"
      @JSExport
      def toString(x: Int): String = s"with arg: $x"
    }

    val a = (new A).asInstanceOf[js.Dynamic]
    assertEquals("no arg", a.applyDynamic("toString")())
    assertEquals("with arg: 1", a.applyDynamic("toString")(1))
  }

  @Test def explicitExportToString(): Unit = {
    class A {
      @JSExport("toString")
      override def toString(): String = "called"
    }

    val a = (new A).asInstanceOf[js.Dynamic]
    assertEquals("called", a.applyDynamic("toString")())
  }

  @Test def boxRepeatedParameterListsWithValueClasses(): Unit = {
    class A {
      @JSExport
      def foo(vcs: SomeValueClass*): Int = vcs.map(_.i).sum
    }

    val vc1 = new SomeValueClass(1)
    val vc2 = new SomeValueClass(2)
    val a = (new A).asInstanceOf[js.Dynamic]

    assertEquals(3, a.foo(vc1.asInstanceOf[js.Any], vc2.asInstanceOf[js.Any]))
  }

  @Test def toplevelExportsForObjects(): AsyncResult = await {
    val objFuture =
      if (isNoModule) Future.successful(global.TopLevelExportedObject)
      else exportsNamespace.map(_.TopLevelExportedObject)
    for (obj <- objFuture) yield {
      assertJSNotUndefined(obj)
      assertEquals("object", js.typeOf(obj))
      assertEquals("witness", obj.witness)
    }
  }

  @Test def toplevelExportsForScalaJSDefinedJSObjects(): AsyncResult = await {
    val obj1Future =
      if (isNoModule) Future.successful(global.SJSDefinedTopLevelExportedObject)
      else exportsNamespace.map(_.SJSDefinedTopLevelExportedObject)
    for (obj1 <- obj1Future) yield {
      assertJSNotUndefined(obj1)
      assertEquals("object", js.typeOf(obj1))
      assertEquals("witness", obj1.witness)

      assertSame(obj1, SJSDefinedExportedObject)
    }
  }

  @Test def toplevelExportsForNestedObjects(): AsyncResult = await {
    val objFuture =
      if (isNoModule) Future.successful(global.NestedExportedObject)
      else exportsNamespace.map(_.NestedExportedObject)
    for (obj <- objFuture) yield {
      assertJSNotUndefined(obj)
      assertEquals("object", js.typeOf(obj))
      assertSame(obj, ExportHolder.ExportedObject)
    }
  }

  @Test def exportsForObjectsWithConstantFoldedName(): AsyncResult = await {
    val objFuture =
      if (isNoModule) Future.successful(global.ConstantFoldedObjectExport)
      else exportsNamespace.map(_.ConstantFoldedObjectExport)
    for (obj <- objFuture) yield {
      assertJSNotUndefined(obj)
      assertEquals("object", js.typeOf(obj))
      assertEquals("witness", obj.witness)
    }
  }

  @Test def exportsForProtectedObjects(): AsyncResult = await {
    val objFuture =
      if (isNoModule) Future.successful(global.ProtectedExportedObject)
      else exportsNamespace.map(_.ProtectedExportedObject)
    for (obj <- objFuture) yield {
      assertJSNotUndefined(obj)
      assertEquals("object", js.typeOf(obj))
      assertEquals("witness", obj.witness)
    }
  }

  @Test def toplevelExportsForClasses(): AsyncResult = await {
    val constrFuture =
      if (isNoModule) Future.successful(global.TopLevelExportedClass)
      else exportsNamespace.map(_.TopLevelExportedClass)
    for (constr <- constrFuture) yield {
      assertJSNotUndefined(constr)
      assertEquals("function", js.typeOf(constr))
      val obj = js.Dynamic.newInstance(constr)(5)
      assertEquals(5, obj.x)
    }
  }

  @Test def toplevelExportsForScalaJSDefinedJSClasses(): AsyncResult = await {
    val constrFuture =
      if (isNoModule) Future.successful(global.SJSDefinedTopLevelExportedClass)
      else exportsNamespace.map(_.SJSDefinedTopLevelExportedClass)
    for (constr <- constrFuture) yield {
      assertJSNotUndefined(constr)
      assertEquals("function", js.typeOf(constr))
      val obj = js.Dynamic.newInstance(constr)(5)
      assertTrue((obj: Any).isInstanceOf[SJSDefinedTopLevelExportedClass])
      assertEquals(5, obj.x)

      assertSame(constr, js.constructorOf[SJSDefinedTopLevelExportedClass])
    }
  }

  @Test def toplevelExportsForAbstractJSClasses_Issue4117(): AsyncResult = await {
    val constrFuture =
      if (isNoModule) Future.successful(global.TopLevelExportedAbstractJSClass)
      else exportsNamespace.map(_.TopLevelExportedAbstractJSClass)

    for (constr <- constrFuture) yield {
      assertEquals("function", js.typeOf(constr))

      val body = if (useECMAScript2015Semantics) {
        """
        class SubClass extends constr {
          constructor(x) {
            super(x);
          }
          foo(y) {
             return y + this.x;
          }
        }
        return SubClass;
        """
      } else {
        """
        function SubClass(x) {
          constr.call(this, x);
        }
        SubClass.prototype = Object.create(constr.prototype);
        SubClass.prototype.foo = function(y) {
          return y + this.x;
        };
        return SubClass;
        """
      }

      val subclassFun = new js.Function("constr", body)
        .asInstanceOf[js.Function1[js.Dynamic, js.Dynamic]]
      val subclass = subclassFun(constr)
      assertEquals("function", js.typeOf(subclass))

      val obj = js.Dynamic.newInstance(subclass)(5)
        .asInstanceOf[TopLevelExportedAbstractJSClass]

      assertEquals(5, obj.x)
      assertEquals(11, obj.foo(6))
      assertEquals(33, obj.bar(6))
    }
  }

  @Test def toplevelExportsForNestedClasses(): AsyncResult = await {
    val constrFuture =
      if (isNoModule) Future.successful(global.NestedExportedClass)
      else exportsNamespace.map(_.NestedExportedClass)
    for (constr <- constrFuture) yield {
      assertJSNotUndefined(constr)
      assertEquals("function", js.typeOf(constr))
      val obj = js.Dynamic.newInstance(constr)()
      assertTrue((obj: Any).isInstanceOf[ExportHolder.ExportedClass])
    }
  }

  @Test def toplevelExportsForNestedSjsDefinedClasses(): AsyncResult = await {
    val constrFuture =
      if (isNoModule) Future.successful(global.NestedSJSDefinedExportedClass)
      else exportsNamespace.map(_.NestedSJSDefinedExportedClass)
    for (constr <- constrFuture) yield {
      assertJSNotUndefined(constr)
      assertEquals("function", js.typeOf(constr))
      val obj = js.Dynamic.newInstance(constr)()
      assertTrue((obj: Any).isInstanceOf[ExportHolder.SJSDefinedExportedClass])
    }
  }

  @Test def exportsForClassesWithConstantFoldedName(): AsyncResult = await {
    val constrFuture =
      if (isNoModule) Future.successful(global.ConstantFoldedClassExport)
      else exportsNamespace.map(_.ConstantFoldedClassExport)
    for (constr <- constrFuture) yield {
      assertJSNotUndefined(constr)
      assertEquals("function", js.typeOf(constr))
      val obj = js.Dynamic.newInstance(constr)(5)
      assertEquals(5, obj.x)
    }
  }

  @Test def exportsForProtectedClasses(): AsyncResult = await {
    val constrFuture =
      if (isNoModule) Future.successful(global.ProtectedExportedClass)
      else exportsNamespace.map(_.ProtectedExportedClass)
    for (constr <- constrFuture) yield {
      assertJSNotUndefined(constr)
      assertEquals("function", js.typeOf(constr))
      val obj = js.Dynamic.newInstance(constr)(5)
      assertEquals(5, obj.x)
    }
  }

  @Test def exportForClassesWithRepeatedParametersInCtor(): AsyncResult = await {
    val constrFuture =
      if (isNoModule) Future.successful(global.ExportedVarArgClass)
      else exportsNamespace.map(_.ExportedVarArgClass)
    for (constr <- constrFuture) yield {
      assertEquals("", js.Dynamic.newInstance(constr)().result)
      assertEquals("a", js.Dynamic.newInstance(constr)("a").result)
      assertEquals("a|b", js.Dynamic.newInstance(constr)("a", "b").result)
      assertEquals("a|b|c", js.Dynamic.newInstance(constr)("a", "b", "c").result)
      assertEquals("Number: <5>|a", js.Dynamic.newInstance(constr)(5, "a").result)
    }
  }

  @Test def exportForClassesWithDefaultParametersInCtor(): AsyncResult = await {
    val constrFuture =
      if (isNoModule) Future.successful(global.ExportedDefaultArgClass)
      else exportsNamespace.map(_.ExportedDefaultArgClass)
    for (constr <- constrFuture) yield {
      assertEquals(6, js.Dynamic.newInstance(constr)(1,2,3).result)
      assertEquals(106, js.Dynamic.newInstance(constr)(1).result)
      assertEquals(103, js.Dynamic.newInstance(constr)(1,2).result)
    }
  }

  @Test def disambiguateOverloadsInvolvingLongs(): Unit = {

    class Foo {
      @JSExport
      def foo(x: Int): Int = 1
      @JSExport
      def foo(x: Long): Int = 2
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]

    // Create a long factory we can call dynamically to retrieve an unboxed
    // long which is typed as a js.Any
    object LongFactory {
      @JSExport
      def aLong: Long = 1L
    }
    val trueJsLong = LongFactory.asInstanceOf[js.Dynamic].aLong

    assertEquals(1, foo.foo(1))
    assertEquals(2, foo.foo(trueJsLong))
  }

  @Test def returnBoxedChars(): Unit = {
    class Foo {
      @JSExport
      def bar(x: Int): Char = x.toChar
    }
    val foo = (new Foo).asInstanceOf[js.Dynamic]

    val charAsAny: Any = foo.bar(65)
    assertTrue(charAsAny.isInstanceOf[Character])
    assertEquals("A", charAsAny.toString())

    /* Do not use `assertEquals` otherwise it would re-box the Char, defeating
     * the purpose of this test.
     */
    assertTrue('A' == charAsAny.asInstanceOf[Char])
  }

  @Test def boxedCharsAsParameter(): Unit = {
    class Foo {
      @JSExport
      def bar(x: Char): Int = x.toInt
    }
    val foo = (new Foo).asInstanceOf[js.Dynamic]

    @noinline def eCharAsAny: Any = Character.valueOf('e')
    assertEquals('e'.toInt, foo.bar(eCharAsAny.asInstanceOf[js.Any]))
  }

  @Test def distinguishIntFromChar(): Unit = {
    class Foo {
      @JSExport
      def bar(x: Char): String = "char: "+x
      @JSExport
      def bar(x: Int): String = "int: "+x
    }
    val foo = (new Foo).asInstanceOf[js.Dynamic]

    @noinline def charAsAny: Any = Character.valueOf('S')
    assertEquals("char: S", foo.bar(charAsAny.asInstanceOf[js.Any]))

    @noinline def intAsAny: Any = Integer.valueOf(68)
    assertEquals("int: 68", foo.bar(intAsAny.asInstanceOf[js.Any]))
  }

  @Test def exportingConstructorParameterFields_Issue970(): Unit = {
    class Foo(@JSExport val x: Int, @JSExport var y: Int)

    val foo = new Foo(5, 6).asInstanceOf[js.Dynamic]
    assertEquals(5, foo.x)
    assertEquals(6, foo.y)
    foo.y = 7
    assertEquals(7, foo.y)
  }

  @Test def exportingCaseClassFields_Issue970(): Unit = {
    case class Bar(@JSExport x: Int, @JSExport var y: Int)

    val bar = Bar(5, 6).asInstanceOf[js.Dynamic]
    assertEquals(5, bar.x)
    assertEquals(6, bar.y)
    bar.y = 7
    assertEquals(7, bar.y)
  }

  @Test def exportingLazyValues_Issue977(): Unit = {
    class Foo {
      @JSExport
      lazy val x = 1
    }
    val foo = (new Foo).asInstanceOf[js.Dynamic]
    assertEquals(1, foo.x)
  }

  @Test def exportingAllMembersOfClass(): Unit = {
    @JSExportAll
    class Foo {
      val a = 1

      @JSExport // double annotation allowed
      def b: Int = 2

      lazy val c = 3

      class Bar // not exported, but should not fail
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]

    assertEquals(1, foo.a)
    assertEquals(2, foo.b)
    assertEquals(3, foo.c)
  }

  @Test def noExportOfSyntheticMembersWithJSExportAll_Issue1195(): Unit = {
    @JSExportAll
    case class Foo(x: Int)

    val foo = Foo(1).asInstanceOf[js.Dynamic]

    assertEquals(1, foo.x)
    assertJSUndefined(foo.copy)
  }

  @Test def multipleEquivalentJSExportAnnotations(): Unit = {
    class Foo {
      @JSExport
      @JSExport("a")
      @JSExport
      @JSExport("a")
      def b: Int = 1
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]

    assertEquals(1, foo.b)
  }

  @Test def nullForArgumentsOfPrimitiveValueType_Issue1719(): Unit = {
    @JSExportAll
    class Foo {
      def doBool(x: Boolean): Unit = assertTrue((x: Any) == false) // scalastyle:ignore
      def doChar(x: Char): Unit = assertTrue(x.equals('\u0000'))
      def doByte(x: Byte): Unit = assertEquals(0, x)
      def doShort(x: Short): Unit = assertEquals(0, x)
      def doInt(x: Int): Unit = assertEquals(0, x)
      def doLong(x: Long): Unit = assertTrue(x.equals(0L))
      def doFloat(x: Float): Unit = assertEquals(0.0f, x, 0.0)
      def doDouble(x: Double): Unit = assertEquals(0.0, x, 0.0)
      def doUnit(x: Unit): Unit = assertTrue((x: Any) == null)
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]

    foo.doBool(null)
    foo.doChar(null)
    foo.doByte(null)
    foo.doShort(null)
    foo.doInt(null)
    foo.doLong(null)
    foo.doFloat(null)
    foo.doDouble(null)
    foo.doUnit(null)
  }

  @Test def throwOnBadValuesForArgumentsOfPrimitiveValueType(): Unit = {
    assumeTrue("Assumed compliant asInstanceOf", hasCompliantAsInstanceOfs)

    @JSExportAll
    class Foo {
      def doBool(x: Boolean): Boolean = x
      def doChar(x: Char): Char = x
      def doByte(x: Byte): Byte = x
      def doShort(x: Short): Short = x
      def doInt(x: Int): Int = x
      def doLong(x: Long): Long = x
      def doFloat(x: Float): Float = x
      def doDouble(x: Double): Double = x
      def doUnit(x: Unit): Unit = x
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]

    // Class type
    assertThrows(classOf[Exception], foo.doBool(foo))
    assertThrows(classOf[Exception], foo.doChar(foo))
    assertThrows(classOf[Exception], foo.doByte(foo))
    assertThrows(classOf[Exception], foo.doShort(foo))
    assertThrows(classOf[Exception], foo.doInt(foo))
    assertThrows(classOf[Exception], foo.doLong(foo))
    assertThrows(classOf[Exception], foo.doFloat(foo))
    assertThrows(classOf[Exception], foo.doDouble(foo))
    assertThrows(classOf[Exception], foo.doUnit(foo))

    // Bad values
    assertThrows(classOf[Exception], foo.doBool(1))
    assertThrows(classOf[Exception], foo.doBool("a"))

    assertThrows(classOf[Exception], foo.doChar(1))
    assertThrows(classOf[Exception], foo.doChar("a"))

    assertThrows(classOf[Exception], foo.doByte(300))
    assertThrows(classOf[Exception], foo.doByte("a"))

    assertThrows(classOf[Exception], foo.doShort(32768))
    assertThrows(classOf[Exception], foo.doShort("a"))

    assertThrows(classOf[Exception], foo.doInt(3.2))
    assertThrows(classOf[Exception], foo.doInt("a"))

    assertThrows(classOf[Exception], foo.doLong(3.2))
    assertThrows(classOf[Exception], foo.doLong(3))
    assertThrows(classOf[Exception], foo.doLong("a"))

    assertThrows(classOf[Exception], foo.doFloat("a"))
  }

  @Test def throwOnBadValuesForArgumentsOfValueClassType_Issue613(): Unit = {
    assumeTrue("Assumed compliant asInstanceOf", hasCompliantAsInstanceOfs)

    class Foo {
      @JSExport
      def doVC(x: SomeValueClass): SomeValueClass = x
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]

    assertThrows(classOf[Exception], foo.doVC(null))
    assertThrows(classOf[Exception], foo.doVC(foo))
    assertThrows(classOf[Exception], foo.doVC(1))
    assertThrows(classOf[Exception], foo.doVC("a"))
  }

  @Test def throwOnBadValuesForArgumentsOfClassType(): Unit = {
    assumeTrue("Assumed compliant asInstanceOf", hasCompliantAsInstanceOfs)

    class A
    class B

    class Foo {
      @JSExport
      def doA(x: A): A = x
    }

    val foo = (new Foo).asInstanceOf[js.Dynamic]

    assertThrows(classOf[Exception], foo.doA(1))
    assertThrows(classOf[Exception], foo.doA((new B).asInstanceOf[js.Any]))
    assertThrows(classOf[Exception], foo.doA("a"))
  }

  private abstract class JSAbstractClass extends js.Object

  @Test def exposePublicMembersOfNewJSObject_Issue1899(): Unit = {

    // Test that the bug is fixed for js.Any classes.

    def testExposure(obj: js.Object): Unit = {
      assertJSNotUndefined(obj)
      assertTrue(obj.hasOwnProperty("x1"))
      assertTrue(obj.hasOwnProperty("y1"))
      assertFalse(obj.hasOwnProperty("x2"))
      assertFalse(obj.hasOwnProperty("y2"))
      assertFalse(obj.hasOwnProperty("x3"))
      assertFalse(obj.hasOwnProperty("y3"))

      val dynObj = obj.asInstanceOf[js.Dynamic]
      assertEquals("x1", dynObj.x1)
      assertJSUndefined(dynObj.x2)
      assertJSUndefined(dynObj.x3)

      assertEquals("y1", dynObj.y1)
      assertJSUndefined(dynObj.y2)
      assertJSUndefined(dynObj.y3)

      assertEquals("z1", dynObj.z1())
      assertJSUndefined(dynObj.z2)
      assertJSUndefined(dynObj.z2)
      assertJSUndefined(dynObj.z3)

      dynObj.y1 = "y1+"
      dynObj.y2 = "y2+"
      dynObj.y3 = "y3+"
      assertEquals("y1+", dynObj.y1)
      assertEquals("y2+", dynObj.y2)
      assertEquals("y3+", dynObj.y3)
      assertEquals("y1+", dynObj.checkOriginalY1())
      assertEquals("y2", dynObj.checkOriginalY2())
      assertEquals("y3", dynObj.checkOriginalY3())
    }

    def getJSObj(): js.Object = new js.Object {
      val x1 = "x1"
      var y1 = "y1"
      def z1() = "z1"
      private val x2 = "x2"
      private var y2 = "y2"
      private def z2() = "z2"
      private[this] val x3 = "x3"
      private[this] var y3 = "y3"
      private[this] def z3() = "z3"
      def checkOriginalY1() = y1
      def checkOriginalY2() = y2
      def checkOriginalY3() = y3
    }

    class JSClass extends js.Object

    def getJSObj2(): js.Object = new JSClass {
      val x1 = "x1"
      var y1 = "y1"
      def z1() = "z1"
      private val x2 = "x2"
      private var y2 = "y2"
      private def z2() = "z2"
      private[this] val x3 = "x3"
      private[this] var y3 = "y3"
      private[this] def z3() = "z3"
      def checkOriginalY1() = y1
      def checkOriginalY2() = y2
      def checkOriginalY3() = y3
    }

    def getJSObj3(): js.Object = new JSAbstractClass {
      val x1 = "x1"
      var y1 = "y1"
      def z1() = "z1"
      private val x2 = "x2"
      private var y2 = "y2"
      private def z2() = "z2"
      private[this] val x3 = "x3"
      private[this] var y3 = "y3"
      private[this] def z3() = "z3"
      def checkOriginalY1() = y1
      def checkOriginalY2() = y2
      def checkOriginalY3() = y3
    }

    trait JSTrait extends js.Object

    def getJSObj4(): js.Object = new JSTrait {
      val x1 = "x1"
      var y1 = "y1"
      def z1() = "z1"
      private val x2 = "x2"
      private var y2 = "y2"
      private def z2() = "z2"
      private[this] val x3 = "x3"
      private[this] var y3 = "y3"
      private[this] def z3() = "z3"
      def checkOriginalY1() = y1
      def checkOriginalY2() = y2
      def checkOriginalY3() = y3
    }

    testExposure(getJSObj())
    testExposure(getJSObj2())
    testExposure(getJSObj3())
    testExposure(getJSObj4())
  }

  // @JSExportTopLevel

  @Test def basicTopLevelExport(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)
    assertEquals(1, global.TopLevelExport_basic())
  }

  @Test def basicTopLevelExportModule(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      assertEquals(1, exp.TopLevelExport_basic())
    }
  }

  @Test def overloadedTopLevelExport(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)
    assertEquals("Hello World", global.TopLevelExport_overload("World"))
    assertEquals(2, global.TopLevelExport_overload(2))
    assertEquals(9, global.TopLevelExport_overload(2, 7))
    assertEquals(10, global.TopLevelExport_overload(1, 2, 3, 4))
  }

  @Test def overloadedTopLevelExportModule(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      assertEquals("Hello World", exp.TopLevelExport_overload("World"))
      assertEquals(2, exp.TopLevelExport_overload(2))
      assertEquals(9, exp.TopLevelExport_overload(2, 7))
      assertEquals(10, exp.TopLevelExport_overload(1, 2, 3, 4))
    }
  }

  @Test def defaultParamsTopLevelExport_Issue4052(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)
    assertEquals(7, global.TopLevelExport_defaultParams(6))
    assertEquals(11, global.TopLevelExport_defaultParams(6, 5))
  }

  @Test def defaultParamsTopLevelExportModule_Issue4052(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      assertEquals(7, exp.TopLevelExport_defaultParams(6))
      assertEquals(11, exp.TopLevelExport_defaultParams(6, 5))
    }
  }

  @Test def topLevelExportUsesUniqueObject(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)
    global.TopLevelExport_set(3)
    assertEquals(3, TopLevelExports.myVar)
    global.TopLevelExport_set(7)
    assertEquals(7, TopLevelExports.myVar)
  }

  @Test def topLevelExportUsesUniqueObjectModule(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      exp.TopLevelExport_set(3)
      assertEquals(3, TopLevelExports.myVar)
      exp.TopLevelExport_set(7)
      assertEquals(7, TopLevelExports.myVar)
    }
  }

  @Test def topLevelExportFromNestedObject(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)
    global.TopLevelExport_setNested(28)
    assertEquals(28, TopLevelExports.Nested.myVar)
  }

  @Test def topLevelExportFromNestedObjectModule(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      exp.TopLevelExport_setNested(28)
      assertEquals(28, TopLevelExports.Nested.myVar)
    }
  }

  @Test def topLevelExportWithDoubleUnderscore(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)
    assertEquals(true, global.__topLevelExportWithDoubleUnderscore)
  }

  @Test def topLevelExportWithDoubleUnderscoreModule(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      assertEquals(true, exp.__topLevelExportWithDoubleUnderscore)
    }
  }

  @Test def topLevelExportIsAlwaysReachable(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)
    assertEquals("Hello World", global.TopLevelExport_reachability())
  }

  @Test def topLevelExportIsAlwaysReachableModule(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      assertEquals("Hello World", exp.TopLevelExport_reachability())
    }
  }

  // @JSExportTopLevel fields

  @Test def topLevelExportBasicField(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)
    // Initialization
    assertEquals(5, global.TopLevelExport_basicVal)
    assertEquals("hello", global.TopLevelExport_basicVar)

    // Scala modifies var
    TopLevelFieldExports.basicVar = "modified"
    assertEquals("modified", TopLevelFieldExports.basicVar)
    assertEquals("modified", global.TopLevelExport_basicVar)

    // Reset var
    TopLevelFieldExports.basicVar = "hello"
  }

  @Test def topLevelExportBasicFieldModule(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      // Initialization
      assertEquals(5, exp.TopLevelExport_basicVal)
      assertEquals("hello", exp.TopLevelExport_basicVar)

      // Scala modifies var
      TopLevelFieldExports.basicVar = "modified"
      assertEquals("modified", TopLevelFieldExports.basicVar)
      assertEquals("modified", exp.TopLevelExport_basicVar)

      // Reset var
      TopLevelFieldExports.basicVar = "hello"
    }
  }

  @Test def topLevelExportFieldTwice(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)

    // Initialization
    assertEquals(5, global.TopLevelExport_valExportedTwice1)
    assertEquals("hello", global.TopLevelExport_varExportedTwice1)
    assertEquals("hello", global.TopLevelExport_varExportedTwice2)

    // Scala modifies var
    TopLevelFieldExports.varExportedTwice = "modified"
    assertEquals("modified", TopLevelFieldExports.varExportedTwice)
    assertEquals("modified", global.TopLevelExport_varExportedTwice1)
    assertEquals("modified", global.TopLevelExport_varExportedTwice2)

    // Reset var
    TopLevelFieldExports.varExportedTwice = "hello"
  }

  @Test def topLevelExportFieldTwiceModule(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      // Initialization
      assertEquals(5, exp.TopLevelExport_valExportedTwice1)
      assertEquals("hello", exp.TopLevelExport_varExportedTwice1)
      assertEquals("hello", exp.TopLevelExport_varExportedTwice2)

      // Scala modifies var
      TopLevelFieldExports.varExportedTwice = "modified"
      assertEquals("modified", TopLevelFieldExports.varExportedTwice)
      assertEquals("modified", exp.TopLevelExport_varExportedTwice1)
      assertEquals("modified", exp.TopLevelExport_varExportedTwice2)

      // Reset var
      TopLevelFieldExports.varExportedTwice = "hello"
    }
  }

  @Test def topLevelExportWriteValVarCausesTypeerror(): AsyncResult = await {
    assumeFalse("Unchecked in Script mode", isNoModule)

    for (exp <- exportsNamespace) yield {
      assertThrows(classOf[js.JavaScriptException], {
        exp.TopLevelExport_basicVal = 54
      })

      assertThrows(classOf[js.JavaScriptException], {
        exp.TopLevelExport_basicVar = 54
      })
    }
  }

  @Test def topLevelExportUninitializedFieldsScala(): Unit = {
    assertEquals(0, TopLevelFieldExports.uninitializedVarInt)
    assertEquals(0L, TopLevelFieldExports.uninitializedVarLong)
    assertEquals(null, TopLevelFieldExports.uninitializedVarString)
    assertEquals('\u0000', TopLevelFieldExports.uninitializedVarChar)
  }

  @Test def topLevelExportUninitializedFields(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)
    assertEquals(null, global.TopLevelExport_uninitializedVarInt)
    assertEquals(null, global.TopLevelExport_uninitializedVarLong)
    assertEquals(null, global.TopLevelExport_uninitializedVarString)
    assertEquals(null, global.TopLevelExport_uninitializedVarChar)
  }

  @Test def topLevelExportUninitializedFieldsModule(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      assertEquals(null, exp.TopLevelExport_uninitializedVarInt)
      assertEquals(null, exp.TopLevelExport_uninitializedVarLong)
      assertEquals(null, exp.TopLevelExport_uninitializedVarString)
      assertEquals(null, exp.TopLevelExport_uninitializedVarChar)
    }
  }

  @Test def topLevelExportFieldIsAlwaysReachableAndInitialized(): Unit = {
    assumeTrue("Assume NoModule", isNoModule)
    assertEquals("Hello World", global.TopLevelExport_fieldreachability)
  }

  @Test def topLevelExportFieldIsAlwaysReachableAndInitializedModule(): AsyncResult = await {
    assumeFalse("Assume Module", isNoModule)
    for (exp <- exportsNamespace) yield {
      assertEquals("Hello World", exp.TopLevelExport_fieldreachability)
    }
  }

  @Test def topLevelExportFieldIsWritableAccrossModules(): Unit = {
    /* We write to basicVar exported above from a different object to test writing
     * of static fields across module boundaries (when module splitting is
     * enabled).
     */

    assertEquals("hello", TopLevelFieldExports.inlineVar)
    TopLevelFieldExports.inlineVar = "hello modules"
    assertEquals("hello modules", TopLevelFieldExports.inlineVar)

    // Reset var
    TopLevelFieldExports.inlineVar = "hello"
  }

  // @JSExportTopLevel in Script's are `let`s in ES 2015, `var`s in ES 5.1

  @Test def topLevelExportsNoModuleAreOfCorrectKind(): Unit = {
    assumeTrue("relevant only for NoModule", isNoModule)

    val g = JSUtils.globalObject

    // Do we expect to get undefined when looking up the exports in the global object?
    val undefinedExpected = useECMAScript2015Semantics

    assertEquals(undefinedExpected, js.isUndefined(g.TopLevelExportedObject))
    assertEquals(undefinedExpected, js.isUndefined(g.SJSDefinedTopLevelExportedObject))
    assertEquals(undefinedExpected, js.isUndefined(g.TopLevelExportedClass))
    assertEquals(undefinedExpected, js.isUndefined(g.SJSDefinedTopLevelExportedClass))
    assertEquals(undefinedExpected, js.isUndefined(g.TopLevelExport_basic))
    assertEquals(undefinedExpected, js.isUndefined(g.TopLevelExport_basicVal))
    assertEquals(undefinedExpected, js.isUndefined(g.TopLevelExport_basicVar))
  }
}

object ExportNameHolder {
  final val className = "ConstantFoldedClassExport"
  final val objectName = "ConstantFoldedObjectExport"
  final val methodName = "myMethod"
}

@JSExportTopLevel("TopLevelExportedObject")
@JSExportTopLevel(ExportNameHolder.objectName)
object TopLevelExportedObject {
  @JSExport
  val witness: String = "witness"
}

@JSExportTopLevel("SJSDefinedTopLevelExportedObject")
object SJSDefinedExportedObject extends js.Object {
  val witness: String = "witness"
}

@JSExportTopLevel("ProtectedExportedObject")
protected object ProtectedExportedObject {
  @JSExport
  def witness: String = "witness"
}

@JSExportTopLevel("TopLevelExportedClass")
@JSExportTopLevel(ExportNameHolder.className)
class TopLevelExportedClass(_x: Int) {
  @JSExport
  val x = _x
}

@JSExportTopLevel("SJSDefinedTopLevelExportedClass")
class SJSDefinedTopLevelExportedClass(val x: Int) extends js.Object

@JSExportTopLevel("TopLevelExportedAbstractJSClass")
abstract class TopLevelExportedAbstractJSClass(val x: Int) extends js.Object {
  def foo(y: Int): Int

  def bar(y: Int): Int = 3 * foo(y)
}

@JSExportTopLevel("ProtectedExportedClass")
protected class ProtectedExportedClass(_x: Int) {
  @JSExport
  val x = _x
}

@JSExportTopLevel("ExportedVarArgClass")
class ExportedVarArgClass(x: String*) {

  @JSExportTopLevel("ExportedVarArgClass")
  def this(x: Int, y: String) = this(s"Number: <$x>", y)

  @JSExport
  def result: String = x.mkString("|")
}

@JSExportTopLevel("ExportedDefaultArgClass")
class ExportedDefaultArgClass(x: Int, y: Int, z: Int) {

  @JSExportTopLevel("ExportedDefaultArgClass")
  def this(x: Int, y: Int = 5) = this(x, y, 100)

  @JSExport
  def result: Int = x + y + z
}

class SomeValueClass(val i: Int) extends AnyVal

object ExportHolder {
  @JSExportTopLevel("NestedExportedClass")
  class ExportedClass

  @JSExportTopLevel("NestedExportedObject")
  object ExportedObject

  @JSExportTopLevel("NestedSJSDefinedExportedClass")
  class SJSDefinedExportedClass extends js.Object
}

object TopLevelExports {
  @JSExportTopLevel("TopLevelExport_basic")
  def basic(): Int = 1

  @JSExportTopLevel("TopLevelExport_overload")
  def overload(x: String): String = "Hello " + x

  @JSExportTopLevel("TopLevelExport_overload")
  def overload(x: Int, y: Int*): Int = x + y.sum

  @JSExportTopLevel("TopLevelExport_defaultParams")
  def defaultParams(x: Int, y: Int = 1): Int = x + y

  var myVar: Int = _

  @JSExportTopLevel("TopLevelExport_set")
  def setMyVar(x: Int): Unit = myVar = x

  object Nested {
    var myVar: Int = _

    @JSExportTopLevel("TopLevelExport_setNested")
    def setMyVar(x: Int): Unit = myVar = x
  }

  @JSExportTopLevel("__topLevelExportWithDoubleUnderscore")
  val topLevelExportWithDoubleUnderscore: Boolean = true
}

/* This object is only reachable via the top level export to make sure the
 * analyzer behaves correctly.
 */
object TopLevelExportsReachability {
  private val name = "World"

  @JSExportTopLevel("TopLevelExport_reachability")
  def basic(): String = "Hello " + name
}

object TopLevelFieldExports {
  @JSExportTopLevel("TopLevelExport_basicVal")
  val basicVal: Int = 5

  @JSExportTopLevel("TopLevelExport_basicVar")
  var basicVar: String = "hello"

  @JSExportTopLevel("TopLevelExport_valExportedTwice1")
  @JSExportTopLevel("TopLevelExport_valExportedTwice2")
  val valExportedTwice: Int = 5

  @JSExportTopLevel("TopLevelExport_varExportedTwice1")
  @JSExportTopLevel("TopLevelExport_varExportedTwice2")
  var varExportedTwice: String = "hello"

  @JSExportTopLevel("TopLevelExport_uninitializedVarInt")
  var uninitializedVarInt: Int = _

  @JSExportTopLevel("TopLevelExport_uninitializedVarLong")
  var uninitializedVarLong: Long = _

  @JSExportTopLevel("TopLevelExport_uninitializedVarString")
  var uninitializedVarString: String = _

  @JSExportTopLevel("TopLevelExport_uninitializedVarChar")
  var uninitializedVarChar: Char = _

  // the export is only to make the field IR-static
  @JSExportTopLevel("TopLevelExport_irrelevant")
  @(inline @meta.getter @meta.setter)
  var inlineVar: String = "hello"
}

/* This object and its static initializer are only reachable via the top-level
 * export of its field, to make sure the analyzer and the static initiliazer
 * behave correctly.
 */
object TopLevelFieldExportsReachability {
  private val name = "World"

  @JSExportTopLevel("TopLevelExport_fieldreachability")
  val greeting = "Hello " + name
}
