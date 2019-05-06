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

package org.scalajs.sbtplugin

import sbt._

import org.scalajs.core.tools.io.FileVirtualFile

private[sbtplugin] object SBTCompat {
  type IncOptions = xsbti.compile.IncOptions

  type CompileAnalysis = xsbti.compile.CompileAnalysis

  val formatImplicits: sjsonnew.BasicJsonProtocol.type =
    sjsonnew.BasicJsonProtocol

  def moduleIDWithConfigurations(moduleID: ModuleID,
      configurations: Option[String]): ModuleID = {
    moduleID.withConfigurations(configurations)
  }

  def crossVersionAddScalaJSPart(cross: CrossVersion,
      part: String): CrossVersion = {
    cross match {
      case CrossVersion.Disabled =>
        CrossVersion.constant(part)
      case cross: sbt.librarymanagement.Constant =>
        cross.withValue(part + "_" + cross.value)
      case cross: CrossVersion.Binary =>
        cross.withPrefix(part + "_" + cross.prefix)
      case cross: CrossVersion.Full =>
        cross.withPrefix(part + "_" + cross.prefix)
    }
  }

  /** Patches the IncOptions so that .sjsir files are pruned as needed.
   *
   *  This complicated logic patches the ClassfileManager factory of the given
   *  IncOptions with one that is aware of .sjsir files emitted by the Scala.js
   *  compiler. This makes sure that, when a .class file must be deleted, the
   *  corresponding .sjsir file are also deleted.
   */
  def scalaJSPatchIncOptions(incOptions: IncOptions): IncOptions = {
    import xsbti.compile.{ClassFileManager, ClassFileManagerUtil}

    val sjsirFileManager = new ClassFileManager {
      private[this] val inherited =
        ClassFileManagerUtil.getDefaultClassFileManager(incOptions)

      def delete(classes: Array[File]): Unit = {
        inherited.delete(classes.flatMap { classFile =>
          if (classFile.getPath.endsWith(".class")) {
            val f = FileVirtualFile.withExtension(classFile, ".class", ".sjsir")
            if (f.exists) List(f)
            else Nil
          } else {
            Nil
          }
        })
      }

      def generated(classes: Array[File]): Unit = {}
      def complete(success: Boolean): Unit = {}
    }

    val newExternalHooks =
      incOptions.externalHooks.withExternalClassFileManager(sjsirFileManager)
    incOptions.withExternalHooks(newExternalHooks)
  }
}
