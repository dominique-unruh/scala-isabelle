package de.unruh.isabelle

import de.unruh.isabelle.control.{IsabelleTest, PIDEWrapper}
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.pure.{Context, Cterm, Term, Thm}
import org.scalatest.funsuite.AnyFunSuite

import _root_.java.net.{URL, URLClassLoader}
import _root_.java.nio.file.{FileVisitOption, Files, Path, Paths}
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.reflect.api.JavaUniverse
import scala.reflect.internal.{StdNames, SymbolTable}
import scala.util.Random

// Implicits
import de.unruh.isabelle.control.IsabelleTest.isabelle
import scala.concurrent.ExecutionContext.Implicits.global
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

class Test extends AnyFunSuite {
  test("README example") {
    Example.main(Array(IsabelleTest.isabelleHome.toString))
  }

  test("Java example") {
    JavaExample.main(Array(IsabelleTest.isabelleHome.toString))
  }

  test("temporary experiments") {
    val mlCode = """tracing "1""""

    val pideWrapper = PIDEWrapper.getDefaultPIDEWrapper(IsabelleTest.isabelleHome)

    val process = pideWrapper.startIsabelleProcess(mlCode=mlCode)

    println(1,process)

    pideWrapper.waitForProcess(process,
      progress_stdout = { line => print(s"  $line\n") },
      progress_stderr = { line => print(s"* $line\n") })

    println(2,process)
  }
}
