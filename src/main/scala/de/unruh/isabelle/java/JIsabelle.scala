package de.unruh.isabelle.java

import java.nio.file.Path

import de.unruh.isabelle.control
import de.unruh.isabelle.control.Isabelle

/**
 * This object contains utility methods for invoking scala-isabelle methods from Java
 * in cases where the original method is difficult to invoke from Java.
 *
 * For Scala methods that need but lack a wrapper, please
 * [[https://github.com/dominique-unruh/scala-isabelle/issues/new?labels=java file an issue]].
 */
object JIsabelle {
  /** Invokes [[control.Isabelle.SetupGeneral Isabelle.Setup]]`(isabelleHome=isabelleHome)`. All other arguments to
   * [[control.Isabelle.SetupGeneral Isabelle.Setup]] take default values.
   **/
  def setup(isabelleHome: Path): Isabelle.Setup = Isabelle.Setup(isabelleHome = isabelleHome)

  /** Sets the [[de.unruh.isabelle.control.Isabelle.Setup.build build]] flag in the setup `setup`.
   *
   * @return `setup` with [[de.unruh.isabelle.control.Isabelle.Setup.build build]] set to `build`
   * @param build the new value for `setup.`[[de.unruh.isabelle.control.Isabelle.Setup.build build]]
   **/
  def setupSetBuild(build : Boolean, setup : Isabelle.Setup): Isabelle.Setup = setup.copy(build = build)
}
