import sbt._
import complete.DefaultParsers._
import Keys._
import com.typesafe.sbt._
import pgp.PgpKeys._
import SbtSite._
import SiteKeys._
import SbtGit._
import GitKeys._
import SbtGhPages._
import GhPagesKeys._
import sbtrelease._
import ReleasePlugin._
import ReleaseKeys._
import ReleaseStateTransformations._
import Utilities._
import Defaults._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

object build extends Build {
  type Settings = Def.Setting[_]

  /** MAIN PROJECT */
  lazy val specs2 = Project(
    id = "specs2",
    base = file("."),
    settings = 
      moduleSettings           ++
      siteSettings             ++
      releaseSettings          ++
      rootSettings             ++
      compatibilitySettings    ++
      Seq(name := "specs2")
  ).aggregate(common, matcher, matcherExtra, core, html, analysis, form, markdown, gwt, junit, scalacheck, mock)
  
  /** COMMON SETTINGS */
  lazy val specs2Settings: Seq[Settings] = Seq(
    organization := "org.specs2",
    specs2Version in GlobalScope <<= version,
    specs2ShellPrompt,
    scalazVersion := "7.0.5",
    scalaVersion := "2.10.3")

  lazy val specs2Version = settingKey[String]("defines the current specs2 version")
  lazy val scalazVersion = settingKey[String]("defines the current scalaz version")
  lazy val paradisePlugin = compilerPlugin("org.scalamacros" %% "paradise" % "2.0.0-M1" cross CrossVersion.full)

  lazy val aggregateCompile = ScopeFilter(
             inProjects(common, matcher, matcherExtra, core, html, analysis, form, markdown, gwt, junit, scalacheck, mock),
             inConfigurations(Compile))

  lazy val aggregateTest = ScopeFilter(
             inProjects(common, matcher, matcherExtra, core, html, analysis, form, markdown, gwt, junit, scalacheck, mock, guide, examples),
             inConfigurations(Test))

  lazy val resolversSettings = resolvers ++= 
    Seq(Resolver.sonatypeRepo("releases"), 
        Resolver.sonatypeRepo("snapshots"),
        Resolver.typesafeIvyRepo("releases"))

  lazy val moduleSettings: Seq[Settings] = 
      defaultSettings      ++
      specs2Settings       ++
      resolversSettings    ++
      compilationSettings  ++
      testingSettings      ++
      publicationSettings

  lazy val rootSettings: Seq[Settings] = Seq(
      sources in Compile                      := sources.all(aggregateCompile).value.flatten,
      unmanagedResourceDirectories in Compile := unmanagedResourceDirectories.all(aggregateCompile).value.flatten,
      sources in Test                         := sources.all(aggregateTest).value.flatten,
      unmanagedResourceDirectories in Test    := unmanagedResourceDirectories.all(aggregateTest).value.flatten,
      libraryDependencies                     := libraryDependencies.all(aggregateTest).value.flatten.map(maybeMarkProvided)
    )

  /** MODULES (sorted in alphabetical order) */
  lazy val analysis = Project(id = "specs2-analysis", base = file("analysis"),
    settings = Seq(
      libraryDependencies ++= Seq(
        ("org.scala-lang" % "scala-compiler" % scalaVersion.value)
          .exclude("org.scala-lang.modules", "scala-parser-combinators_2.11.0-M6")
          .exclude("org.scala-lang.modules", "scala-xml_2.11.0-M6")
        ,
        "org.specs2"     % "classycle"      % "1.4.1")) ++
    moduleSettings
  ).dependsOn(common % "test->test", core, matcher, scalacheck % "test")

  lazy val common = Project(id = "specs2-common", base = file("common"),
    settings = moduleSettings ++ 
      Seq(libraryDependencies ++= Seq(
            "org.scalaz"     %% "scalaz-core"       % scalazVersion.value,
            "org.scalaz"     %% "scalaz-concurrent" % scalazVersion.value,
            "org.scala-lang" %  "scala-reflect"     % scalaVersion.value,
            scalacheckLib % "test"))      
  )

  lazy val core = Project(id = "specs2-core", base = file("core"),
    settings = Seq(
      libraryDependencies ++= Seq(
        "org.scala-sbt"  % "test-interface" % "1.0" % "optional",
        mockitoLib % "test",
        junitLib   % "test")) ++
      moduleSettings
  ).dependsOn(matcher, common % "test->test")

