#!/bin/sh

sbt package
spark-submit --class "WikipediaTopicLabeling" --master local[8] target/scala-2.11/wikipedia-topic-project_2.11-1.0.jar
