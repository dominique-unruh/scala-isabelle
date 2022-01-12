package de.unruh.isabelle.misc

import com.ibm.icu.lang.{CharacterProperties, UCharacter, UProperty}
import com.ibm.icu.lang.UCharacter.DecompositionType.{SUB, SUPER}
import com.ibm.icu.text.Normalizer2

import java.io.{BufferedReader, CharConversionException, InputStreamReader}
import java.net.URL
import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.{immutable, mutable}
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
 * @param processSubSuper Whether to process ⇩ and ⇧ symbols (on the Isabelle side) into/from subscript/superscript symbols in Unicode.
 *                        (for those letters that have Unicode subscript/superscript symbols)
 */
class Symbols(symbolsFile: URL = classOf[Symbols].getResource("symbols"),
              extraSymbols: Iterable[(String,Int)] = Nil,
              processSubSuper: Boolean = true) {

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
   *
   * @param failUnknown If true, unknown symbols cause a [[java.io.CharConversionException CharConversionException]].
   *                    If false, unknown symbols are left unchanged in the string. */
  def symbolsToUnicode(str: String, failUnknown: Boolean = false): String = {
    val result = symbolRegex.replaceAllIn(str,
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
    if (processSubSuper) processSubSuperToUnicode(result) else result
  }

  /** Converts a Unicode string to a string using Isabelle's symbol encoding.
   * @param failUnknown If true, unknown Unicode characters cause a [[java.io.CharConversionException CharConversionException]].
   *                    If false, unknown Unicode characters are encoded as `\<unicodeX>` where `X` is the code position
   *                    in uppercase hex. (Without leading zeros.)
   **/
  def unicodeToSymbols(str: String, failUnknown: Boolean = false): String = {
    val sb = new StringBuffer(str.length() * 11 / 10)
    val str2 = if (processSubSuper) processSubSuperFromUnicode(str) else str
    for (cp <- codepoints(str2)) {
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

  private val subSymbol = '⇩'
  private val superSymbol = '⇧'

  private lazy val (fromSubSuper, toSub, toSuper) = {
    val fromSubSuper = new mutable.HashMap[Int, String]
    val toSub = new mutable.HashMap[Int, Int]
    val toSuper = new mutable.HashMap[Int, Int]
    val normalizer = Normalizer2.getNFKDInstance

    for (range <- CharacterProperties.getIntPropertyMap(UProperty.DECOMPOSITION_TYPE).asScala) {
      val decompositionType = range.getValue
      if (decompositionType == SUB || decompositionType == SUPER) {
        for (c <- range.getStart to range.getEnd) {
          val decompositionString = normalizer.getRawDecomposition(c)
          if (decompositionString.length == 1) {
            val decompositionChar = decompositionString.charAt(0)
            val name = UCharacter.getName(c)
            if (decompositionType == SUB) {
              //                println(s"${c.asInstanceOf[Char]} -> sub $decompositionString;   ${name}")
              fromSubSuper.put(c, new String(Array(subSymbol, decompositionChar)))
              /*if (!toSub.contains(decompositionChar))*/ toSub.put(decompositionChar, c)
            } else {
              //                println(s"${c.asInstanceOf[Char]} -> super $decompositionString;   ${name}")
              fromSubSuper.put(c, new String(Array(superSymbol, decompositionChar)))
              /*if (!toSuper.contains(decompositionChar))*/ toSuper.put(decompositionChar, c)
            }
          }
        }
      }
    }

    (fromSubSuper.toMap, toSub.toMap, toSuper.toMap)
  }

  private def processSubSuperFromUnicode(str: String): String = {
    val sb = new StringBuffer(str.length() * 11 / 10)
    for (cp <- codepoints(str)) {
      fromSubSuper.get(cp) match {
        case Some(subst) => sb.append(subst)
        case None => sb.append(Character.toChars(cp))
      }
    }
    sb.toString
  }

  private def processSubSuperToUnicode(str: String): String = {
    val sb = new StringBuffer(str.length())
    val it = str.iterator
    for (c <- it) {
      c match {
        case `subSymbol` | `superSymbol` =>
          val table = if (c == subSymbol) toSub else toSuper
          if (it.hasNext) {
            val c2 = it.next()
            table.get(c2) match {
              case Some(replacement) => sb.append(replacement.toChar)
              case None => sb.append(c).append(c2)
            }
          } else
            sb.append(c)
        case _ => sb.append(c)
      }
    }
    sb.toString
  }
}