  lazy val examples = Project(id = "specs2-examples", base = file("examples"),
    settings = moduleSettings
  ).dependsOn(common, matcher, matcherExtra, core, analysis, form, html, markdown, gwt, junit, scalacheck, mock)

  lazy val form = Project(id = "specs2-form", base = file("form"),
    settings = moduleSettings
  ).dependsOn(core, markdown, matcherExtra, scalacheck % "test->test")

  lazy val guide = Project(id = "specs2-guide", base = file("guide"),
    settings = moduleSettings
  ).dependsOn(examples % "compile->compile;test->test")

  lazy val gwt = Project(id = "specs2-gwt", base = file("gwt"),
    settings = Seq(
      libraryDependencies += "com.chuusai" %% "shapeless" % "2.0.0-M1" cross CrossVersion.full
    ) ++ moduleSettings
  ).dependsOn(core, matcherExtra, scalacheck)

  lazy val html = Project(id = "specs2-html", base = file("html"),
    settings = moduleSettings
  ).dependsOn(form, mock % "test", matcherExtra % "test")

  lazy val junit = Project(id = "specs2-junit", base = file("junit"),
    settings = Seq(libraryDependencies ++= Seq(junitLib)) ++
      moduleSettings
  ).dependsOn(core, matcherExtra % "test", mock % "test")

  lazy val markdown = Project(id = "specs2-markdown", base = file("markdown"),
    settings = Seq(
     libraryDependencies ++= Seq(
        "org.pegdown"  % "pegdown" % "1.2.1")) ++
      moduleSettings
  ).dependsOn(common, core % "compile->test")

  lazy val matcher = Project(id = "specs2-matcher", base = file("matcher"),
    settings = moduleSettings
  ).dependsOn(common)

  lazy val matcherExtra = Project(id = "specs2-matcher-extra", base = file("matcher-extra"),
    settings = moduleSettings ++ Seq(
      libraryDependencies ++= (if (scalaVersion.value.startsWith("2.11")) Nil else List(paradisePlugin))
    )
  ).dependsOn(analysis, scalacheck, matcher, core % "test->test")

  lazy val mock = Project(id = "specs2-mock", base = file("mock"),
    settings = Seq(
     libraryDependencies ++= Seq(
      hamcrestLib,
      mockitoLib)) ++
      moduleSettings
  ).dependsOn(core)

  lazy val scalacheck = Project(id = "specs2-scalacheck", base = file("scalacheck"),
    settings = Seq(
     libraryDependencies ++= Seq(scalacheckLib)) ++
      moduleSettings
  ).dependsOn(core)

  lazy val tests = Project(id = "specs2-tests", base = file("tests"),
    settings = moduleSettings
  ).dependsOn(core % "compile->compile;test->test", matcherExtra, junit % "test->test", examples % "test->test")

  /**
   * Main libraries 
   */
  lazy val scalacheckLib = "org.scalacheck" %% "scalacheck"   % "1.11.2"
  lazy val mockitoLib    = "org.mockito"    % "mockito-core"  % "1.9.5"
  lazy val junitLib      = "junit"          % "junit"         % "4.11"
  lazy val hamcrestLib   = "org.hamcrest"   % "hamcrest-core" % "1.3"

  lazy val specs2ShellPrompt = shellPrompt in ThisBuild := { state => 
    val name = Project.extract(state).currentRef.project
    (if (name == "specs2") "" else name) + "> " 
  }

