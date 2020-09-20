package de.unruh.isabelle.misc

import org.scalatest.funsuite.AnyFunSuite

class EscapeSMLTest extends AnyFunSuite {
  test("mixed string") {
    assert(
      EscapeSML.escapeSml("chars\nmore\b\\\u0010")
      ==
      """chars\nmore\b\\\^P""")
  }

  test("unicode") {
    assertThrows[RuntimeException] {
      EscapeSML.escapeSml("γ")
    }
  }
}
