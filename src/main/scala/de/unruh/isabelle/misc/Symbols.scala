package de.unruh.isabelle.misc

import java.io.{BufferedReader, CharConversionException, IOException, InputStreamReader}
import java.net.URL

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.matching.Regex

/**
 * To encode non-ASCII characters ("symbols") in strings, Isabelle uses a proprietary encoding which
 * encodes a symbol as a substring `\<name>` where `name` is the name of the substring. (Thus, strings
 * containing symbols are ASCII strings.)
 * There is a loose correspondence between symbols and Unicode codepositions that
 * is documented in a machine readable format in `etc/symbols` in the Isabelle distribution.
 * This class translates between Isabelle's encoding and Unicode strings.
 *
 * If the default values for this class's constructor are appropriate, consider using the static
 * functions [[Symbols.symbolsToUnicode]] and [[Symbols.unicodeToSymbols]] instead of instantiating this class.
 *
 * @param symbolsFile Location of the `symbols` that specifies the correspondence.
 *                    Default: `symbols` file from Isabelle2020 (bundled with this library).
 * @param extraSymbols Additional symbol name / codepoint pairs to use in addition to those in the `symbols` file.
 */
class Symbols(symbolsFile: URL = classOf[Symbols].getResource("symbols"),
              extraSymbols: Iterable[(String,Int)] = Nil) {
  import Symbols._

  assert(symbolsFile != null)

  private val (symbols, symbolsInv) = {
    // Can't use the replacement scala.jdk.CollectionConverters because we support Scala 2.12
    //noinspection ScalaDeprecation
    import scala.collection.JavaConverters._

    val lineRegex = """^\\<([a-zA-Z0-9^_]+)>\s+code:\s+0x([0-9a-fA-F]+)\b.*""".r
    val reader = new BufferedReader(new InputStreamReader(symbolsFile.openStream()))
    val results = new ListBuffer[(String, Int)]
    for (line <- reader.lines().iterator.asScala) {
      line match {
        case lineRegex(name, codepoint) => results.append((name, Integer.parseInt(codepoint, 16)))
        case _ => // Ignoring lines that do not introduce a symbol or do not contain a code point
      }
    }
    reader.close()

    results.appendAll(extraSymbols)

    val symbols = Map(results.toSeq: _*)
    val symbolsInv = Map(results.toSeq.map { case (n, i) => (i, n) }: _*)
    (symbols, symbolsInv)
  }

  /** Converts a string in Isabelle's encoding to Unicode.
   * @param failUnknown If true, unknown symbols cause a [[java.io.CharConversionException CharConversionException]].
   *                    If false, unknown symbols are left unchanged in the string. */
  def symbolsToUnicode(str: String, failUnknown: Boolean = false): String = symbolRegex.replaceAllIn(str,
    { m: Regex.Match =>
      symbols.get(m.group(1)) match {
        case Some(i) => new String(Character.toChars(i))
        case None =>
          if (failUnknown)
            throw new CharConversionException(s"Unknown symbol ${m.matched}")
          else
            m.matched
      }
    })

  /** Converts a Unicode string to a string using Isabelle's symbol encoding.
   * @param failUnknown If true, unknown Unicode characters cause a [[java.io.CharConversionException CharConversionException]].
   *                    If false, unknown Unicode characters are encoded as `\<unicodeX>` where `X` is the code position
   *                    in uppercase hex. (Without leading zeros.)
   **/
  def unicodeToSymbols(str: String, failUnknown: Boolean = false): String = {
    val sb = new StringBuffer(str.length() * 11 / 10)
    for (cp <- codepoints(str)) {
      if (cp <= 128) sb.append(cp.toChar)
      else symbolsInv.get(cp) match {
        case Some(sym) => sb.append("\\<"); sb.append(sym); sb.append('>')
        case None =>
          if (cp > 255) {
            if (failUnknown)
              throw new CharConversionException(
                f"""Character "${new String(Character.toChars(cp))}%s" (Ux$cp%04X) not supported by Isabelle""")
            else
              sb.append(f"\\<unicode$cp%X>")
          } else
            sb.appendCodePoint(cp)
      }
    }
    sb.toString
  }
}

object Symbols {
  /** Global instance of [[Symbols]]. Can be used if the default settings of [[Symbols]] are needed. */
  lazy val globalInstance = new Symbols()
  /** Translates an Isabelle string to unicode using the global instance [[globalInstance]].
   * @see [[Symbols.symbolsToUnicode]] for details. */
  def symbolsToUnicode(str: String, failUnknown: Boolean = false): String = globalInstance.symbolsToUnicode(str, failUnknown)
  /** Translates a unicode string to an Isabelle string using the global instance [[globalInstance]].
   * @see [[Symbols.unicodeToSymbols]] for details. */
  def unicodeToSymbols(str: String, failUnknown: Boolean = false): String = globalInstance.unicodeToSymbols(str, failUnknown)

  private val symbolRegex = """\\<([a-zA-Z0-9^_]+)>""".r

  // following https://stackoverflow.com/a/1527891/2646248
  private def codepoints(str: String): Iterable[Int] = {
    val len = str.length
    val result = new ArrayBuffer[Int](len)
    var offset = 0
    while (offset < len) {
      val cp = str.codePointAt(offset)
      result.append(cp)
      offset += Character.charCount(cp)
    }
    result
  }
}