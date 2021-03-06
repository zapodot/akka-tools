
val publishToNexus = true


lazy val commonSettings = Seq(
  organization := "no.nextgentel.oss.akka-tools",
  organizationName := "NextGenTel AS",
  organizationHomepage := Some(url("http://www.nextgentel.net")),
  version := "0.9.0-RC5-SNAPSHOT",
  scalaVersion := "2.11.6",
  //publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  homepage := Some(url("https://github.com/NextGenTel/akka-tools")),
  licenses := Seq("MIT" -> url("https://github.com/NextGenTel/akka-tools/blob/master/LICENSE.txt")),
  startYear := Some(2015),
  pomExtra := (
      <scm>
        <url>git@github.com:NextGenTel/akka-tools.git</url>
        <connection>scm:git:git@github.com:NextGenTel/akka-tools.git</connection>
      </scm>
      <developers>
        <developer>
          <id>mbknor</id>
          <name>Morten Kjetland</name>
          <url>https://github.com/mbknor</url>
        </developer>
      </developers>)
)


val akkaVersion = "2.3.12"
val jacksonVersion = "2.4.6"
val jacksonScalaModuleVersion = "2.4.5"
val slf4jVersion = "1.7.7"

lazy val akkaToolsCommonDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.slf4j" % "slf4j-api" % slf4jVersion)

lazy val akkaToolsPersistenceDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "com.google.guava" % "guava" % "18.0")

lazy val akkaToolsJsonSerializingDependencies = Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonScalaModuleVersion exclude("org.scala-lang", "scala-reflect")
)

lazy val akkaToolsJdbcJournalDependencies = Seq(
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
  "org.sql2o" % "sql2o" % "1.5.2"
)

lazy val akkaToolsClusterDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "org.slf4j" % "slf4j-api" % slf4jVersion
)

lazy val testDependencies = Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion % "test"
)

lazy val exampleAggregatesDependencies = Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

lazy val root = (project in file("."))
  .settings(name := "akka-tools-parent")
  .settings(commonSettings: _*)
  .aggregate(
    akkaToolsCommon,
    akkaToolsPersistence,
    akkaToolsJsonSerializing,
    akkaToolsJdbcJournal,
    akkaToolsCluster,
    akkaExampleAggregates)

lazy val akkaToolsCommon = (project in file("akka-tools-common"))
  .settings(name := "akka-tools-common")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= (akkaToolsCommonDependencies))

lazy val akkaToolsPersistence = (project in file("akka-tools-persistence"))
  .settings(name := "akka-tools-persistence")
  .settings(commonSettings: _*)
  .dependsOn(akkaToolsCommon)
  .settings(libraryDependencies ++= (akkaToolsPersistenceDependencies))
  .settings(libraryDependencies ++= (testDependencies))
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.github.michaelpisula" %% "akka-persistence-inmemory" % "0.2.1" % "test" exclude("com.github.krasserm", "akka-persistence-testkit_2.11") exclude("com.typesafe.akka", "akka-persistence-experimental_2.11")))


lazy val akkaToolsJsonSerializing = (project in file("akka-tools-json-serializing"))
  .settings(name := "akka-tools-json-serializing")
  .dependsOn(akkaToolsCommon)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= (akkaToolsJsonSerializingDependencies))
  .settings(libraryDependencies ++= (testDependencies))


lazy val akkaToolsJdbcJournal = (project in file("akka-tools-jdbc-journal"))
  .settings(name := "akka-tools-jdbc-journal")
  .dependsOn(akkaToolsCommon)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= (akkaToolsJdbcJournalDependencies))

lazy val akkaToolsCluster = (project in file("akka-tools-cluster"))
  .settings(name := "akka-tools-cluster")
  .dependsOn(akkaToolsCommon)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= (akkaToolsClusterDependencies))
  .settings(libraryDependencies ++= (testDependencies))

lazy val akkaExampleAggregates = (project in file("examples/aggregates"))
  .settings(name := "example-aggregates")
  .dependsOn(akkaToolsPersistence)
  .dependsOn(akkaToolsCluster)
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= (exampleAggregatesDependencies))
  .settings(libraryDependencies ++= (testDependencies))
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.github.michaelpisula" %% "akka-persistence-inmemory" % "0.2.1" % "test" exclude("com.github.krasserm", "akka-persistence-testkit_2.11") exclude("com.typesafe.akka", "akka-persistence-experimental_2.11")))
