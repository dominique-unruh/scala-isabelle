package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.{FutureValue, Utils}

import scala.concurrent.{ExecutionContext, Future}

/** Base trait to simplify creating classes that contain a reference to a value in the Isabelle process.
 * Classes inheriting from this trait contain an [[MLValue]] that refers to a value in the Isabelle process.
 * (That is, unless the inheriting class specifies further fields, this is the only data carried by the class.
 * In particular, the class will not transfer data from the Isabelle process.)
 *
 * Examples of classes based on [[MLValueWrapper]] are [[pure.Context]], [[pure.Thm]], [[pure.Position]].
 *
 * The minimal implementation of a class based on [[MLValueWrapper]] is as follows:
 * {{{
 * final class Something private (val mlValue: MLValue[Something]) extends MLValueWrapper[Something]
 *
 * object Something extends MLValueWrapper.Companion[Something] {
 *   override protected val mlType: String = "Something"
 *   override protected def instantiate(mlValue: MLValue[Something]): Something = new Something(mlValue)
 * }
 * }}}
 * Here `Something` is the name of the Scala class that we want to implement, and `something` is the ML type
 * of the corresponding values in the Isabelle process.
 *
 * After importing `Something.`[[MLValueWrapper.Companion.converter converter]], the type `Something` can be
 * transparently used to refer to `something` in the Isabelle process. E.g.,
 * [[MLValue.compileFunction[D,R]* MLValue.compileFunction]]`[String,Something]` or [[MLValue.apply MLValue]]`(st)` for `st` of type `Something`
 * or `mlst.`[[MLValue.retrieveNow retrieveNow]] for `mlst` of type [[MLValue]]`[Something]`.
 *
 * The class `Something` may be customized in several ways:
 *  - By default, an ML value `st : something` is encoded as an exception (see the description of [[MLValue]])
 *    as `EXN st` where `EXN` is an ML exception whose name is returned by
 *    [[MLValueWrapper.Companion.exceptionName Something.exceptionName]]. The name `EXN` is automatically chosen
 *    and the exception declared. If this is not desired, a different exception name can be provided by defining
 *    [[MLValueWrapper.Companion.predefinedException predefinedException]] in the companion object `Something`.
 *  - Arbitrary additional methods and fields can be added to the definition of the class `Something` or
 *    the companion object `Something`. However, it should be noted that the constructor of `Something` can
 *    only have arguments [[MLValueWrapper.mlValue mlValue]]` : `[[MLValue]]`[Something]` and optionally
 *    an [[control.Isabelle Isabelle]] instance and an [[scala.concurrent.ExecutionContext ExecutionContext]]
 *    and not other user defined values. (This is
 *    because the constructor needs to be invoked by [[MLValueWrapper.Companion.instantiate Something.instantiate]]
 *    which does not have access to other data.)
 *  - To implement functions that invoke ML code in the Isabelle process, it is recommended to place the compiled
 *    values inside an `Ops` class (see [[control.OperationCollection OperationCollection]]) in the companion object
 *    `Something`.
 *    Since the base trait [[MLValueWrapper.Companion]] already defines `Ops`, this needs to be done as follows:
 *    {{{
 *    override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops
 *    protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) extends super.Ops {
 *      // Example:
 *      lazy val test: MLFunction[Something,String] = compileFunction[Something,String]("... : something -> string")
 *    }
 *    }}}
 *
 * The class `Something` represents an asynchronous value. That is, it is possible to have get an instance
 * of `Something` even if the computation computing the corresponding ML value is still ongoing or has failed.
 * To wait for that computation to terminate successfully, use the methods from [[FutureValue]] which `Something`
 * inherits.
 *
 * Note: Another possibility for defining wrapper classes for [[MLValue]]s is [[AdHocConverter]]. Classes derived
 * using [[AdHocConverter]] cannot be customized but only minimal boilerplate is needed.
 *
 **/
trait MLValueWrapper[A <: MLValueWrapper[A]] extends FutureValue {
  /** An [[MLValue]] referencing `this` (in the Isabelle process). Since `this` is just a thin
   * wrapper around an [[MLValue]], [[mlValue]] carries exactly the same data as `this` and can be converted back
   * and forth quickly (no transfer of data to/from the Isabelle process involved).
   *
   * Same as [[MLValue.apply MLValue]]`(this)`, assuming the correct [[MLValue.Converter]] from the
   * companion object for `A` is imported.
   * */
  val mlValue : MLValue[A]
  override def await: Unit = mlValue.await
  override def someFuture: Future[Any] = mlValue.someFuture
}

object MLValueWrapper {
  /** Base trait for companion objects of classes inheriting from [[MLValueWrapper]].
   * See the class documentation of [[MLValueWrapper]] for a usage example. */
  trait Companion[A <: MLValueWrapper[A]] extends OperationCollection {
    /** Must return the ML type corresponding to `A`. */
    protected val mlType: String
    /** Must return the name of the exception for storing ML values of type [[mlType]] in
     * the object store in the Isabelle process. If not overwritten, the exception will be generated automatically.
     * If overwritten, the overriding class needs to ensure that the exception is actually declared in the ML toplevel.
     **/
    protected val predefinedException: String = null

    private lazy val _exceptionName: String =
      if (predefinedException==null)
        Utils.freshName("E_" + mlType)
      else
        predefinedException

    /** Must return a new instance of `A` with `A.`[[mlvalue.MLValueWrapper.mlValue mlValue]] = `mlValue`. */
    protected def instantiate(mlValue: MLValue[A]) : A

    /** Returns the name of the exception for storing values in the object store.
     * That is, if `exceptionName` returns `EXN`, then an ML value `a` of type [[mlType]] is stored in the
     * object store as `EXN a`. If [[predefinedException]] is overwritten, this returns [[predefinedException]].
     * Otherwise it returns a fresh exception name (and ensures that that exception is declared).
     **/
    final def exceptionName(implicit isabelle: Isabelle, ec: ExecutionContext): String = {
      init(); _exceptionName
    }

    /** A class for storing [[control.Isabelle Isabelle]]-instance-dependent declarations. See
     * [[control.OperationCollection OperationCollection]].
     * The `Ops` be overridden but must then inherit from this `Ops` class (`extends super.Ops`). */
    protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
      if (predefinedException==null)
        isabelle.executeMLCodeNow(s"exception ${_exceptionName} of ($mlType)")
    }

    /** Must return a fresh instance of [[Ops]] (with implicit arguments `isabelle` and `ec`. */
    override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops

    /** The [[MLValue.Converter]] for `A`. Import this as an implicit to support using objects of
     * type `A` in [[MLFunction]]s etc. */
    final implicit object converter extends MLValue.Converter[A] {
      override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = Companion.this.mlType

      override def retrieve(value: MLValue[A])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[A] =
        Future.successful(instantiate(value))

      override def store(value: A)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[A] =
        value.mlValue

      override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = s"fn $exceptionName x => x"

      override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = exceptionName
    }
  }
}
