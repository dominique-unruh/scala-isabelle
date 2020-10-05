package de.unruh.isabelle.misc

import scala.util.Random

import scalaz.syntax.id._

/** Contains miscellaneous utility functions */
object Utils {
  // DOCUMENT
  def freshName(name: String): String = {
    name
      .map { c => if (c<128 && c.isLetterOrDigit) c else '_' }
      .into { n => if (n.head.isLetter) n else "X"+n }
      .into { _ + '_' + Random.alphanumeric.take(12).mkString }
  }
}
