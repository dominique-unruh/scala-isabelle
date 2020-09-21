package de.unruh.isabelle.mlvalue

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// TODO document
trait FutureValue {
  /** Waits till the computation of this value (in the Isabelle process) has finished.
   * (Or until an exception is thrown.)
   * @return this value, but it is guaranteed to have completed the computation
   **/
  def force : this.type = { await; this }
  //noinspection UnitMethodIsParameterless
  def await : Unit

  /** A future containing this value with the computation completed.
   * In particular, if this value throws an exception upon computation,
   * the future holds that exception.
   *
   * Roughly the same as `[[scala.concurrent.Future.apply Future]] { this.[[force]] }`.
   */
  def forceFuture(implicit ec: ExecutionContext) : Future[this.type] =
    for (_ <- someFuture) yield this
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
