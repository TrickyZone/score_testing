addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "3.4.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.8.2")

//addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.1.0")

addSbtPlugin("com.github.sbt" % "sbt-cpd" % "2.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.0")

addSbtPlugin("nl.codestar" % "sbt-findsecbugs" % "0.16")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")

//sbt plugin to load environment variables from .env into the JVM System Environment for local development.
//addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "2.1.146")

addSbtPlugin("com.github.sbt" % "sbt-jacoco" % "3.0.3")

addSbtPlugin("com.sonar-scala" % "sbt-sonar" % "2.3.0")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.1")
