package com.dscleaver.sbt

import java.net.Socket
import org.msgpack.core._
import org.msgpack.value.ValueFactory
import sbt._
import scala.collection.JavaConverters._

class Neovim private {
  private val packer = MessagePack.newDefaultBufferPacker()

  def setOption(name: String, value: String): Unit = {
    packer.packArrayHeader(3)
      .packInt(2)
      .packString("vim_set_option")
      .packArrayHeader(2)
      .packString(name)
      .packString(value)
  }

  def setErrorFile(path: String): Unit = setOption("errorfile", path)

  def setScalaErrorFormat(): Unit = {
    val format = """%E %#[error] %#%f:%l: %m,%-Z %#[error] %p^,%-C %#[error] %m""" +
      """,%W %#[warn] %#%f:%l: %m,%-Z %#[warn] %p^,%-C %#[warn] %m""" +
      """,%-G%.%#"""
    setOption("errorformat", format)
  }

  def pack(f: MessagePacker => Unit): Unit = f(packer)

  def close(): Unit = {
    packer.close()
  }
}

object Neovim {
  def connect[A](addr: String, port: Int)(f: Neovim => A): A = {
    val socket = new Socket(addr, port)
    try {
      val nvim = new Neovim()

      try {
        val a = f(nvim)
        socket.getOutputStream().write(nvim.packer.toByteArray)
        a
      } finally {
        nvim.close()
      }
    } finally {
      socket.close()
    }
  }
}
