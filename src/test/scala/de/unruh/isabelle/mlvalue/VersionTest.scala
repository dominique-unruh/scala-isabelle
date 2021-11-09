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
    println(triple)
    string match {
      case "Isabelle2020: April 2020" =>
        assert(triple == (2020,0,NOT_RC))
      case "Isabelle2019: June 2019" =>
        assert(triple == (2019,0,NOT_RC))
      case "Isabelle2021-RC0: November 2020" =>
        assert(triple == (2021,0,0))
      case "Isabelle2021-RC2: January 2021" =>
        assert(triple == (2021,0,2))
      case "Isabelle2021-RC3: January 2021" =>
        assert(triple == (2021,0,3))
      case "Isabelle2021-RC4: February 2021" =>
        assert(triple == (2021,0,4))
      case "Isabelle2021-RC5: February 2021" =>
        assert(triple == (2021,0,5))
      case "Isabelle2021-RC6: February 2021" =>
        assert(triple == (2021,0,6))
      case "Isabelle2021: February 2021" =>
        assert(triple == (2021,0,NOT_RC))
      case "Isabelle2021-1-RC1" =>
        assert(triple == (2021,1,1))
      case "Isabelle2021-1-RC2" =>
        assert(triple == (2021,1,2))
      case "Isabelle2021-1" =>
        assert(triple == (2021,1,NOT_RC))
      case _ =>
        fail(s"Unknown version string $string. Please extend test case")
    }
  }
}
