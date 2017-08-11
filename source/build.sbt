val scalaVer = "2.11.7"
val akkaVersion = "2.4.17"

organization := "com.cqrs"
name := "cqrs"
version := "1.0"

scalaVersion := scalaVer
autoScalaLibrary := false

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/maven-releases/"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:postfixOps",
  "-target:jvm-1.8")

parallelExecution in ThisBuild := false

parallelExecution in Test := false

logBuffered in Test := false

unmanagedBase := baseDirectory.value / "project/lib"

//assemblyJarName := s"$name-$version.jar"

libraryDependencies ++= 
  Seq(
    "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion % Provided,
    "com.typesafe.akka" % "akka-cluster_2.11" % akkaVersion % Provided,
    "com.typesafe.akka" % "akka-contrib_2.11" % akkaVersion % Provided,
    "com.typesafe.akka" % "akka-kernel_2.11" % akkaVersion % Provided,
    "com.typesafe.akka" % "akka-protobuf_2.11" % akkaVersion % Provided,
    "com.typesafe.akka" % "akka-remote_2.11" % akkaVersion % Provided,
    "com.typesafe.akka" % "akka-slf4j_2.11" % akkaVersion % Provided,
    "com.typesafe.akka" % "akka-stream_2.11" % akkaVersion % Provided,
  )