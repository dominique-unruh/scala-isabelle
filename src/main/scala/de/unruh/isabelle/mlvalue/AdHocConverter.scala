package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.Utils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

// DOCUMENT
abstract class AdHocConverter protected(val mlType: String) extends OperationCollection {
  private val tString = s"‹$mlType›"

  final class T private[AdHocConverter](val mlValue: MLValue[T]) extends FutureValue {
    override def await: Unit = mlValue.await
    override def someFuture: Future[Any] = mlValue.someFuture
    override def toString: String = tString
  }

  private val _exceptionName: String = Utils.freshName("E_" + mlType)

  def exceptionName(implicit isabelle: Isabelle, ec: ExecutionContext): String = {
    init(); _exceptionName }

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
