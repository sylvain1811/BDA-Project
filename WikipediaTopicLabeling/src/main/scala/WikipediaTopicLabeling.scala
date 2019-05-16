import org.apache.spark.ml.feature.{HashingTF, IDF, RegexTokenizer, StopWordsRemover}
import org.apache.spark.sql.SparkSession

object WikipediaTopicLabeling {

  def main(args: Array[String]) {
    val spark = SparkSession.builder.appName("Wikipedia Topic Labeling").getOrCreate()

    // load json
    val articles = spark.sqlContext.read.json("data/wiki.json")

    // tokenizer
    val regexTokenizer = new RegexTokenizer()
      .setInputCol("text")
      .setOutputCol("raw words")
      .setPattern("\\W")

    val articlesTokenized = regexTokenizer.transform(articles)

    // stop words removal
    val remover = new StopWordsRemover()
      .setInputCol("raw words")
      .setOutputCol("words")

    val articlesCleaned = remover.transform(articlesTokenized)

    // TF-IDF
    val hashingTF = new HashingTF()
      .setInputCol("words")
      .setOutputCol("raw features")
      .setNumFeatures(20)

    val featurizedData = hashingTF.transform(articlesCleaned)

    val idf = new IDF().setInputCol("raw features").setOutputCol("features")
    val idfModel = idf.fit(featurizedData)

    val rescaledData = idfModel.transform(featurizedData)

    println(rescaledData.show)

    spark.stop()
  }
}
