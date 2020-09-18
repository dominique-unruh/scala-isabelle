package de.unruh.isabelle.pure

import de.unruh.isabelle.pure.Context.ContextConverter
import de.unruh.isabelle.pure.Cterm.CtermConverter
import de.unruh.isabelle.pure.Term.TermConverter
import de.unruh.isabelle.pure.Theory.TheoryConverter
import de.unruh.isabelle.pure.Thm.ThmConverter
import de.unruh.isabelle.pure.Typ.TypConverter

// TODO document
object Implicits {
  implicit val contextConverter: ContextConverter.type = ContextConverter
  implicit val termConverter: TermConverter.type = TermConverter
  implicit val ctermConverter: CtermConverter.type = CtermConverter
  implicit val theoryConverter: TheoryConverter.type = TheoryConverter
  implicit val thmConverter: ThmConverter.type = ThmConverter
  implicit val typConverter: TypConverter.type = TypConverter
}
