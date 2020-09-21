package de.unruh.isabelle

import java.nio.file.Path

import de.unruh.isabelle.control.IsabelleTest
import de.unruh.isabelle.mlvalue.{MLFunction2, MLValue}
import de.unruh.isabelle.pure.{Abs, App, Const, Term}
import org.scalatest.funsuite.AnyFunSuite


class Test extends AnyFunSuite {
  test("README example") {
    Example.main(Array(IsabelleTest.isabelleHome.toString))
  }
}
