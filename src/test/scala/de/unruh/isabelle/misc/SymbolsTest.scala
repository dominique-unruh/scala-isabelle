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

  test("superscript") {
    val string = "x²"
    val symbolString = Symbols.unicodeToSymbols(string)
    assert(symbolString == raw"x\<^sup>2")
    val unicodeString = Symbols.symbolsToUnicode(symbolString)
    assert(unicodeString == string)
  }

  test("superscript a") {
    // different Unicode superscript a's
    val string1 = "xª" // FEMININE ORDINAL INDICATOR
    val string2 = "xᵃ" // MODIFIER LETTER SMALL A
    assert(string1 != string2)
    val symbolString1 = Symbols.unicodeToSymbols(string1)
    val symbolString2 = Symbols.unicodeToSymbols(string2)
    assert(symbolString1 == raw"x\<^sup>a")
    assert(symbolString2 == raw"x\<^sup>a")
    assert(symbolString1 == symbolString2)
    val unicodeString = Symbols.symbolsToUnicode(symbolString1)
    assert(unicodeString == string2)
  }

  test("subscript") {
    val string = "xᵣ"
    val symbolString = Symbols.unicodeToSymbols(string)
    assert(symbolString == raw"x\<^sub>r")
    val unicodeString = Symbols.symbolsToUnicode(symbolString)
    assert(unicodeString == string)
  }
}
