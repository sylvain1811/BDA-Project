# Big Data Analytics Project - Analyzing Wikipedia with Apache Spark

This project is developed as part of the course _Big Data Analytics_ of the
Master of Science in Engineering, Hes-SO Master.

The objective is to analyze Wikipedia in order to automatically give a topic
name for each article, by selecting more or less 5 words which describe best the
article.

## Dataset description

The dataset which is used for the analysis is available here:

[wikipedia dump](https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2)

It contains a very messy XML document, which obviously needs to be pre-processed
before extracting anything from it. Let's describe a little bit its structure.

The beginning of the document has various unneeded tags which seems to be more
or less metadata. Let's forget about this and scroll down a little. At some point,
a `<page>` tag should appear. Each of these tags represent a Wikipedia article
and contains the information we are looking for, as well as unwanted information
which need to be filtered out.

Among the sub-tag of the `<page>`, two tags which seem obviously useful are:

1. `<title>`
2. `<text>`

One can notice that the text is far from a clean text ready to be used. For example,
the subtitles of the page are represented like this:

== subtitle ==

Furthermore, there are a lot of brackets (`[]` and `{}`) which seem to represent the
multiple links between articles, a thing that wikipedia is well-known for.

## Features descriptions/extraction and preprocessing

Because Spark is not known for its XML parsing skills, a first preprocessing step
is done by this [tool](https://github.com/attardi/wikiextractor) written in python. As a result,
a JSON document is produced with a much simpler structure:

```json
{
  "id": "unique id for each article",
  "url": "wikipedia url",
  "title": "title of the article",
  "text": "text of the article"
}
```

The resulting script create a folder structure which is as follow:

```
wiki
    -- AA
        -- wiki_00
        -- wiki_01
        -- ...
        -- wiki_99
    -- AB
        -- wiki_00
        -- wiki_01
        -- ...
        -- wiki_99
    -- AC
        -- ...
    -- ...
```

This is not really a problem since these multiple documents can be merge together
with a simple bash command from the root directory (i.e. wiki/):

```bash
for dir in *; do
    cat $dir/** >> wiki.json;
done
```

Since the output is a json file, Spark can load it into a `DataFrame` without any troubles.
The idea is that each row is an article, with the following columns: **id**, **url**, **title**, **text**

Here is the schema of the loaded `DataFrame`:

| id  | url        | title            | text                         |
| --- | ---------- | ---------------- | ---------------------------- |
| 0   | http://... | Title of article | Preprocessed text of article |
| ... | ...        | ...              | ...                          |

Each **text** cell of the `DataFrame` can then be pre-processed with the
usual NLP pre-processing (stop words removal, apply a lemmatizer, ...).

### Preprocess the DataFrame with Spark ML

Two transformations are applied to the raw dataset before starting its analysis.
Each one is described below.

1. The text of each article is just a big `String`, which is not ideal for working
   with words. This problem can easily be solved by applying a _Tokenizer_ to each
   cell of the text column. This will split the String, leaving us an array of words
   to work with.
2. It's very usual to remove stop words from a corpus. Without surprises, this
   preprocessing undergo the same process.

### Extract features with TF-IDF

Even though the text is tokenized and freed from stop words, it is still not
possible to analyze it. One more step is needed to extract a small number of
features from these words. For this, a term frequency hashing combined to the
inverse document frequency is applied to the `DataFrame`. Each article is therefore
represented by a chosen number of features.

## Analysis questions

The questions that this project is trying to answer can be formulated like this:

- How well can we regroup wikipedia articles according to their similarity ?
- Can we extract the most informative words from these groups in order to give them a label ?

In other words, the idea is to apply topic modeling to the Wikipedia corpus,
thanks to various techniques and evaluate them.

## Algorithms

### LDA

[TODO]

### Clustering

For example, with K-Means:

1. Using a feature vector for each document
2. K = number of desired topics

The drawback of using K-Means is that there are no metrics to evaluate it, except
from checking by hand to make sure it is more or less accurate.

### Word2Vec

1. Get the whole corpus of the articles.
2. Generate a Word2Vec model with the whole corpus. Each word is therefore represented by a feature vector.
3. Get the corpus of one cluster of the previous part.
4. Apply Word2Vec to the cluster's corpus.
5. Take the mean feature vector of the cluster's corpus.
6. Look at the closest feature vectors of this mean feature vector. Their corresponding
   word should be a good insight of the cluster.

## Optimizations

## Tests and evaluations

## Results

## Future work

## Cloud cluster

To run this program in the cloud, follow this steps. Be aware that specified versions of libraries, spark and scala are important. Other versions have not been tested.

### Requirements

You need the following tools installed on your local machine.

- awscli (`pip install --user awscli`)
- Flintrock (`pip install --user flintrock`)

This tools need your AWS access key to work. So create a file at `~/.aws/credentials` and put the following inside:

```sh
[default]
aws_access_key_id=your_aws_access_key
aws_secret_access_key=associated_secret_access_key
```

### Upload dataset on S3

Use aws-cli to upload the dataset (wiki.json) into a S3 bucket.

```bash
aws s3 mb s3://bda-bucket
aws s3 cp /path/to/wiki.json s3://bda-bucket
```

### Configure Flintrock

Configure flintrock (with `flintrock configure`) like this:

```yml
services:
  spark:
    version: 2.2.1
    download-source: "https://archive.apache.org/dist/spark/spark-{v}/spark-{v}-bin-hadoop2.7.tgz"
  hdfs:
    version: 2.7.7
    download-source: https://www-eu.apache.org/dist/hadoop/common/hadoop-{v}/hadoop-{v}-src.tar.gz

provider: ec2

providers:
  ec2:
    key-name: aws-bda-project # cluster name
    identity-file: "/path/to/private_key.pem"
    instance-type: m3.medium # Choose EC2 flavor
    region: us-east-1
    ami: ami-0b8d0d6ac70e5750c # Amazon Linux 2, us-east-1
    user: ec2-user
    vpc-id: vpc-dc7caea6 # your VPC id
    subnet-id: subnet-42fe2b7c # one of your subnet id
    security-groups:
      - only-ssh-from-anywhere # security-group name that allow ssh from anywhere (0.0.0.0/0)
    instance-profile-name: Spark_S3 # IAM Role with AmazonS3FullAccess policy
    tenancy: default
    ebs-optimized: no
    instance-initiated-shutdown-behavior: terminate

launch:
  num-slaves: 1 # Choose number of slaves

debug: false
```

This will create the config file at `~/.config/flintrock/config.yaml`. You can reopen and edit it later if necessary.

### Launch and configure the cluster

Launch your cluster with

```bash
flintrock launch bda-wiki-cluster
```

Then, to access your dataset stored on S3 from Spark, you need to upload this libraries to the cluster.

- [hadoop-aws-2.7.7.jar](http://central.maven.org/maven2/org/apache/hadoop/hadoop-aws/2.7.7/hadoop-aws-2.7.7.jar)
- [aws-java-sdk-1.7.4.jar](http://central.maven.org/maven2/com/amazonaws/aws-java-sdk/1.7.4/aws-java-sdk-1.7.4.jar)

You can download theses files directly into the cluster with a remote `wget`. Thanks to `flintrock run-command`.

```bash
flintrock run-command bda-wiki-cluster "wget -o spark/jars/hadoop-aws-2.7.7.jar http://central.maven.org/maven2/org/apache/hadoop/hadoop-aws/2.7.7/hadoop-aws-2.7.7.jar"
flintrock run-command bda-wiki-cluster "wget -o spark/jars/aws-java-sdk-1.7.4.jar http://central.maven.org/maven2/com/amazonaws/aws-java-sdk/1.7.4/aws-java-sdk-1.7.4.jar"
```

Or you can download theses files on your local machine and upload them with `flintrock copy-file`.

```bash
flintrock copy-file bda-wiki-cluster /local/path/to/hadoop-aws-2.7.7.jar /home/ec2-user/spark/jars/
flintrock copy-file bda-wiki-cluster /local/path/to/aws-java-sdk-1.7.4.jar /home/ec2-user/spark/jars/
```

### Package the program and upload the archive

First, you need to package the program in a jar file that you will upload on the cluster. You can use sbt in command line or compile your project from your IDE.

```bash
cd /path/to/WikipediaTopicLabeling
sbt package
```

This will generate a jar file in `./target/scala-2.11/wikipedia-topic-project_2.11-1.0.jar`

Once you have this jar, you need to upload it to the cluster.

```bash
flintrock copy-file bda-wiki-cluster ./target/scala-2.11/wikipedia-topic-project_2.11-1.0.jar /home/ec2-user/
```

### Submit a job to the cluster

To submit a job, you need to know the DNS name of the master node of your cluster. You can get it with:

```bash
$ flintrock describe
Found 1 cluster in region us-east-1.
---
bda-wiki-cluster:
  state: running
  node-count: 2
  master: ec2-100-25-152-130.compute-1.amazonaws.com
  slaves:
    - ec2-54-152-129-154.compute-1.amazonaws.com
```

Here it is `ec2-100-25-152-130.compute-1.amazonaws.com`

Then, from your local machine, you can use spark-submit to launch a new job on your cluster. You must give the DNS name of the master node as a parameter. Don't forget the port number.

```bash
spark-submit \
 --class WikipediaTopicLabeling \
 --master spark://ec2-100-25-152-130.compute-1.amazonaws.com:7077 \
 --deploy-mode cluster \
 /home/ec2-user/wikipedia-topic-project_2.11-1.0.jar
```

Then you can see the running jobs and their outputs per slaves at <http://ec2-100-25-152-130.compute-1.amazonaws.com:8080>
