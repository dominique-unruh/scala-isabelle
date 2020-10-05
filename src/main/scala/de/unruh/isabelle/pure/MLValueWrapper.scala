package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.{FutureValue, MLValue}

import scala.concurrent.{ExecutionContext, Future}

// DOCUMENT
trait MLValueWrapper[A <: MLValueWrapper[A]] extends FutureValue {
  val mlValue : MLValue[A]
  override def await: Unit = mlValue.await
  override def someFuture: Future[Any] = mlValue.someFuture
}
object MLValueWrapper {

  trait Companion[A <: MLValueWrapper[A]] extends OperationCollection {
    protected val mlType: String
    protected val _exceptionName: String = Utils.freshName("E_" + mlType)
    protected def instantiate(mlValue: MLValue[A]) : A

    def exceptionName(implicit isabelle: Isabelle, ec: ExecutionContext): String = {
      init(); _exceptionName
    }

    protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
      isabelle.executeMLCodeNow(s"exception ${_exceptionName} of ($mlType)")
    }

    override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops

    object converter extends MLValue.Converter[A] {
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
