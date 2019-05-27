import os.path
import re
import subprocess


def clean_string(string):
    return re.sub(r'([^\s\w]|_)+', '', string).replace("\t", " ")

def process():
    with open('enwiki-latest-abstract1.xml', 'r') as f:
        with open('data.json', 'w') as json:
            for line in f:
                if line[0:7] == '<title>':
                    title = clean_string(line[18:-9])
                    print('{"title": "', title, '", ', sep='', end='', file=json)
                elif line[0:10] == '<abstract>':
                    text = clean_string(line[10:-12])
                    print('"text": "', text, '"}', file=json)
    
def download():
    print('downloading...')
    subprocess.run(["wget", "https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-abstract1.xml.gz"])
    print('extracting...')
    subprocess.run(["gzip", "-d", "enwiki-latest-abstract1.xml.gz"])

if __name__ == '__main__':
    if not os.path.isfile('enwiki-latest-abstract1.xml'):
        download()
    print('preprocessing...')
    process()

