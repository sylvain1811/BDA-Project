#!/bin/bash

CLUSTER_NAME=$1

flintrock launch $CLUSTER_NAME && \
    flintrock run-command bda-wiki-cluster "wget -o spark/jars/hadoop-aws-2.7.7.jar http://central.maven.org/maven2/org/apache/hadoop/hadoop-aws/2.7.7/hadoop-aws-2.7.7.jar" && \
    flintrock run-command bda-wiki-cluster "wget -o spark/jars/aws-java-sdk-1.7.4.jar http://central.maven.org/maven2/com/amazonaws/aws-java-sdk/1.7.4/aws-java-sdk-1.7.4.jar" && \
    flintrock run-command bda-wiki-cluster "mkdir data && wget -o data/wiki.json https://s3.amazonaws.com/bda-project/wiki.json" && \
    flintrock describe && \
    echo "Cluster started"

# ./../spark-2.2.1-bin-hadoop2.7/bin/spark-submit \
#  --class WikipediaTopicLabeling \
#  --master spark://ec2-100-26-138-11.compute-1.amazonaws.com:7077 \
#  --deploy-mode cluster \
#  --conf "spark.jars.packages=com.amazonaws:aws-java-sdk:1.7.4,org.apache.hadoop:hadoop-aws:2.7.7" \
#  /home/ec2-user/wikipedia-topic-project_2.11-1.0.jar
