import com.johnsnowlabs.nlp.annotator._
import com.johnsnowlabs.nlp.base._
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.clustering.{KMeans, KMeansModel, LDA, LocalLDAModel}
import org.apache.spark.ml.feature.{CountVectorizer, CountVectorizerModel, RegexTokenizer, StopWordsRemover}
import org.apache.spark.ml.linalg.{DenseVector, SparseVector}
import org.apache.spark.ml.linalg.{Vector => MLVector}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD

object WikipediaTopicLabeling {

    def preprocessing(spark: SparkSession, dataset: DataFrame): PipelineModel = { //(RDD[(Long, Vector)], Array[String], Long) = {


        val tokenizer = new RegexTokenizer()
          .setInputCol("text")
          .setOutputCol("token")
          .setPattern("\\W")

        val stopWordRemover = new StopWordsRemover()
          .setInputCol("token")
          .setOutputCol("stop_words")

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


    def cosineSimilarity(v1: List[Double], v2: List[Double]): Double = {
        val numerator = (v1.zip(v2)).foldRight(0.0)((elem, acc) => acc + elem._1 * elem._2)
        val denominator =
            math.sqrt(v1.foldRight(0.0)((elem, acc) => acc + elem * elem)) *
              math.sqrt(v2.foldRight(0.0)((elem, acc) => acc + elem * elem))
        numerator / denominator
    }


    def main(args: Array[String]) {
        val spark = SparkSession.builder
          .appName("Wikipedia Topic Labeling")
          .master("local[*]")
          .getOrCreate()

        spark.sparkContext.setLogLevel("ERROR")

        import spark.implicits._

        val train = true

        val dataset = spark.sqlContext.read.json("data/wiki.json")

        val preprocessingModel = if (train) {
            preprocessing(spark, dataset)
        } else {
            PipelineModel.load("models/preprocessing")
        }

        val preprocessedData = preprocessingModel.transform(dataset)
        val vocabArray = preprocessingModel.stages(2).asInstanceOf[CountVectorizerModel].vocabulary

        println(preprocessedData.show)
        println(vocabArray.length)

        val ldaModel = if (train) {
            val lda = new LDA().setK(10).setMaxIter(10)
            val ldaModel = lda.fit(preprocessedData)
            ldaModel.write.overwrite.save("model/lda")
            ldaModel
        } else {
            LocalLDAModel.load("models/lda")
        }

        val topicIndices = ldaModel.describeTopics(3)


        // src: https://github.com/karenyyy/Coursera_and_Udemy/blob/2e93a401868ff25900ff83550c08c16588ac2267/Big_Data_coursera/Exercises/notebooks/ScalaMachineLearningProjects_Code/Chapter05/TopicModelling/topicModellingwithLDA.scala
        val topics = topicIndices.map {
            case Row(topic: Int, termIndices: Seq[Int], termWeights: Seq[Double]) => {
                termIndices.zip(termWeights).map {
                    case (termIndice, termWeight) => (vocabArray(termIndice.toInt), termWeight)
                }
            }
        }.collect()


        print("3 topics:")
        topics.zipWithIndex.foreach {
            case (topic, i) =>
                println(s"topic $i")
                println("------------------------")
                topic.foreach {
                    case (term, weight) =>
                        println(s"$term \t $weight")
                }
                println("------------------------")
                println()
        }

        println(ldaModel.transform(preprocessedData).show(false))

        spark.stop()
    }
}
