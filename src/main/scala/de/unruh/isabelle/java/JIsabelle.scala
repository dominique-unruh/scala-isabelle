package de.unruh.isabelle.java

import java.nio.file.Path

import de.unruh.isabelle.control
import de.unruh.isabelle.control.Isabelle
import scala.jdk.CollectionConverters._

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

  /** Sets the [[de.unruh.isabelle.control.Isabelle.Setup.verbose verbose]] flag in the setup `setup`.
   *
   * @return `setup` with [[de.unruh.isabelle.control.Isabelle.Setup.verbose verbose]] set to `verbose`
   * @param verbose the new value for `setup.`[[de.unruh.isabelle.control.Isabelle.Setup.verbose verbose]]
   * */
  def setupSetVerbose(verbose : Boolean, setup : Isabelle.Setup): Isabelle.Setup = setup.copy(verbose = verbose)

  /** Sets the [[de.unruh.isabelle.control.Isabelle.Setup.userDir userDir]] directory in the setup `setup`.
   * Note: There is no way to change the `userDir` to `None`. However, the default value for `userDir` is `None`.
   *
   * @return `setup` with [[de.unruh.isabelle.control.Isabelle.Setup.userDir userDir]] set to `Some(userDir)`
   * @param verbose the new value for `setup.`[[de.unruh.isabelle.control.Isabelle.Setup.verbose verbose]]
   * */
  def setupSetUserDir(userDir: Path, setup: Isabelle.Setup): Isabelle.Setup = setup.copy(userDir = Some(userDir))

  /** Sets the [[de.unruh.isabelle.control.Isabelle.Setup.workingDirectory workingDirectory]] directory in the setup `setup`.
   *
   * @return `setup` with [[de.unruh.isabelle.control.Isabelle.Setup.workingDirectory workingDirectory]] set to `workingDirectory`
   * @param workingDirectory the new value for `setup.`[[de.unruh.isabelle.control.Isabelle.Setup.workingDirectory workingDirectory]]
   * */
  def setupSetWorkingDirectory(workingDirectory: Path, setup: Isabelle.Setup): Isabelle.Setup =
    setup.copy(workingDirectory = workingDirectory)

  /** Sets the [[de.unruh.isabelle.control.Isabelle.Setup.sessionRoots sessionRoots]] directories in the setup `setup`.
   *
   * @return `setup` with [[de.unruh.isabelle.control.Isabelle.Setup.sessionRoots sessionRoots]] set to `sessionRoots`
   * @param sessionRoots the new value for `setup.`[[de.unruh.isabelle.control.Isabelle.Setup.sessionRoots sessionRoots]].
   *                     It will automatically be converted to a Scala [[Seq]].
   * */
  def setupSetSessionRoots(sessionRoots: java.lang.Iterable[Path], setup: Isabelle.Setup): Isabelle.Setup =
    setup.copy(sessionRoots = sessionRoots.asScala.toSeq)
}
