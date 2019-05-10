# Partie 0 : Preprocessing

1. Récupérer dataset et préprocesser avec script python ([repository github](https://spark-in.me/post/parsing-wikipedia-in-four-commands-for-nlp)). Il en ressort un fichier XML contenant : les attributs id, url, title et le text à l'intérieur de la balise.
2. Loader le fichier XML dans Scala/Spark en tant que DataFrame avec les colonnes **id, url, title, text** et chaque ligne est un document. [scala-xml](https://github.com/scala/scala-xml/wiki/Getting-started), [spark-xml](https://github.com/databricks/spark-xml)
3. Enlever [stopwords](https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.ml.feature.StopWordsRemover), [Stemmer et Lemmatizer](https://nlp.johnsnowlabs.com/docs/en/annotators#stemmer) pour avoir une nouvelle DataFrame avec les colonnes ...

A la fin de cette partie, on doit avoir une DataFrame comme ceci

| id  | url        | title            | text                         |
| --- | ---------- | ---------------- | ---------------------------- |
| 0   | http://... | Title of article | Preprocessed text of article |
| ... | ...        | ...              | ...                          |

# Partie 1 : LSA

1. Calcul de TF-IDF sur les textes des documents [spark tf-idf](https://spark.apache.org/docs/latest/mllib-feature-extraction.html#tf-idf)
2. SVD (Singular Value Decomposition) [spark svd](https://spark.apache.org/docs/latest/mllib-dimensionality-reduction.html) à partir de la matrice TF-IDF

A la fin de cette partie, on doit avoir pour chaque document un vecteur de caractéristiques le représentant.

# Partie 2 : Clustering

Par exemple avec KMeans.

1. Créer un modèle avec KMeans avec comme features le vecteur de la partie précédente
2. K correspond au nombre de topic différents que l'on veut
3. Utiliser la distance du cosinus pour calculer la distance entre les centroid peut être intéressant à tester

# Partie 3 : Word2Vec

1. Création d'un "super-document" contenant tous les mots de tous les documents (l'entièreté du corpus)
2. Appliquer Word2Vec sur ce super-document. On a alors un vecteur pour chaque mot du super-document (donc un vecteur pour chaque mots de tout le dataset). Cela génère un modèle.
3. Pour chacun des mots d'un cluster de la partie 2, on prédit leur vecteur Word2Vec grâce au modèle précédemment créé. Cela génère un sub-super-document par cluster.
4. Ensuite, on calcul le vecteur moyen du sub-super-document
5. Pour chaque sub-super-document, on cherche des mots-clés en calculant les mots les plus proche du centre. Cela donne des mots-clés pour le topic du cluster.

# Partie 4 : Justifier l'échec
