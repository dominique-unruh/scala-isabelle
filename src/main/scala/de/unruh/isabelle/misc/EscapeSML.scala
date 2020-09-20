package de.unruh.isabelle.misc

import java.io.{CharConversionException, Writer}
import java.nio.charset.CharacterCodingException

import org.apache.commons.text.StringEscapeUtils
import org.apache.commons.text.translate.{CharSequenceTranslator, CodePointTranslator}

// Escape rules are in Section 2.2, https://smlfamily.github.io/sml97-defn.pdf
object EscapeSML {
  /** A [[org.apache.commons.text.translate.CharSequenceTranslator CharSequenceTranslator]] for escaping ML strings (for use as ML
   * string literals).
   * @see escapeSml
   * */
  object ESCAPE_SML extends CodePointTranslator {
    override def translate(codepoint: Int, out: Writer): Boolean = codepoint match {
      case '\\' => out.write("""\\"""); true
      case '"' => out.write("""\""""); true
      case 7 => out.write("""\a"""); true
      case '\b' => out.write("""\b"""); true
      case '\t' => out.write("""\t"""); true
      case '\n' => out.write("""\n"""); true
      case '\f' => out.write("""\f"""); true
      case '\r' => out.write("""\r"""); true
      case _ if codepoint < 32 =>
        out.write("""\^""")
        out.write(codepoint + 64)
        true
      case _ if codepoint > 126 && codepoint <= 255 =>
        out.write(f"\\$codepoint%03d")
        true
//      case _ if codepoint > 255 =>
//        out.write(f"\\u$codepoint%04x")
//        true
      case _ if codepoint > 255 =>
        // https://smlfamily.github.io/sml97-defn.pdf defines \u, but experiments
        // in Isabelle show this to be unsupported
        throw new RuntimeException("Codepoints > 255 not supported in Isabelle's ML dialect")
      case _ =>
        false
    }
  }

  /**
   * Escapes a string for use in an ML string literal.
   *
   * Follows Section 2.2, [[https://smlfamily.github.io/sml97-defn.pdf]], but without the \u escape sequence
   * (which is not supported in Isabelle).
   *
   * Example:
   * {{{
   *   val rawString = "... something potentially containing unsafe characters ..."
   *   val mlCode = s"val str = \"\${EscapeSML.escapeSML(rawString)}\""
   * }}}
   */
  def escapeSml(string: String): String = ESCAPE_SML.translate(string)
}
