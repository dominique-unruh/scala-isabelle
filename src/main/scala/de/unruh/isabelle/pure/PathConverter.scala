package de.unruh.isabelle.pure

import java.nio.file.{Path, Paths}

import de.unruh.isabelle.control.Isabelle.DString
import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.{MLRetrieveFunction, MLStoreFunction, MLValue}
import de.unruh.isabelle.pure.PathConverter.{Ops, exceptionName}

import scala.concurrent.{ExecutionContext, Future}

// Implicits
import Implicits._

object PathConverter extends MLValue.Converter[Path] with OperationCollection {
  protected val _exceptionName: String = Utils.freshName("E_Path")
  def exceptionName(implicit isabelle: Isabelle, ec: ExecutionContext): String = Ops.exceptionName

  override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = "Path.T"

  override def retrieve(value: MLValue[Path])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Path] =
    for (DString(path) <- Ops.retrievePath(value))
      yield Paths.get(path) // TODO: do some conversion

  override def store(value: Path)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Path] =
    Ops.storePath(DString(value.toString)) // TODO do some conversion

  override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = s"fn $exceptionName path => path"

  override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = exceptionName

  //noinspection TypeAnnotation
  protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
    def exceptionName = _exceptionName
    // TODO: Move to Control_Isabelle
    isabelle.executeMLCodeNow(s"exception ${_exceptionName} of Path.T")
    lazy val retrievePath = MLRetrieveFunction[Path]("DString o Path.implode")
    lazy val storePath = MLStoreFunction[Path]("fn DString path => Path.explode path")
  }
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops
}
