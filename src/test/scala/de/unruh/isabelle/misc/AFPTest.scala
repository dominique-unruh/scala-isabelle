package de.unruh.isabelle.misc

import org.scalatest.funsuite.AnyFunSuite

import java.io.{BufferedReader, FileReader}
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDate
import java.util.{Calendar, Date, GregorianCalendar}

class AFPTest extends AnyFunSuite {
  val afpHome: Path = {
    val config = Paths.get(".afp-home") // For setting the Isabelle home in Github Action etc.
    if (Files.exists(config))
      Paths.get(new BufferedReader(new FileReader(config.toFile)).readLine())
    else
      Paths.get("/opt/afp-2021-1")
  }

  test("newestSubmissionDate") {
    val afp = new AFP(afpHome)
    val afpDate = afp.newestSubmissionDate
    println(afpDate)
    assert(afpDate.isAfter(LocalDate.of(2021,11,4)))
    assert(afpDate.isBefore(LocalDate.of(2035,11,4)))
  }
}
