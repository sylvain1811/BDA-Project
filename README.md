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
is done by this [tool](https://github.com/attardi/wikiextractor) written in python. As a result, a cleaned XML document is produced with a much
simpler structure:

```xml
<doc id="unique id for each article" url="wikipedia url" title="title of the article">
    this is the whole text of the article, without any weird brackets...
</doc>
<doc ...>
...
</doc>
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
    cat $dir/** >> wiki.xml;
done
```

Since the resulting xml contains tags only to delimit articles, the parsing with
Spark is much easier. The idea is to load the dataset into a `DataFrame`,
where each row is an article, with the following columns: **id**, **url**, **title**, **text**

Each **text** cell of the `DataFrame` can then be pre-processed with the
usual NLP pre-processing (stop words removal, apply a stemmer and a lemmatizer, ...)

As a result, the end of this part should return the following `DataFrame`:

| id  | url        | title            | text                         |
| --- | ---------- | ---------------- | ---------------------------- |
| 0   | http://... | Title of article | Preprocessed text of article |
| ... | ...        | ...              | ...                          |

## Analysis questions

The questions that this project is trying to answer can be formulated like this:

- How well can we regroup wikipedia articles according to their similarity into N defined categories ?
- Can we extract the most informative words from these groups in order to give them a label ?

To summarize, the analysis tries to categorize wikipedia, by working either with
the articles or with the words directly.

## Algorithms

### LSA

- TF-IDF
- SVD (singular value decomposition)

this should give a feature vector for each article.

### Clustering

For example, with K-Means:

1. Using the feature vector computed in the previous part
2. K = number of desired topics
3. Exploring the use of the Cosine distance to cluster the articles

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
