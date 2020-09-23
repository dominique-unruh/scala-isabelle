package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.MLValue.Converter
import scalaz.Id.Id

import scala.concurrent.{ExecutionContext, Future}

/** A special [[MLValue.Converter]] that allows us to create [[MLValue]]s that contain [[MLValue]]s.
 *
 * MLType: `a` (if `a` is the type corresponding to `A`). (However, [[mlType]] returns "_" for technical reasons,
 * see below.)
 * Encoding of `a` as an exception: Same as the encoding specified by the converter for `A`.
 *
 * This means that both [[MLValue]]`[A]` and `A` correspond to the same ML type and are encoded in the same way.
 * Consequently, an instance of `MLValue[MLValue[A]]` can be safely typecast to an instance of `MLValue[A]` and vice versa.
 * (Or more generally, `MLValue[C[MLValue[A]]]` into `MLValue[C[A]]` and vice versa.)
 *
 * The benefit of this special converter is probably best illustrated by an example. Say `Huge` is an Scala type
 * corresponding to an ML type `huge`. And assume transferring `huge` between the Isabelle process and Scala is
 * expensive (or maybe even impossible). And say we have an `myValue : MLValue[List[Huge]]` which contains a long list of `huge` in the Isabelle
 * process. We can get a `List[Huge]` by calling `myValue.retrieveNow`, but this might be too expensive, especially if
 * we do not need the whole list. Instead, we can do:
 * {{{
 *   val myValue2 : MLValue[List[MLValue[Huge]]] = myValue.asInstanceOf[MLValue[List[MLValue[Huge]]]]
 *   val myList : List[MLValue[Huge]] = myValue2.retrieveNow
 * }}}
 * `myList` now is a list of `MLValue[Huge]` and is constructed efficiently (because an `MLValue[Huge]` just references
 * an object in the Isabelle process's object store). And then one can retrieve individual `Huge` objects in that list
 * with additional calls [[MLValue.retrieveNow .retrieveNow]] (or just use the values without ever retrieving them
 * as arguments to other functions that reside in the ML process).
 *
 * An instance of [[MLValueConverter]] can be constructed without using a [[MLValue.Converter Converter]] for `A`. This
 * means the above approach (except for retrieving values of type `huge`) can even be used if no converter for `A`
 * has not actually been implemented. (As long as we consistently imagine a specific ML type and encoding for `A`.)
 * As a consequence, [[mlType]] returns "_" instead of the ML type `a` corresponding to `A`.
 */
@inline final class MLValueConverter[A] extends Converter[MLValue[A]] {
  override def retrieve(value: MLValue[MLValue[A]])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[MLValue[A]] =
    Future.successful(value.removeMLValue[Id, A])

  override def store(value: MLValue[A])(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[MLValue[A]] =
    value.insertMLValue[Id, A]

  @inline override def exnToValue: String = "fn x => x"
  @inline override def valueToExn: String = "fn x => x"

  override def mlType: String = "_"
}
