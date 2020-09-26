package de.unruh.isabelle.pure

import java.nio.file.Path

import de.unruh.isabelle.control.Isabelle.DString
import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.{MLRetrieveFunction, MLStoreFunction, MLValue}

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.{ExecutionContext, Future}

// Implicits
import Implicits._

// DOCUMENT
object PathConverter extends MLValue.Converter[Path] with OperationCollection {
  override def mlType: String = "Path.T"

  // TODO implement
  override def retrieve(value: MLValue[Path])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Path] = ???

  override def store(value: Path)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Path] = {
    val elements = value.iterator().asScala.map(_.toString).toList
    val path =
      if (value.isAbsolute)
        (""::elements).mkString("/")
      else
        elements.mkString("/")
    Ops.storePath(DString(path))
  }

  override def exnToValue: String = "fn E_Path path => path"

  override def valueToExn: String = "E_Path"

  //noinspection TypeAnnotation
  protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
    isabelle.executeMLCodeNow("exception E_Path of Path.T")
    val retrievePath = MLRetrieveFunction[Path]("Path.expand #> Path.implode #> DString")
    val storePath = MLStoreFunction[Path]("fn DString path => Path.explode path")
  }

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext) = new Ops
}
