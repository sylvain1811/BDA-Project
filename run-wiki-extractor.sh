#!/bin/bash

python3 wikiextractor/WikiExtractor.py -o WikipediaTopicLabeling/data/wiki --process 12 --json WikipediaTopicLabeling/data/enwiki-latest-pages-articles1.xml-p10p30302.bz2

cd WikipediaTopicLabeling/data/wiki
for dir in *; do
    cat $dir/** >> ../wiki.json;
done
