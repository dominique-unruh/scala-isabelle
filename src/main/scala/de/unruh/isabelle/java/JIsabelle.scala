package de.unruh.isabelle.java

import java.nio.file.Path

import de.unruh.isabelle.control
import de.unruh.isabelle.control.Isabelle

/**
 * This object contains utility methods for invoking scala-isabelle methods from Java
 * in cases where the original method is difficult to invoke from Java.
 */
object JIsabelle {
  /** Invokes [[Isabelle.Setup]]`(isabelleHome=isabelleHome)`. All other arguments to
   * [[Isabelle.Setup]] take default values. */
  def setup(isabelleHome: Path): Isabelle.Setup = Isabelle.Setup(isabelleHome = isabelleHome)
}
