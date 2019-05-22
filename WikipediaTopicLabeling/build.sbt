name := "Wikipedia Topic Project"

version := "1.0"

scalaVersion := "2.11.12"

val sparkVersion = "2.4.3"

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-sql" % sparkVersion,
    "org.apache.spark" %% "spark-mllib" % sparkVersion,

    "com.johnsnowlabs.nlp" %% "spark-nlp" % "2.0.4"
)
