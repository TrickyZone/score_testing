addCommandAlias("check", "compile:scalafix --check; test:scalafix --check")
addCommandAlias("fix", "compile:scalafix; test:scalafix; scalafmtSbt; scalafmtAll")

lazy val akkaHttpVersion = "10.2.9"
lazy val akkaVersion    = "2.6.19"
val jdbcAndLiftJsonVersion = "3.4.1"
val flywayCore = "8.5.13"
val keycloakVersion = "4.0.0.Final"
lazy val doobieVersion = "1.0.0-RC1"
val jacksonVersion = "2.13.2"
val swaggerVersion = "2.2.0"
val embeddedPostgresVersion = "1.0.1"

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true
// javaOptions in run += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007"
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

val swaggerDependencies = Seq(
  "jakarta.ws.rs" % "jakarta.ws.rs-api" % "3.0.0",
  "com.github.swagger-akka-http" %% "swagger-akka-http" % "2.7.0",
  "com.github.swagger-akka-http" %% "swagger-scala-module" % "2.6.0",
  "com.github.swagger-akka-http" %% "swagger-enumeratum-module" % "2.4.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "io.swagger.core.v3" % "swagger-jaxrs2-jakarta" % swaggerVersion
)

/**
 * Leave out swaggerUIDependencies if you don't want to include the swaggerUI.
 * See also SwaggerDocService
 */
val swaggerUIDependencies = Seq(
  "org.webjars" % "webjars-locator" % "0.45",
  "org.webjars" % "swagger-ui" % "4.6.2",
)

val embeddedPostgres = Seq("com.opentable.components" % "otj-pg-embedded" % embeddedPostgresVersion % Test)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.example",
      scalaVersion    := "2.13.4",
      semanticdbEnabled := true, // enable SemanticDB
      semanticdbVersion := scalafixSemanticdb.revision // only required for Scala 2.x
    )),
    name := "scoring-service",
    scalacOptions += "-Wunused:imports", // Scala 2.x only, required by `RemoveUnused`
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3",
      "ch.megard" %% "akka-http-cors" % "1.1.2",
      "com.github.jwt-scala" %% "jwt-circe" % "9.0.1",
      "org.keycloak"      % "keycloak-core"         % keycloakVersion,
      "org.keycloak"      % "keycloak-adapter-core" % keycloakVersion,
      "org.jboss.logging" % "jboss-logging"         % "3.3.0.Final" % Runtime,
      "org.keycloak" % "keycloak-admin-client" % "12.0.2",
      //flyway
      "org.flywaydb" % "flyway-core" % flywayCore,
      "com.github.jwt-scala" %% "jwt-circe" % "9.0.1",
      //doobie
      "org.tpolecat" %% "doobie-core"     % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "com.github.pureconfig" %% "pureconfig" % "0.17.1",
      "com.rabbitmq" % "amqp-client" % "5.12.0",
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-specs2"   % doobieVersion  % Test,
      "com.opentable.components" % "otj-pg-embedded" % "1.0.1" % Test,
      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.1.4"         % Test,
      "org.mockito" %% "mockito-scala" % "1.11.4" % Test
    ) ++ swaggerDependencies ++ swaggerUIDependencies ++ embeddedPostgres
  )
coverageExcludedPackages := ".*QuickstartApp*;.*SwaggerHttpUiService*;.*SwaggerSite*;.*ResourceManager*;.*SwaggerHttpWithUiService*;.*FlywayService*;.*RabbitMQConnection*"


sonarProperties := Map(
  "sonar.host.url" -> "http://3.88.10.34:9000/",
  "sonar.login" -> "sqp_0e7cc6faba6bd723c14b51bfc3f99ea4bfc79dad",
  "sonar.projectName" -> "first-project",
  "sonar.projectKey" -> "first-project",
  "sonar.language" -> "scala",
  "sonar.sources" -> "src/main/scala",
  "sonar.tests" -> "src/test/scala",
  "sonar.scala.scalastyle.reportPaths" -> "target/scalastyle-result.xml",
  "sonar.scala.coverage.reportPaths" -> "target/scala-2.12/scoverage-report/scoverage.xml",
  "sonar.coverage.exclusions" -> "**/Mailing.scala,**/Api.scala,**/MongoRequire.scala,**/GithubHelper.scala,**/authorization/*.scala,**/rabbitMQConnection/*.scala,**/models/*.scala",
  "sonar.scala.scapegoat.reportPaths" -> "target/scala-2.12/scapegoat-report/scapegoat-scalastyle.xml",
  "sonar.java.binaries" -> "target/scala-2.13/classes",
  "sonar.externalIssuesReportPaths" -> "sonarqube.json"
)