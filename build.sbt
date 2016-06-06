name := "bibleToPPT"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
	"org.apache.spark"			  %%  "spark-core"				% "1.2.0",
    "org.apache.poi" % "poi-ooxml" % "3.14"
)