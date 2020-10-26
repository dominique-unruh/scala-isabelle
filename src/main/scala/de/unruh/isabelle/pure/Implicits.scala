package de.unruh.isabelle.pure

import de.unruh.isabelle.pure._

/** Contains all the implicit [[mlvalue.MLValue.Converter MLValue.Converter]] instances provided by the package [[pure]].
 * Use
 * {{{
 *   import de.unruh.isabelle.pure.Implicits._
 * }}}
 * if you use [[mlvalue.MLValue MLValue]]`[A]` instances where `A` is any of [[Context]], [[Theory]], [[Term]], [[Typ]],
 * [[Cterm]], [[Ctyp]].
 */
//noinspection TypeAnnotation
object Implicits {
  implicit val contextConverter = Context.converter
  implicit val termConverter = Term.TermConverter
  implicit val ctermConverter = Cterm.CtermConverter
  implicit val theoryConverter = Theory.TheoryConverter
  implicit val thmConverter = Thm.ThmConverter
  implicit val typConverter = Typ.TypConverter
  implicit val ctypConverter = Ctyp.CtypConverter
  implicit val theoryHeaderConverter = TheoryHeader.converter
  implicit val positionConverter = Position.converter
  implicit val mutexConverter = Mutex.converter
  implicit val keywordsConverter = Keywords.converter
  implicit val toplevelStateConverter = ToplevelState.converter
  implicit val pathConverter = PathConverter
  implicit val prooftermConverter = Proofterm.converter
  implicit val prooftermBodyConverter = Proofterm.ThmBody.converter
}
