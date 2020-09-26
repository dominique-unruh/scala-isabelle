package de.unruh.isabelle.java

import java.nio.file.Path

import de.unruh.isabelle.control
import de.unruh.isabelle.control.Isabelle

object JIsabelle {
  def setup(isabelleHome: Path): Isabelle.Setup = Isabelle.Setup(isabelleHome = isabelleHome)
}
