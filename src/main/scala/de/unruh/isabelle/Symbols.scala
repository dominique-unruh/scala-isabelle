package de.unruh.isabelle

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.net.URL

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.matching.Regex

// TODO: Document API
class Symbols(symbolsFile: URL = classOf[Symbols].getResource("symbols"),
              extraSymbols: Traversable[(String,Int)] = Nil) {
  import Symbols._

  assert(symbolsFile != null)

  private val (symbols, symbolsInv) = {
    import scala.collection.JavaConverters._

    val lineRegex = """^\\<([a-zA-Z0-9^_]+)>\s+code:\s+0x([0-9a-fA-F]+)\b.*""".r
    val reader = new BufferedReader(new InputStreamReader(symbolsFile.openStream()))
    val results = new ListBuffer[(String, Int)]
    for (line <- reader.lines().iterator.asScala) {
      //      println(line)
      line match {
        case lineRegex(name, codepoint) => results.append((name, Integer.parseInt(codepoint, 16)))
        case _ => // Ignoring lines that do not introduce a symbol or do not contain a code point
      }
    }
    reader.close()

    results.appendAll(extraSymbols)

    //    println(results map { case (n,i) => new String(Character.toChars(i))+" " } mkString)
    val symbols = Map(results.toSeq: _*)
    val symbolsInv = Map(results.toSeq.map { case (n, i) => (i, n) }: _*)
    (symbols, symbolsInv)
  }

  def symbolsToUnicode(str: String): String = symbolRegex.replaceAllIn(str,
    { m: Regex.Match =>
      symbols.get(m.group(1)) match {
        case Some(i) => new String(Character.toChars(i))
        case None => m.matched
      }
    })

  def unicodeToSymbols(str: String): String = {
    val sb = new StringBuffer(str.length() * 11 / 10)
    for (cp <- codepoints(str)) {
      if (cp <= 128) sb.append(cp.toChar)
      else symbolsInv.get(cp) match {
        case Some(sym) => sb.append("\\<"); sb.append(sym); sb.append('>')
        case None =>
          if (cp > 255) throw new IOException(
            f"""Character "${new String(Character.toChars(cp))}%s" (Ux$cp%04X) not supported by Isabelle""")
          sb.appendCodePoint(cp)
      }
    }
    sb.toString
  }
}

object Symbols {
  lazy val globalInstance = new Symbols()
  def symbolsToUnicode(str: String): String = globalInstance.symbolsToUnicode(str)
  def unicodeToSymbols(str: String): String = globalInstance.unicodeToSymbols(str)

  private val symbolRegex = """\\<([a-zA-Z0-9^_]+)>""".r

  // following https://stackoverflow.com/a/1527891/2646248
  private def codepoints(str: String): Seq[Int] = {
    val len = str.length
    val result = new ArrayBuffer[Int](len)
    var offset = 0
    while (offset < len) {
      val cp = str.codePointAt(offset)
      result.append(cp)
      offset += Character.charCount(cp)
    }
    result.toSeq
  }

}