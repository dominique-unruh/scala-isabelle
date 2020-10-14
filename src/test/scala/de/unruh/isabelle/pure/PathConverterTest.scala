package de.unruh.isabelle.pure

import java.nio.file.{Files, Path, Paths}

import de.unruh.isabelle.control.IsabelleTest.{isabelle, setup}
import de.unruh.isabelle.mlvalue.{MLFunction, MLValue}
import de.unruh.isabelle.mlvalue.Implicits._
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import Implicits._

class PathConverterTest extends AnyFunSuite {
  lazy val fileExists: MLFunction[Path, Boolean] = MLValue.compileFunction[Path,Boolean]("File.exists")
  lazy val pathI: MLFunction[Path, Path] = MLValue.compileFunction("I")
  lazy val pathExplode: MLFunction[String, Path] = MLValue.compileFunction[String, Path]("Path.explode")

  test("Isabelle home") {
    val path = pathExplode("~~/ROOTS").retrieveNow
    println(path)
    assert(Files.exists(path))
  }

  test("access file, absolute path") {
    val path = setup.workingDirectory.resolve("Empty.thy").toAbsolutePath
    assert(Files.exists(path))
    assert(fileExists(path).retrieveNow)
  }

  test("access file, relative path") {
    val path = Paths.get("Empty.thy")
    assert(Files.exists(setup.workingDirectory.resolve(path)))
    assert(fileExists(path).retrieveNow)
  }

  test("roundtrip relative") {
    val path = Paths.get("test","subdir","file.ext")
    assert(pathI(path).retrieveNow == path)
  }

  test("roundtrip absolute") {
    val path = Paths.get("test","subdir","file.ext").toAbsolutePath
    assert(pathI(path).retrieveNow == path)
  }
}
