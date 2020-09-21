package de.unruh.isabelle.misc

import java.io.CharConversionException

import org.scalatest.funsuite.AnyFunSuite

class SymbolsTest extends AnyFunSuite {

  test("roundtrip") {
    val string = """Proj (Span {ket 0})»⟦C1⟧ · Proj (Span {ket 0})»⟦A1⟧ ⋅ """
    val symbolString = Symbols.unicodeToSymbols(string)
    println(symbolString)
    val unicodeString = Symbols.symbolsToUnicode(symbolString)
    println(unicodeString)
    assert(unicodeString == string)
  }

  test("unknown unicode (no fail)") {
    val string = "火"
    val symbolString = Symbols.unicodeToSymbols(string)
    assert(symbolString == "\\<unicode706B>")
  }

  test("unknown unicode (fail)") {
    val string = "火"
    assertThrows[CharConversionException] {
      Symbols.unicodeToSymbols(string, failUnknown = true)
    }
  }
}
