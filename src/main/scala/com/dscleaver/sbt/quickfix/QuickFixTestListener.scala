package com.dscleaver.sbt.quickfix

import scala.reflect.runtime.{ universe => ru }
import scala.language.reflectiveCalls

import com.dscleaver.sbt.Neovim
import sbt._
import sbt.TestResult.Value
import sbt.testing.Status._
import sbt.testing.Event

class QuickFixTestListener(output: File, srcFiles: => Seq[File], addr: Option[String], port: Option[Int]) extends TestReportListener {
  import QuickFixLogger._

  type TFE = Exception {
    def failedCodeFileName: Option[String]
    def failedCodeLineNumber: Option[Int]
  }

  IO.delete(output)
  IO.touch(List(output))

  def startGroup(name: String): Unit = {}

  def testEvent(event: TestEvent): Unit = {
    writeFailure(event)

    addr.zip(port).foreach { case (a, p) =>
      if (event.detail.exists(e => e.status == Failure)) {
        Neovim.connect(a, p) { neovim =>
          neovim.setScalaErrorFormat()
          neovim.setErrorFile(output.toString)
        }
      }
    }
  }
 
  def endGroup(name: String, t: Throwable): Unit = {}

  def endGroup(name: String, v: Value): Unit = {}

  def writeFailure(event: TestEvent): Unit =
    for {
      detail <- event.detail
      if writeable(detail)
      (file, line) <- find(detail.throwable.get) 
    } append(output, "error", file, line, detail.throwable.get.getMessage)

  def writeable(detail: Event): Boolean =
    detail.status == Failure && detail.throwable.isDefined

  type E = { def failedCodeStackDepth: Int }

  def find(error: Throwable): Option[(File, Int)] = error match {
    case e: E =>
      try {
        val stackTrace = error.getStackTrace()(e.failedCodeStackDepth)
        for { 
          file <- findSource(stackTrace.getFileName) 
        } yield (file, stackTrace.getLineNumber)
      } catch {
        case _: Throwable => 
          findInStackTrace(error.getStackTrace)
      }
    case _ => findInStackTrace(error.getStackTrace)
  }

  def findInStackTrace(trace: Array[StackTraceElement]): Option[(File, Int)] = 
    { for {
      elem <- trace
      file <- findSource(elem.getFileName)
    } yield (file, elem.getLineNumber) }.headOption

  def findSource(name: String): Option[File] =
    srcFiles find { file => file.getName endsWith name }
}

object QuickFixTestListener {
  def apply(output: File, srcFiles: Seq[File], addr: Option[String], port: Option[Int]): TestReportListener =
    new QuickFixTestListener(output, srcFiles, addr, port)
}
