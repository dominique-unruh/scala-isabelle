package de.unruh.isabelle.mlvalue

// Implicits
import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.mlvalue.Version.NOT_RC

import scala.concurrent.ExecutionContext.Implicits.global

class VersionTest extends org.scalatest.funsuite.AnyFunSuite {
  test("parse version") {
    val string = Version.versionString
    println(string)
    val triple = (Version.year, Version.step, Version.rc)
    string match {
      case "Isabelle2020: April 2020" =>
        assert(triple == (2020,0,NOT_RC))
      case "Isabelle2019: June 2019" =>
        assert(triple == (2019,0,NOT_RC))
      case "Isabelle2021-RC0: November 2020" =>
        assert(triple == (2021,0,0))
      case _ => fail(s"Unknown version string $string")
    }
  }
}
