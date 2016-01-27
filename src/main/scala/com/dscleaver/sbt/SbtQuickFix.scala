package com.dscleaver.sbt

import sbt._, Keys._
import quickfix.{ QuickFixLogger, QuickFixTestListener }

object SbtQuickFix extends AutoPlugin {

  object autoImport {
    val quickFixDirectory = target.in(config("quickfix"))

    val nvimListenAddress = settingKey[Option[String]]("Neovim msgpack-rpc listen address")
    val nvimListenPort = settingKey[Option[Int]]("Neovim msgpack-rpc listen port")
  }

  import autoImport._

  override def trigger = allRequirements

  override val projectSettings = Seq(
    quickFixDirectory := target.value / "quickfix",
    nvimListenAddress in ThisBuild := {
      Option(System.getProperty("sbtquickfix.nvim_listen_address")) orElse
        Option(System.getenv("SBTQUICKFIX_NVIM_LISTEN_ADDRESS"))
    },
    nvimListenPort in ThisBuild := {
      (Option(System.getProperty("sbtquickfix.nvim_listen_port")) orElse
        Option(System.getenv("SBTQUICKFIX_NVIM_LISTEN_PORT"))).map(_.toInt)
    },
    extraLoggers <<= (quickFixDirectory, extraLoggers, nvimListenAddress, nvimListenPort) apply { (target, currentFunction, addr, port) =>
      (key: ScopedKey[_]) => {
        val loggers = currentFunction(key)
        val taskOption = key.scope.task.toOption
        if (taskOption.map(_.label.startsWith("compile")) == Some(true))
          new QuickFixLogger(target / "sbt.quickfix", addr, port) +: loggers
        else
          loggers
      }
    },
    testListeners <+= (quickFixDirectory, sources in Test, nvimListenAddress, nvimListenPort) map { (target, testSources, addr, port) =>
      QuickFixTestListener(target / "sbt.quickfix", testSources, addr, port)
    }
  )
}
