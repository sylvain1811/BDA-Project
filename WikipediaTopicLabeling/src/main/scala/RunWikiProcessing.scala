import org.apache.spark.ml.feature.{CountVectorizerModel, Word2VecModel}
import org.apache.spark.sql.{SparkSession,Row}
import org.apache.spark.sql.functions._

object RunWikiProcessing {


    def main(args: Array[String]) {

        //        println("load dataset...")
        //        // val dataset = spark.sqlContext.read.json("s3a://bda-wiki-bucket/wiki.json")
        //        // val dataset = spark.sqlContext.read.json("s3a://bda-wiki-bucket/wiki_min.json")
        //        //        val dataset = spark.sqlContext.read.json("data/wiki_small.json")


        val spark = SparkSession.builder
          .appName("Wikipedia Topic Labeling")
          .master("local[*]")
          .getOrCreate()

        spark.sparkContext.setLogLevel("ERROR")

        val dataset = spark.sqlContext
          .read.json("data/data.json")
          .filter(row => row(0) == null) // remove corrupted records
          .drop("_corrupt_record")

        val wikiProcessing = new WikiProcessing(spark, false)

        println("preprocess dataset...")
        val (preprocessingModel, preprocessedData) = wikiProcessing.preprocessing(dataset)

        println("train kmeans...")
        val (kMeansModel, kMeansData) = wikiProcessing.kMeans(preprocessedData)

        print("compute results...")
        wikiProcessing.showKMeansTopicByOcucrences(kMeansData)


//        val word2VecModel = preprocessingModel.stages(3).asInstanceOf[Word2VecModel]
//        wikiProcessing.showKMeansTopicLabeling(word2VecModel.getVectors, kMeansModel.clusterCenters, kMeansData)

//        print("LDA clustering...")
//        val vocabulary = preprocessingModel.stages(2).asInstanceOf[CountVectorizerModel].vocabulary
//        val ldaResult = wikiProcessing.lda(spark, preprocessedData, vocabulary)
//        println(ldaResult.filter("cluster == 4").show(false))

        spark.stop()
    }
}