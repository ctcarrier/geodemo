resolvers ++= Seq(
"Web plugin repo" at "http://siasia.github.com/maven2",
Classpaths.typesafeResolver,
"spray repo" at "http://repo.spray.cc"
)

addSbtPlugin("com.typesafe.startscript" % "xsbt-start-script-plugin" % "0.5.3")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.7.2")

addSbtPlugin("cc.spray" % "sbt-revolver" % "0.6.1")