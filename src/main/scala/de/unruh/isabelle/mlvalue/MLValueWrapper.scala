package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.Utils

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
    protected val predefinedException: String = null
    private lazy val _exceptionName: String =
      if (predefinedException==null)
        Utils.freshName("E_" + mlType)
      else
        predefinedException
    protected def instantiate(mlValue: MLValue[A]) : A

    def exceptionName(implicit isabelle: Isabelle, ec: ExecutionContext): String = {
      init(); _exceptionName
    }

    protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
      if (predefinedException==null)
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
