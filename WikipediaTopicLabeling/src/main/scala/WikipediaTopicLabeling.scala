import org.apache.spark.ml.feature.{HashingTF, IDF, RegexTokenizer, StopWordsRemover}
import org.apache.spark.sql.{DataFrame, SparkSession}


object WikipediaTopicLabeling {

    def preprocessing(spark: SparkSession, path: String): DataFrame = {
        // load json
        val jsonData = spark.sqlContext.read.json(path)

        // tokenizer
        val regexTokenizer = new RegexTokenizer()
          .setInputCol("text")
          .setOutputCol("raw words")
          .setPattern("\\W")

        val tokenizedData = regexTokenizer.transform(jsonData)

        // stop words removal
        val remover = new StopWordsRemover()
          .setInputCol("raw words")
          .setOutputCol("words")

        val preprocessedData = remover.transform(tokenizedData)

        // TF-IDF
        val hashingTF = new HashingTF()
          .setInputCol("words")
          .setOutputCol("raw features")
          .setNumFeatures(20)

        val featurizedData = hashingTF.transform(preprocessedData)

        val idf = new IDF().setInputCol("raw features").setOutputCol("features")
        val idfModel = idf.fit(featurizedData)

        idfModel.transform(featurizedData)
    }

  def main(args: Array[String]) {
    val spark = SparkSession.builder.appName("Wikipedia Topic Labeling").getOrCreate()

    val data = preprocessing(spark, "data/wiki.json")

    println(data.select("raw features").show)

    spark.stop()
  }
}