  lazy val compilationSettings: Seq[Settings] = Seq(
    javacOptions ++= Seq("-Xmx3G", "-Xms512m", "-Xss4m"),
    maxErrors := 20,
    scalacOptions in GlobalScope ++= Seq("-Xcheckinit", "-Xlint", "-deprecation", "-unchecked", "-feature", "-language:_"),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

  lazy val testingSettings: Seq[Settings] = Seq(
    initialCommands in console in test := "import org.specs2._",
    logBuffered := false,
    cancelable := true,
    javaOptions += "-Xmx3G",
    fork in test := true,
    testOptions := Seq(Tests.Filter(s => Seq("Spec", "Guide", "Index").exists(s.endsWith) && Seq("Specification", "FeaturesSpec").forall(n => !s.endsWith(n))))
  )

  /**
   * RELEASE PROCESS
   */
  lazy val releaseSettings: Seq[Settings] =
    ReleasePlugin.releaseSettings ++ Seq(
    tagName <<= (version in ThisBuild) map (v => "SPECS2-" + v),
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      setReleaseVersion,
      commitReleaseVersion,
      generateUserGuide,
      generateIndexPage,
      publishSite,
      publishSignedArtifacts,
      notifyHerald,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges
    ),
    releaseSnapshotProcess := Seq[ReleaseStep](
      generateUserGuide,
      publishSite,
      publishSignedArtifacts
      ),
    commands += releaseSnapshotCommand
    ) ++
  Seq(publishUserGuideTask <<= pushSite.dependsOn(makeSite).dependsOn(generateUserGuideTask)) ++
  documentationSettings

  lazy val releaseSnapshotProcess = SettingKey[Seq[ReleaseStep]]("release-snapshot-process")
  private lazy val releaseSnapshotCommandKey = "release-snapshot"
  private val WithDefaults = "with-defaults"
  private val SkipTests = "skip-tests"
  private val releaseSnapshotParser = (Space ~> WithDefaults | Space ~> SkipTests).*

  val releaseSnapshotCommand: Command = Command(releaseSnapshotCommandKey)(_ => releaseSnapshotParser) { (st, args) =>
    val extracted = Project.extract(st)
    val releaseParts = extracted.get(releaseSnapshotProcess)

    val startState = st
      .put(useDefaults, args.contains(WithDefaults))
      .put(skipTests, args.contains(SkipTests))

    val initialChecks = releaseParts.map(_.check)
    val process = releaseParts.map(_.action)

    initialChecks.foreach(_(startState))
    Function.chain(process)(startState)
  }

  /**
   * DOCUMENTATION
   */
  lazy val siteSettings: Seq[Settings] = ghpages.settings ++ SbtSite.site.settings ++ Seq(
    siteSourceDirectory <<= target (_ / "specs2-reports"),
    // depending on the version, copy the api files to a different directory
    siteMappings <++= (mappings in packageDoc in Compile, version) map { (m, v) =>
      for((f, d) <- m) yield (f, if (v.trim.endsWith("SNAPSHOT")) ("api/master/" + d) else ("api/SPECS2-"+v+"/"+d))
    },
    // override the synchLocal task to avoid removing the existing files
    synchLocal <<= (privateMappings, updatedRepository, gitRunner, streams) map { (mappings, repo, git, s) =>
      val betterMappings = mappings map { case (file, target) => (file, repo / target) }
      IO.copy(betterMappings)
      repo
    },
    gitRemoteRepo := "git@github.com:etorreborre/specs2.git"
  )

  lazy val documentationSettings =
    testTaskDefinition(generateUserGuideTask, Seq(Tests.Filter(_.endsWith("UserGuide")), Tests.Argument("html"))) ++
    testTaskDefinition(generateIndexPageTask, Seq(Tests.Filter(_.endsWith("Index")), Tests.Argument("html")))

  lazy val generateUserGuideTask = TaskKey[Tests.Output]("generate-user-guide", "generate the user guide")
  lazy val generateUserGuide     = ReleaseStep { st: State =>
    val st2 = executeStepTask(generateUserGuideTask, "Generating the User Guide", Test)(st)
    commitCurrent("updated the UserGuide")(st2)
  }

  lazy val generateIndexPageTask = TaskKey[Tests.Output]("generate-index-page", "generate the index page")
  lazy val generateIndexPage     = executeStepTask(generateIndexPageTask, "Generating the Index page", Test)

  lazy val publishUserGuideTask = TaskKey[Unit]("publish-user-guide", "publish the user guide")

  lazy val publishSite = ReleaseStep { st: State =>
    val st2 = executeStepTask(makeSite, "Making the site")(st)
    executeStepTask(pushSite, "Publishing the site")(st2)
  }

  def testTaskDefinition(task: TaskKey[Tests.Output], options: Seq[TestOption]) =
    Seq(testTask(task))                          ++
    inScope(GlobalScope)(defaultTestTasks(task)) ++
    inConfig(Test)(testTaskOptions(task))        ++
    (testOptions in (Test, task) ++= options)

