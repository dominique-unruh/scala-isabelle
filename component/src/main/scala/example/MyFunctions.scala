package example
import de.unruh.isabelle.pure.{Context, Term}
import example.MyFunctions._
import isabelle.Scala.{Fun, Functions}
import isabelle.Scala_Project

class MyFunctions extends Functions(echo, log, accessScalaIsabelle) {
}
object MyFunctions {
  object echo extends Fun("reverse") {
    override val here: Scala_Project.Here = Scala_Project.here
    override def apply(arg: String): String = arg.reverse
  }

  object log extends Fun("log") {
    override val here: Scala_Project.Here = Scala_Project.here
    override def apply(arg: String): String = {
      LogWindow.log(arg)
      "no return value"
    }
  }

  object accessScalaIsabelle extends Fun("accessScalaIsabelle") {
    override def here: Scala_Project.Here = Scala_Project.here
    override def apply(arg: String): String = {
      Term.toString
    }
  }
}

