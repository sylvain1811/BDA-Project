name := "Wikipedia Topic Project"

version := "1.0"

scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-sql" % "2.2.1",
    "org.apache.spark" %% "spark-mllib" % "2.2.1"
)
