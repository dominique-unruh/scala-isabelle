package de.unruh.isabelle.control

import java.io.{BufferedReader, FileInputStream, FileReader}
import java.nio.file.{Files, Path, Paths}
import de.unruh.isabelle.control.Isabelle.{DInt, DList, DString, Data, Setup, SetupGeneral}
import de.unruh.isabelle.control.IsabelleTest.{isabelle, setup}
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.mlvalue.MLValueTest.await
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.text.StringEscapeUtils
import org.scalatest.concurrent.{Signaler, ThreadSignaler}
import org.scalatest.concurrent.TimeLimits.failAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Awaitable, Future}
import scala.concurrent.duration.Duration
import scala.sys.process.Process

class IsabelleTest extends AnyFunSuite {
  private def await[A](f: Awaitable[A]) : A = Await.result(f, Duration.Inf)

  test("handle compilation error") {
    assertThrows[IsabelleMLException] {
      isabelle.executeMLCodeNow("1+true")
    }
  }

  test("initialize Isabelle") {
    isabelle.await
  }

  test("executeMLCode") {
    println("Sending code")
    val exec = isabelle.executeMLCode("val _ = tracing \"Hello\"")
    println("Waiting for completion")
    await(exec)
  }

  test("executeMLCode – declare symbol") {
    val variable = Utils.freshName("variable")
    println(s"Declaring $variable")
    isabelle.executeMLCodeNow(s"""val $variable = "hello"""")
    println(s"Using $variable")
    isabelle.executeMLCodeNow(s"val _ = tracing $variable")
  }

  lazy val identityId: Isabelle.ID = await(isabelle.storeValue("E_Function (fn x => x)"))
  def roundTrip(data: Data): Unit = {
    val future = isabelle.applyFunction(identityId, data)
    val returned = await(future)
    assert(returned == data)
  }

  test("roundtrip DInt") {
    roundTrip(DInt(4356))
  }

  test("roundtrip DString") {
    roundTrip(DString("hello"))
  }

  test("roundtrip DInt negative") {
    roundTrip(DInt(-235345))
  }

  test("roundtrip DList") {
    roundTrip(DList(DInt(1), DString("2")))
  }

  test("non ASCII string") {
    val result = await(isabelle.applyFunction(identityId, DString("eĥoŝanĝo ĉiuĵaŭde")))
    assert(result == DString("e?o?an?o ?iu?a?de"))
  }

  test("too long string") {
    val str = "x".repeat(70*1000*1000)
    assertThrows[IsabelleMLException] {
      roundTrip(DString(str))
    }
  }

  // Checks that the protocol doesn't get desynced by too long strings.
  test("too long string & continue") {
    val str = "x".repeat(70*1000*1000)
    assertThrows[IsabelleMLException] {
      roundTrip(DString(str))
    }
    println("Roundtrip of string finished")

    implicit val signaler: ThreadSignaler.type = ThreadSignaler
    failAfter(Span(30, Seconds)) {
      roundTrip(DInt(0))
    }
  }

  test("destroy & wait for a future") {
    implicit val isabelle: Isabelle = new Isabelle(IsabelleTest.setup)
    // Basically never finishes
    val slowComputation = isabelle.storeValue("OS.Process.sleep (Time.fromSeconds 1000000000); Match")
    isabelle.destroy()
    assertThrows[IsabelleDestroyedException] {
      await(slowComputation)
    }
  }

  test("Isabelle fails to start") {
    val setup = IsabelleTest.setup.copy(logic="NonexistingLogic")
    val isabelle = new Isabelle(setup)
    val id = isabelle.storeValue("Match")
    val exn = intercept[IsabelleDestroyedException] {
      await(id)
    }
    println(exn)
    assert(exn.message.contains("Bad parent session"))
  }

  test("correct working directory") {
    val id = isabelle.storeValue("E_Function (fn _ => DString (OS.FileSys.getDir ()))")
    val DString(dir) = await(isabelle.applyFunction(id, DList()))
    println(dir)
    assert(Paths.get(dir).normalize.toAbsolutePath
      == setup.workingDirectory.normalize.toAbsolutePath)
  }

  test("build") {
    import StringEscapeUtils.escapeXSI
    import scala.collection.JavaConverters._

    // Deleting all existing heaps called ScalaIsabelleTestSession
    for (dir <- List(setup.isabelleHomeAbsolute, setup.userDirAbsolute);
         if Files.isDirectory(dir);
         dir2 <- Seq(dir) ++ Files.list(dir).iterator().asScala; // "", "*/"
         dir3 = dir2.resolve("heaps");
         if Files.isDirectory(dir3);
         dir4 <- Files.list(dir3).iterator().asScala; // "", "*/"
         file = dir4.resolve("ScalaIsabelleTestSession");
         if Files.exists(file)) {
      println(s"Deleting $file")
      Files.delete(file)
    }

//    assert(Process(List("bash","-c",s"rm -vf ${escapeXSI(setup.isabelleHomeAbsolute.toString)}/heaps/*/ScalaIsabelleTestSession")).! == 0)
//    assert(Process(List("bash","-c",s"rm -vf ${escapeXSI(setup.userDirAbsolute.toString)}/*/heaps/*/ScalaIsabelleTestSession")).! == 0)

    // Building ScalaIsabelleTestSession again
    val isabelle = new Isabelle(setup.copy(logic = "ScalaIsabelleTestSession", build = true, sessionRoots = List(Path.of("."))))

    // To check if some exception is thrown
    isabelle.executeMLCodeNow("1")

    isabelle.destroy()
  }

  test("exception arguments are preserved in 'executeMLCodeNow'") {
    println(1)
    val exn = intercept[IsabelleMLException] {
      println(2)
      isabelle.executeMLCodeNow("""raise TERM ("magic"^"string", [Free("magic"^"var", dummyT)])""")
      println(3)
    }
    println(4)
    println(exn.message)
    println(5)
    assert(exn.message.contains("magicstring"))
    assert(exn.message.contains("magicvar"))
  }

  test("exception arguments are preserved in 'storeValue'") {
    val exn = intercept[IsabelleMLException] {
      await(isabelle.storeValue("""raise TERM ("magic"^"string", [Free("magic"^"var", dummyT)])"""))
    }
    println(exn.message)
    assert(exn.message.contains("magicstring"))
    assert(exn.message.contains("magicvar"))
  }

  test("exception arguments are preserved in 'applyFunction'") {
    val exn = intercept[IsabelleMLException] {
      val fun = await(isabelle.storeValue("""E_Function (fn _ => raise TERM ("magic"^"string", [Free("magic"^"var", dummyT)]))"""))
      val _ = await(isabelle.applyFunction(fun, DList()))
    }
    println(exn.message)
    assert(exn.message.contains("magicstring"))
    assert(exn.message.contains("magicvar"))
  }

}

object IsabelleTest {
  val isabelleHome: Path = {
    val version = "2021-1"
    val config = Paths.get(".isabelle-home") // For setting the Isabelle home in Github Action etc.
    val path = if (Files.exists(config))
      new BufferedReader(new FileReader(config.toFile)).readLine()
    else if (SystemUtils.IS_OS_WINDOWS)
      s"c:\\Isabelle$version"
    else
      s"/opt/Isabelle$version"
    Paths.get(path)
  }

  val scalaIsabelleDir: Path = {
    val cwd = Path.of("").toAbsolutePath
    if (!cwd.endsWith("scala-isabelle") && Files.exists(cwd.resolve("scala-isabelle")))
      // In this case, scala-isabelle is probably embedded as a subproject of some other project
      Path.of("scala-isabelle")
    else
      Path.of("")
  }

  val setup: Setup = Setup(
    isabelleHome = isabelleHome,
    sessionRoots = Nil,
    userDir = None,
    workingDirectory = scalaIsabelleDir.resolve("src/test/isabelle"),
    build=false
  )

  implicit lazy val isabelle: Isabelle = {
    println("Starting Isabelle")
    val isa = new Isabelle(setup)
    println("Initialized.")
    isa
  }
}


