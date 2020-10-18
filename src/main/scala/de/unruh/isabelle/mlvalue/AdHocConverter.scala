package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.Utils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/** This class allows to add support for Isabelle ML types in Scala in a quick ad-hoc way.
 *
 * Let `something` be an ML type. To support values of ML type `type` in Scala, write
 * {{{
 * object Something extends AdHocConverter("something")
 * }}}
 * (Here `Something` is an arbitrary identifier that should preferably remind of the ML type `something`.)
 * Then ML values of type `something` stored in the Isabelle process can be used in Scala as objects of
 * type `Something.`[[AdHocConverter.T T]]. That is, an `Something.`[[AdHocConverter.T T]] is a thin wrapper
 * around an [[MLValue]] that references values of type `something`. The actual ML value is never transferred from Isabelle.
 *
 * An [[MLValueConverter]] is automatically available for type `Something.`[[AdHocConverter.T T]],
 * thus, e.g., [[MLValue.apply]] and [[MLValue.retrieveNow]] and related functions work properly to
 * translate between `Something.`[[AdHocConverter.T T]] and `MLValue[Something.T]`.
 *
 * '''Note:''' This is only intended for local use. That is, if some Scala code needs to handle ML values
 * for which there is no proper support, and that just need to be passed around between different ML functions,
 * then it is possible to use this class for supporting them with minimal boilerplate. However, if the Scala types
 * representing the ML values are supposed to be used on a larger scale or exported from a library,
 * or are supposed to be extended, then it is recommended to use a more flexible mechanism such as [[MLValueWrapper]]
 * for creating wrapper classes for ML values.
 *
 * '''Note:''' `object ... extends AdHocConverter(...)` creates a new exception type in ML when it is created.
 * Thus this declaration should only be used in
 * a static context (toplevel/inside an object) or in classes that are instantiated very rarely (or in
 * [[control.OperationCollection.Ops OperationCollection.Ops]] in a static object).
 *
 * '''Usage example:'''
 * Say we want to use the ML function `Simplifier.pretty_simpset: bool -> Proof.context -> Pretty.T`. This function returns a
 * `Pretty.T` which is not (currently) directly supported by this library. But we simply want to pass it on to
 * `Pretty.unformatted_string_of: Pretty.T -> string`. So we can declare
 * {{{
 * object Pretty extends AdHocConverter("Pretty.T")
 * val pretty_simpset = MLValue.compileFunction[Boolean, Context, Pretty.T]("Simplifier.pretty_simpset")
 * val unformatted_string_of = MLValue.compileFunction[Pretty.T, String]("Pretty.unformatted_string_of")
 * }}}
 * and then pretty print the simpset of a context `ctxt` by
 * {{{
 * val string : String = unformatted_string_of(pretty_simpset(false, ctxt).retrieveNow).retrieveNow
 * }}}
 * (Or without the first `.retrieveNow`.)
 *
 * @param mlType The ML type for which to create a Scala wrapper type
 **/
abstract class AdHocConverter protected(val mlType: String) extends OperationCollection {
  private val tString = s"‹$mlType›"

  /** Represents an ML Value of ML type [[mlType]]. The representation of an ML value `x` as an exception
   * (see explanations in [[MLValue]]) is `EXN x` where `EXN` is what [[exceptionName]] returns.
   *
   * An [[MLValue]] containing this can be obtained from [[mlValue]]. (Both `this` and [[mlValue]]
   * refer to essentially the same object, conversion is cheap. No data is transferred from/to the Isabelle context.)
   **/
  final class T private[AdHocConverter](val mlValue: MLValue[T]) extends FutureValue {
    override def await: Unit = mlValue.await
    override def someFuture: Future[Any] = mlValue.someFuture
    override def toString: String = tString
  }

  private val _exceptionName: String = Utils.freshName("E_" + mlType)

  /** The name of the exception that encodes ML values of type [[mlType]]. (See [[MLValue]] for explanations.)
   * The name is guaranteed not to conflict with existing names (randomized name).
   * And the exception is guaranteed to be declared in ML when this function returns.
   **/
  def exceptionName(implicit isabelle: Isabelle, ec: ExecutionContext): String = {
    init(); _exceptionName }

  /** [[MLValueConverter]] for [[T]]. This [[MLValueConverter]] does not need to be explicitly imported
   * to be available for implicit instances of [[MLValueConverter]]`[T]` because it is in the
   * "implicit scope" of [[T]] (cf. the
   * [[https://www.scala-lang.org/files/archive/spec/2.13/07-implicits.html#implicit-parameters Scala spec]]). */
  // Implicit can be used without importing this.converter. Inspired by https://stackoverflow.com/a/64105099/2646248
  implicit object converter extends MLValue.Converter[T] {
    override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String =
      AdHocConverter.this.mlType

    override def retrieve(value: MLValue[T])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[T] =
      Future.successful(new T(value))

    override def store(value: T)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[T] =
      value.mlValue

    override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String =
      s"fn ${exceptionName} x => x"

    override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String =
      exceptionName
  }

  protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
    isabelle.executeMLCodeNow(s"exception ${_exceptionName} of ($mlType)")
  }

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext) = new Ops
}