  def testTask(task: TaskKey[Tests.Output]) =
    task <<= (streams in Test, loadedTestFrameworks in Test, testLoader in Test,
      testGrouping in Test in test, testExecution in Test in task,
      fullClasspath in Test in test, javaHome in test) flatMap Defaults.allTestGroupsTask

  /**
   * PUBLICATION
   */
  lazy val publishSignedArtifacts = executeAggregateTask(publishSigned, "Publishing signed artifacts")

  lazy val publicationSettings: Seq[Settings] = Seq(
    publishTo in Global <<= version { v: String =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else                             Some("staging" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },
    pomExtra := (
      <url>http://specs2.org/</url>
        <licenses>
          <license>
            <name>MIT-style</name>
            <url>http://www.opensource.org/licenses/mit-license.php</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>http://github.com/etorreborre/specs2</url>
          <connection>scm:http:http://etorreborre@github.com/etorreborre/specs2.git</connection>
        </scm>
        <developers>
          <developer>
            <id>etorreborre</id>
            <name>Eric Torreborre</name>
            <url>http://etorreborre.blogspot.com/</url>
          </developer>
        </developers>
    ),
    credentials := Seq(Credentials(Path.userHome / ".sbt" / "specs2.credentials"))
  )

  /**
   * NOTIFICATION
   */
  lazy val notifyHerald = ReleaseStep (
    action = (st: State) => {
      Process("herald &").lines; st.log.info("Starting herald to publish the release notes")
      commitCurrent("Updated the release notes")(st)
    },
    check  = (st: State) => {
      st.log.info("Checking if herald is installed")
      if ("which herald".!<(st.log) != 0) sys.error("You must install 'herald': http://github.com/n8han/herald on your machine")
      st
    }
  )

  /**
   * COMPATIBILITY
   */
  lazy val compatibilitySettings = mimaDefaultSettings ++
    Seq(previousArtifact := Some("org.specs2" % "specs2_2.10" % "2.3.3"))

  /**
   * UTILITIES
   */

  /** Mark some dependencies of the full artifact as provided */
  def maybeMarkProvided(dep: ModuleID): ModuleID =
    if (providedDependenciesInAggregate.exists(dep.name.startsWith)) dep.copy(configurations = Some("provided"))
    else dep

  /* A list of dependency module names that should be marked as "provided" for the aggregate artifact */
  lazy val providedDependenciesInAggregate = Seq("shapeless")

  private def executeStepTask(task: TaskKey[_], info: String) = ReleaseStep { st: State =>
    executeTask(task, info)(st)
  }

  private def executeAggregateTask(task: TaskKey[_], info: String) = (st: State) => {
    st.log.info(info)
    val extracted = Project.extract(st)
    val ref: ProjectRef = extracted.get(thisProjectRef)
    extracted.runAggregated(task in ref, st)
  }

  private def executeTask(task: TaskKey[_], info: String) = (st: State) => {
    st.log.info(info)
    val extracted = Project.extract(st)
    val ref: ProjectRef = extracted.get(thisProjectRef)
    extracted.runTask(task in ref, st)._1
  }

  private def executeStepTask(task: TaskKey[_], info: String, configuration: Configuration) = ReleaseStep { st: State =>
    executeTask(task, info, configuration)(st)
  }

  private def executeTask(task: TaskKey[_], info: String, configuration: Configuration) = (st: State) => {
    st.log.info(info)
    val extracted = Project.extract(st)
    val ref: ProjectRef = extracted.get(thisProjectRef)
    extracted.runTask(task in configuration in ref, st)._1
  }

  private def commitCurrent(commitMessage: String): State => State = { st: State =>
    vcs(st).add(".") !! st.log
    val status = (vcs(st).status !!) trim

    if (status.nonEmpty) {
      vcs(st).commit(commitMessage) ! st.log
      st
    } else st
  }

  private def pushCurrent: State => State = { st: State =>
    vcs(st).pushChanges !! st.log
    st
  }

  private def vcs(st: State): Vcs = {
    st.extract.get(versionControlSystem).getOrElse(sys.error("Aborting release. Working directory is not a repository of a recognized VCS."))
  }

}


