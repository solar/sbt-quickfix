package com.dscleaver.sbt.quickfix

import com.dscleaver.sbt.Neovim
import sbt._

object QuickFixLogger {
  def append(output: File, prefix: String, message: String): Unit = 
    IO.append(output, "[%s] %s\n".format(prefix, message))

  def append(output: File, prefix: String, file: File, line: Int, message: String): Unit = 
    append(output, prefix, "%s:%d: %s".format(file, line, message))
}

class QuickFixLogger(val output: File, addr: Option[String], port: Option[Int]) extends BasicLogger {
  import QuickFixLogger._

  def log(level: Level.Value, message: => String): Unit = level match {
    case Level.Info => handleInfoMessage(message)
    case Level.Error => handleErrorMessage(message)
    case Level.Warn => handleWarnMessage(message)
    case _ => handleDebugMessage(message)
  }

  def handleDebugMessage(message: String) = {
    addr.zip(port).foreach { case (a, p) =>
      if (message.toLowerCase.contains("compilation failed")) {
        Neovim.connect(a, p) { neovim =>
          neovim.setScalaErrorFormat()
          neovim.setErrorFile(output.toString)
        }
      }
    }
  }

  def handleInfoMessage(message: String) = {
    if (message startsWith "Compiling") {
      IO.delete(output)
      IO.touch(List(output))
    } else ()
  }

  def handleErrorMessage(message: String) = append(output, "error", message)

  def handleWarnMessage(message: String) = append(output, "warn", message)

  def control(event: ControlEvent.Value, message: => String): Unit = ()

  def logAll(events: Seq[LogEvent]): Unit = ()

  def success(message: => String): Unit = ()

  def trace(t: => Throwable): Unit = ()
}
