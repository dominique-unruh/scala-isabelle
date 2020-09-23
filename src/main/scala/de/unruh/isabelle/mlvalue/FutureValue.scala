package de.unruh.isabelle.mlvalue

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/** Common trait for all classes that exhibit a future-like behavior.
 * That is, they contain a value that may not yet have been computed, and that may still throw exceptions.
 * But unlike for [[scala.concurrent.Future Future]], no methods are provided for extracting that value,
 * only to wait for it.
 */
trait FutureValue {
  /** Waits till the computation of this value (in the Isabelle process) has finished.
   * (Or until an exception is thrown.)
   *
   * @return this value, but it is guaranteed to have completed the computation
   **/
  def force : this.type = { await; this }

  /** Blocks until this future value is computed. (Or throws an exception if the computation fails.) */
  //noinspection UnitMethodIsParameterless
  def await : Unit

  /** A future containing this object with the computation completed.
   * In particular, if this value throws an exception upon computation,
   * the future holds that exception.
   *
   * Roughly the same as `[[scala.concurrent.Future.apply Future]] { this.[[force]] }`.
   */
  def forceFuture(implicit ec: ExecutionContext) : Future[this.type] =
    for (_ <- someFuture) yield this

  /** Returns a future that completes when the computation of this object is complete.
   * (Or that holds an exception if that computation throws an exception.)
   * However, upon successful completion, the future may return an arbitrary (and thus useless) value.
   * May be faster to implement than [[forceFuture]] because there may be already a future available but that returns
   * the wrong value. */
  def someFuture : Future[Any]

  /** A utility method that returns "" if this value was successfully computed, " (computing)" if it still computes,
   * and " (failed)" if it finished with an exception.
   *
   * This can be useful to constructing human readable messages about this value.
   **/
  def stateString: String = someFuture.value match {
    case Some(Success(_)) => ""
    case Some(Failure(_)) => " (failed)"
    case None => " (computing)"
  }
}
