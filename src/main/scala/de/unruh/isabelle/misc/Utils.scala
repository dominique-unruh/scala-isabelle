package de.unruh.isabelle.misc

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
      .into { _ + '_' + Random.alphanumeric.take(12).mkString }
  }
}
