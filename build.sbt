
name := "SR_service"
organization := "work.lithos"
version := "0.0.1"
maintainer := "Cheese@lithos.work"
scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.ergoplatform" %% "ergo-appkit" % "5.0.4",
  "org.postgresql" % "postgresql" % "42.7.1",
  "org.scalatest" %% "scalatest" % "3.2.17" % "test",
  "io.swagger" % "swagger-annotations" % "1.6.12",
   guice,
   ws,
  "com.typesafe.play" %% "play-slick" % "5.1.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0",
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",

)

lazy val root = Project(id = "SR_service", base = file(".")).enablePlugins(PlayScala)

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)
topLevelDirectory := Some("SR_service")

import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerPermissionStrategy := DockerPermissionStrategy.CopyChown
dockerEntrypoint := Seq("/opt/docker/bin/subpooling_service", "-Dconfig.file=/opt/docker/conf/test.conf")

