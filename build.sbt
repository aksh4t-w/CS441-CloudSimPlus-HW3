ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "CS441-CloudSimPlus-HW3"
  )

val logbackVersion = "1.4.1"
val cloudSimVersion = "7.3.0"
val typesafeConfigVersion = "1.4.2"

libraryDependencies ++= Seq(
  "org.cloudsimplus" % "cloudsim-plus" % cloudSimVersion,
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.typesafe" % "config" % typesafeConfigVersion,
)