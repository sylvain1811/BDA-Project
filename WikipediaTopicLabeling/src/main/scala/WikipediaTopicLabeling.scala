import org.apache.spark.ml.clustering.{LDA, LDAModel, LocalLDAModel}
import org.apache.spark.ml.feature.{CountVectorizer, CountVectorizerModel, RegexTokenizer, StopWordsRemover}
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

object WikipediaTopicLabeling {

    def preprocessing(spark: SparkSession, dataset: DataFrame): PipelineModel = { //(RDD[(Long, Vector)], Array[String], Long) = {

        val tokenizer = new RegexTokenizer()
          .setInputCol("text")
          .setOutputCol("token")
          .setPattern("\\W")

        val stopwords = spark.read.text("data/stopwords.txt").collect.map(row => row.getString(0))

        val stopWordRemover = new StopWordsRemover()
          .setInputCol("token")
          .setOutputCol("stop_words")
          .setStopWords(stopwords)

        val countVectorizer = new CountVectorizer()
          .setInputCol("stop_words")
          .setOutputCol("features")

        val pipeline = new Pipeline()
          .setStages(Array(
              tokenizer,
              stopWordRemover,
              countVectorizer
          ))

        val preprocessingModel = pipeline.fit(dataset)
        preprocessingModel.write.overwrite.save("models/preprocessing")
        preprocessingModel
    }


    def getLDATopics(spark: SparkSession, ldaModel: LDAModel, vocabulary: Array[String], print: Boolean = false): Array[Seq[(String, Double)]] = {
        // src: https://github.com/karenyyy/Coursera_and_Udemy/blob/2e93a401868ff25900ff83550c08c16588ac2267/ \
        //      Big_Data_coursera/Exercises/notebooks/ScalaMachineLearningProjects_Code/Chapter05/TopicModelling/ \
        //      topicModellingwithLDA.scala

        import spark.implicits._

        //  topics:
        // +------+--------+
        // | term | weight |
        // +------+--------+
        // | [..] |  [..]  |
        // +------+--------+
        val topicIndices = ldaModel.describeTopics(5)
        val topics = topicIndices.map {
            case Row(topic: Int, termIndices: Seq[Int], termWeights: Seq[Double]) => {
                termIndices.zip(termWeights).map {
                    case (termIndice: Int, termWeight: Double) => (vocabulary(termIndice.toInt), termWeight)
                }
            }
        }.collect

        if (print)
            topics.zipWithIndex.foreach {
                case (topic, i) =>  println(s"topic $i: ${topic.toString()}")
            }
            println

        topics
    }


    def cosineSimilarity(v1: List[Double], v2: List[Double]): Double = {
        val numerator = (v1.zip(v2)).foldRight(0.0)((elem, acc) => acc + elem._1 * elem._2)
        val denominator =
            math.sqrt(v1.foldRight(0.0)((elem, acc) => acc + elem * elem)) *
              math.sqrt(v2.foldRight(0.0)((elem, acc) => acc + elem * elem))
        numerator / denominator
    }


    def lda(spark: SparkSession, dataset: DataFrame, vocabulary: Array[String], train: Boolean): DataFrame = {
        import spark.implicits._

        val ldaModel = if (train) {
            val lda = new LDA().setK(2).setMaxIter(50)
            val ldaModel = lda.fit(dataset)
            ldaModel.write.overwrite.save("model/lda")
            ldaModel
        } else {
            LocalLDAModel.load("models/lda")
        }

        println
        println(s"preplexity: ${ldaModel.logPerplexity(dataset)}")
        println

        val topics = getLDATopics(spark, ldaModel, vocabulary, print=true)

        val ldaDataset = ldaModel.transform(dataset).select("id", "title", "topicDistribution")

        ldaDataset.map {
            case Row(id: String, title: String, topicDistribution: Vector) =>
                (title, topicDistribution, topics(topicDistribution.argmax).map(_._1), topicDistribution.argmax)
        }
          .withColumnRenamed("_1", "title")
          .withColumnRenamed("_2", "topicDistribution")
          .withColumnRenamed("_3", "topicWords")
          .withColumnRenamed("_4", "cluster")
    }


    def main(args: Array[String]) {

        val spark = SparkSession.builder
          .appName("Wikipedia Topic Labeling")
          .master("local[*]")
          .getOrCreate()

        spark.sparkContext.setLogLevel("ERROR")

        import spark.implicits._

        val train = true

        println("load dataset...")
        // val dataset = spark.sqlContext.read.json("s3a://bda-wiki-bucket/wiki.json")
        // val dataset = spark.sqlContext.read.json("s3a://bda-wiki-bucket/wiki_min.json")
        val dataset = spark.sqlContext.read.json("data/wiki_small.json")

        println("preprocess dataset...")
        val preprocessingModel = if (train) {
            preprocessing(spark, dataset)
        } else {
            PipelineModel.load("models/preprocessing")
        }

        val preprocessedData = preprocessingModel.transform(dataset)
        val vocabulary = preprocessingModel.stages(2).asInstanceOf[CountVectorizerModel].vocabulary

        println("LDA clustering...")
        val ldaResult = lda(spark, preprocessedData, vocabulary, train)
        println(ldaResult.show(false))

        spark.stop()
    }
}
