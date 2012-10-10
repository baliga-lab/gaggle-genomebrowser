name         := "genomebrowser-core"

version      := "1.0"

organization := "org.systemsbiology"

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "2.0.M4" % "test",
                            "junit" % "junit" % "4.10" % "test",
                            "log4j" % "log4j" % "1.2.17")
