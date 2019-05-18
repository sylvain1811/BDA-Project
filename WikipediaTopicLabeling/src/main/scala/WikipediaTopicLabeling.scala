import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
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

        idfModel.transform(featurizedData).drop("text", "url", "raw words", "raw features")
    }

    def cosineSimilarity(v1: List[Double], v2: List[Double]): Double = {
        val numerator = v1.zip(v2).foldRight(0.0)((elem, acc) => acc + elem._1 * elem._2)
        val denominator =
            math.sqrt(v1.foldRight(0.0)((elem, acc) => acc + elem * elem)) *
                math.sqrt(v2.foldRight(0.0)((elem, acc) => acc + elem * elem))

        numerator / denominator
    }

    def clustering(data: DataFrame): (KMeansModel, DataFrame) = {
        val kmeans = new KMeans().setK(10).setSeed(1L)
        val model = kmeans.fit(data).setPredictionCol("cluster")

        (model, model.transform(data))
    }

    def main(args: Array[String]) {
        val spark = SparkSession.builder
            .appName("Wikipedia Topic Labeling")
            .master("local")
            .getOrCreate()

        // val data = preprocessing(spark, "s3a://bda-project/wiki.json").cache()
        val data = preprocessing(spark, "data/wiki.json").cache()

        val (model, clusteredData) = clustering(data)

        // Shows the result.
        println("Cluster Centers: ")
        model.clusterCenters.foreach(println)

        println(clusteredData.orderBy("cluster").show)

        spark.stop()
    }
}
