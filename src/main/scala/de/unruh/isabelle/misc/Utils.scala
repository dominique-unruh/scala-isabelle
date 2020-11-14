package de.unruh.isabelle.misc

import java.nio.file.Path

import org.apache.commons.lang3.SystemUtils

import scala.util.Random
import scalaz.syntax.id._

/** Contains miscellaneous utility functions */
object Utils {
  /** Generates a fresh name based on `name`.
   *
   * The name is guaranteed to:
   *  - Be unique (by containing a random substring)
   *  - Contain only ASCII letters, digits, and underscores
   *  - Start with a letter
   *
   * `name` will be a prefix of the fresh name as far as possible.
   *
   * @return the fresh name
   **/
  def freshName(name: String): String = {
    name
      .map { c => if (c<128 && c.isLetterOrDigit) c else '_' }
      .into { n => if (n.head.isLetter) n else "x"+n }
      .into { _ + '_' + randomString() }
  }

  // DOCUMENT
  def randomString(): String = Random.alphanumeric.take(12).mkString

  /** Converts `path` to a path understood by Cygwin.
   * (Only available when running under Windows.) */
  def cygwinPath(path: Path): String =
    if (path.isAbsolute) {
      assert(SystemUtils.IS_OS_WINDOWS)
      val root = path.getRoot.toString.stripSuffix(":\\")
      val parts = for (i <- 0 until path.getNameCount) yield path.getName(i)
      s"/cygdrive/$root/${parts.mkString("/")}"
    } else {
      assert(SystemUtils.IS_OS_WINDOWS)
      val parts = for (i <- 0 until path.getNameCount) yield path.getName(i)
      parts.mkString("/")
    }
}
