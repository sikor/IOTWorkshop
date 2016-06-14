import sbt.Package.ManifestAttributes
import sbtassembly.AssemblyPlugin.autoImport._

name := "IOTWorkshop"

version in ThisBuild := "0.1.0-SNAPSHOT"
scalaVersion in ThisBuild := "2.11.8"
organization in ThisBuild := "com.avsystem"
crossPaths in ThisBuild := false
scalacOptions in ThisBuild ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
  "-language:existentials",
  "-language:dynamics",
  "-Xfuture",
  "-Xfatal-warnings",
  "-Xlint:_,-missing-interpolator,-adapted-args",
  "-target:jvm-1.8"
)
javacOptions in ThisBuild := Seq(
  "-source", "1.8",
  "-target", "1.8",
  "-parameters"
)

mainClass in Compile := Some("com.avsystem.iot.workshop.Launcher")
assemblyJarName in assembly := "iotworkshop.jar"
test in assembly := {}
mainClass in assembly := Some("com.avsystem.iot.workshop.Launcher")
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}
//packageOptions in assembly := Seq(ManifestAttributes(("Main-Class", "test"), ("Built-By", "team"), ("Implementation-Title", "console"), ("Implementation-Version", "1.0")))

val commonSettings = Seq[SettingsDefinition](
  mainClass in Compile := Some("com.avsystem.iot.workshop.Launcher"),
  assemblyJarName in assembly := "iotworkshop.jar",
  test in assembly := {},
  mainClass in assembly := Some("com.avsystem.iot.workshop.Launcher"),
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
    case _ => MergeStrategy.first
  },
  assemblyShadeRules in assembly := Seq(
    //    ShadeRule.zap("META-INF**.SF", "META-INF**.DSA", "META-INF**.RSA").inAll
  ),
  dependencyOverrides ++= standardDependencyOverrides.toSet

)

def crossLibs(configuration: Configuration) =
  libraryDependencies ++= crossDeps.value.map(_ % configuration)

lazy val IOTWorkshop = project.in(file("."))
  .aggregate(sharedJS, sharedJVM, frontend, backend)
  .dependsOn(backend)
  .settings(
    publishArtifact := false
  ).settings(commonSettings: _*)

lazy val shared = crossProject.crossType(CrossType.Pure).in(file("shared"))
  .settings(
    crossLibs(Provided)
  )

lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js

lazy val backend = project.in(file("backend"))
  .dependsOn(sharedJVM)
  .settings(
    libraryDependencies ++= backendDeps.value,
    crossLibs(Compile),

    compile <<= (compile in Compile),
    (compile in Compile) <<= (compile in Compile).dependsOn(copyStatics),
    copyStatics := IO.copyDirectory((crossTarget in frontend).value / StaticFilesDir, (target in Compile).value / StaticFilesDir),
    copyStatics <<= copyStatics.dependsOn(compileStatics in frontend),

    mappings in(Compile, packageBin) ++= {
      copyStatics.value
      ((target in Compile).value / StaticFilesDir).***.get map { file =>
        file -> file.getAbsolutePath.stripPrefix((target in Compile).value.getAbsolutePath)
      }
    },

    watchSources ++= (sourceDirectory in frontend).value.***.get
  ).settings(commonSettings: _*)

lazy val frontend = project.in(file("frontend")).enablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJS)
  .settings(
    libraryDependencies ++= frontendDeps.value,
    crossLibs(Compile),
    jsDependencies ++= frontendJSDeps.value,
    persistLauncher in Compile := true,

    compile <<= (compile in Compile),
    compileStatics := {
      IO.copyDirectory(sourceDirectory.value / "main/assets/fonts", crossTarget.value / StaticFilesDir / WebContent / "assets/fonts")
      IO.copyDirectory(sourceDirectory.value / "main/assets/images", crossTarget.value / StaticFilesDir / WebContent / "assets/images")
      IO.copyDirectory(sourceDirectory.value / "main/assets/js", crossTarget.value / StaticFilesDir / WebContent / "assets/js")
      IO.copyDirectory(sourceDirectory.value / "main/assets/styles", crossTarget.value / StaticFilesDir / WebContent / "assets/styles")
      compileStaticsForRelease.value
      (crossTarget.value / StaticFilesDir).***.get
    },
    compileStatics <<= compileStatics.dependsOn(compile in Compile),

    artifactPath in(Compile, fastOptJS) :=
      (crossTarget in(Compile, fastOptJS)).value / StaticFilesDir / WebContent / "scripts" / "frontend-impl-fast.js",
    artifactPath in(Compile, fullOptJS) :=
      (crossTarget in(Compile, fullOptJS)).value / StaticFilesDir / WebContent / "scripts" / "frontend-impl.js",
    artifactPath in(Compile, packageJSDependencies) :=
      (crossTarget in(Compile, packageJSDependencies)).value / StaticFilesDir / WebContent / "scripts" / "frontend-deps-fast.js",
    artifactPath in(Compile, packageMinifiedJSDependencies) :=
      (crossTarget in(Compile, packageMinifiedJSDependencies)).value / StaticFilesDir / WebContent / "scripts" / "frontend-deps.js",
    artifactPath in(Compile, packageScalaJSLauncher) :=
      (crossTarget in(Compile, packageScalaJSLauncher)).value / StaticFilesDir / WebContent / "scripts" / "frontend-init.js"
  )