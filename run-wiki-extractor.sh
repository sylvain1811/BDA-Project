#!/bin/sh

python3 wikiextractor/WikiExtractor.py -o data/wiki --json --process 8 WikipediaTopicLabeling/data/enwiki-latest-pages-articles1.xml-p10p30302
