package com.dscleaver.sbt

import org.scalatest._

class NeovimSpec extends FlatSpec with Matchers {

  "Neovim" should "send command to neovim" in {
    // @TODO
    Neovim.connect("127.0.0.1", 10200) { neovim =>
      neovim.setScalaErrorFormat()
      neovim.setErrorFile("hoge")
    }
  }
}
