lazy val `reactive-config` = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,

    organization := "com.github.achelimed",
    name := "reactive-config",
    version := "1.0.0",

    scalaVersion := Versions.scala,

    // publish using `sbt + publish` command to maintain cross version compatibility
    crossScalaVersions := Seq(scalaVersion.value, Versions.crossVersions),

    autoAPIMappings := true, // to tell scaladoc where it can find the API documentation for managed dependencies

    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-target:jvm-1.8"),

    libraryDependencies ++= Seq(
      // For core
      "com.typesafe.akka" %% "akka-actor" % Versions.akka,
      "com.typesafe" % "config" % Versions.typesafeConfig,
      // For tests
      "com.typesafe.akka" %% "akka-testkit" % Versions.akka % "it,test",
      "org.scalatest" %% "scalatest" % Versions.scalatest % "it,test",
      "com.github.pathikrit" %% "better-files" % Versions.betterFiles % "it,test"
    ),

    fork in Test := true,
    parallelExecution in Test := true,

    javaOptions in Test ++= Seq(
      "-Dreactive-config.file-watcher.interval=500 millis", // to speed tests
      s"-Dconfig.file=${(resourceDirectory in Test).value}/application-test.conf"
    ),

    fork in IntegrationTest := true,

    javaOptions in IntegrationTest ++= Seq(
      "-Dreactive-config.file-watcher.interval=500 millis", // to speed tests
      "-Dconfig.file=conf/application.conf"
    )
  )