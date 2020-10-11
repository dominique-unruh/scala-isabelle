package de.unruh.isabelle.experiments

import java.nio.file.{Path, Paths}

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.experiments.ExecuteIsar.{command_exception, init_toplevel, parse_text, theorySource, toplevel_end_theory}
import de.unruh.isabelle.experiments.TheoryManager.{Heap, Source, Text}
import de.unruh.isabelle.mlvalue.MLValue.compileFunction
import de.unruh.isabelle.pure.{Position, Theory, TheoryHeader}
import TheoryManager.Ops

import scala.concurrent.ExecutionContext

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

class TheoryManager {
  def getTheorySource(name: String): Source = Heap(name)
  def getTheory(source: Source)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory = source match {
    case Heap(name) => Theory(name)
    case Text(text, path, position) =>
      var toplevel = init_toplevel().force.retrieveNow
      var thy0 = beginTheory(source)
      for ((transition, text) <- parse_text(thy0, text).force.retrieveNow) {
        toplevel = command_exception(true, transition, toplevel).retrieveNow.force
      }
      toplevel_end_theory(toplevel).retrieveNow.force
  }

  def beginTheory(source: Source)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory = {
    val header = getHeader(source)
    val masterDir = source.path.getParent
    Ops.begin_theory(masterDir, header, header.imports.map(getTheorySource).map(getTheory)).retrieveNow
  }
  def getHeader(source: Source)(implicit isabelle: Isabelle, ec: ExecutionContext): TheoryHeader = source match {
    case Text(text, path, position) => Ops.header_read(text, position).retrieveNow
  }
}

object TheoryManager extends OperationCollection {

  trait Source { def path : Path }
  case class Heap(name: String) extends Source {
    override def path: Path = Paths.get("INVALID")
  }
  case class File(path: Path) extends Source
  case class Text(text: String, path: Path, position: Position) extends Source
  object Text {
    def apply(text: String, path: Path)(implicit isabelle: Isabelle, ec: ExecutionContext): Text = new Text(text, path, Position.none)
  }

  //noinspection TypeAnnotation
  protected final class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
    val header_read = compileFunction[String, Position, TheoryHeader]("fn (text,pos) => Thy_Header.read pos text")
    val begin_theory = compileFunction[Path, TheoryHeader, List[Theory], Theory](
      "fn (path, header, parents) => Resources.begin_theory path header parents")
  }

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext) = new this.Ops
}