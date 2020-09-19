package de.unruh.isabelle.mlvalue

import scala.concurrent.{ExecutionContext, Future}

trait FutureValue {
  def force : this.type = { await; this }
  //noinspection UnitMethodIsParameterless
  def await : Unit
  def forceFuture(implicit ec: ExecutionContext) : Future[this.type] =
    for (_ <- someFuture) yield this
  def someFuture(implicit ec: ExecutionContext) : Future[Any]
}
