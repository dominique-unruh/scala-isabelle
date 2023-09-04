package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle
import org.scalatest.funsuite.AnyFunSuite


class TheoryHeaderTest extends AnyFunSuite {
  test ("thy_header simple") {
    val header = TheoryHeader.read("theory Foo imports Bar Baz begin")
    assert(header.name == "Foo")
    assert(header.imports == List("Bar", "Baz"))
  }

  test ("thy_header hard") {
    val t = "theory \"Foo-a_b\" imports \"../Bar-a_b\" Baz.Baz (* comment *) begin whatever"
    val header = TheoryHeader.read(t)
    assert(header.name == "Foo-a_b")
    assert(header.imports == List("../Bar-a_b", "Baz.Baz"))
  }
}
