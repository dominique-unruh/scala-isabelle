package de.unruh.isabelle.misc

import org.scalatest.funsuite.AnyFunSuite

class SMLCodeUtilsTest extends AnyFunSuite {
  test("escapeSml: mixed string") {
    assert(
      SMLCodeUtils.escapeSml("chars\nmore\b\\\u0010")
      ==
      """chars\nmore\b\\\^P""")
  }

  test("escapeSml: unicode") {
    assertThrows[RuntimeException] {
      SMLCodeUtils.escapeSml("γ")
    }
  }
}
