package de.unruh.isabelle.pure

import de.unruh.isabelle.pure.Context.ContextConverter
import de.unruh.isabelle.pure.Cterm.CtermConverter
import de.unruh.isabelle.pure.Ctyp.CtypConverter
import de.unruh.isabelle.pure.Term.TermConverter
import de.unruh.isabelle.pure.Theory.TheoryConverter
import de.unruh.isabelle.pure.Thm.ThmConverter
import de.unruh.isabelle.pure.Typ.TypConverter

/** Contains all the implicit [[mlvalue.MLValue.Converter MLValue.Converter]] instances provided by the package [[pure]].
 * Use
 * {{{
 *   import de.unruh.isabelle.pure.Implicits._
 * }}}
 * if you use [[mlvalue.MLValue MLValue]]`[A]` instances where `A` is any of [[Context]], [[Theory]], [[Term]], [[Typ]],
 * [[Cterm]], [[Ctyp]].
 */
object Implicits {
  implicit val contextConverter: ContextConverter.type = ContextConverter
  implicit val termConverter: TermConverter.type = TermConverter
  implicit val ctermConverter: CtermConverter.type = CtermConverter
  implicit val theoryConverter: TheoryConverter.type = TheoryConverter
  implicit val thmConverter: ThmConverter.type = ThmConverter
  implicit val typConverter: TypConverter.type = TypConverter
  implicit val ctypConverter: CtypConverter.type = CtypConverter
  implicit val theoryHeaderConverter : TheoryHeader.converter.type = TheoryHeader.converter
  implicit val positionConverter : Position.converter.type = Position.converter
  implicit val keywordsConverter : Keywords.converter.type = Keywords.converter
}
