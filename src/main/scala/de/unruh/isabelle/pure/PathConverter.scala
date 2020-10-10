package de.unruh.isabelle.pure

import java.nio.file.{Path, Paths}

import de.unruh.isabelle.control.Isabelle.DString
import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.{MLRetrieveFunction, MLStoreFunction, MLValue}

import scala.concurrent.{ExecutionContext, Future}

// Implicits
import Implicits._

object PathConverter extends MLValue.Converter[Path] with OperationCollection {
  def exceptionName(implicit isabelle: Isabelle, ec: ExecutionContext): String = "E_Path"

  override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = "Path.T"

  override def retrieve(value: MLValue[Path])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Path] =
    for (DString(path) <- Ops.retrievePath(value))
      yield Paths.get(path)

  override def store(value: Path)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Path] =
    Ops.storePath(DString(value.toString))

  override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = s"fn $exceptionName path => path"

  override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = exceptionName

  //noinspection TypeAnnotation
  protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
    lazy val retrievePath = MLRetrieveFunction[Path]("DString o Path.implode")
    lazy val storePath = MLStoreFunction[Path]("fn DString path => Path.explode path")
  }
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops
}
