sbtPlugin := true

name := "sbt-quickfix-nvim"

organization := "com.dscleaver.sbt"

versionWithGit

version := "0.5.0"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")

publishMavenStyle := false

libraryDependencies ++= Seq(
  "org.msgpack" % "msgpack-core" % "0.8.2",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

publishTo <<= (version) { v =>
  def scalasbt(repo: String) = ("scalasbt " + repo, "http://repo.scala-sbt.org/scalasbt/sbt-plugin-" + repo)
  val (name, repo) = if (v.endsWith("-SNAPSHOT")) scalasbt("snapshots") else scalasbt("releases")
  Some(Resolver.url(name, url(repo))(Resolver.ivyStylePatterns))
}
