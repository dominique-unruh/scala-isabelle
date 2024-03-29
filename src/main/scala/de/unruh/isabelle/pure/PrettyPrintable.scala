package de.unruh.isabelle.pure

import de.unruh.isabelle.misc.Symbols
import org.jetbrains.annotations.NotNull

/** Base trait for object that can be pretty printed using an Isabelle proof context ([[Context]]). */
trait PrettyPrintable {
  /** Produces a string representation of this object.
   * Uses the Isabelle pretty printer.
   * @param ctxt The Isabelle proof context to use (this contains syntax declarations etc.)
   * @param symbols Instance of [[misc.Symbols Symbols]] for converting to Unicode. Default: global default instance
   *                [[misc.Symbols.globalInstance Symbols.globalInstance]]. Use [[prettyRaw]] to avoid conversion to Unicode.
   * */
  @NotNull def pretty(@NotNull ctxt: Context, @NotNull symbols: Symbols = Symbols.globalInstance): String =
    symbols.symbolsToUnicode(prettyRaw(ctxt))

  /** Produces a string representation of this object.
   * Uses the Isabelle pretty printer.
   * Does not convert to Unicode, i.e.,
   * the return value will contain substrings such as `\<forall>`)
   *
   * @param ctxt The Isabelle proof context to use (this contains syntax declarations etc.)
   */
  def prettyRaw(ctxt: Context): String
}
