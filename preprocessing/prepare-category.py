import os.path
import re
import subprocess


def clean_string(string):
    return re.sub(r'([^\s\w]|_)+', '', string).replace("\t", " ")

def process():
    with open('enwiki-latest-pages-articles1.xml-p10p30302', 'r') as f:
        with open('categories.json', 'w') as json:
            first = True
            for line in f:
                line = line.lstrip(' ')

                if re.search("<title>", line):
                    if not first:
                        # remove last ',' of the categories list 
                        json.seek(json.tell() - 1, os.SEEK_SET)
                        json.write('')
                        # close the line and start a new one
                        json.write('}\n')
                        json.flush()
                    else:
                        first = False
                    title = clean_string(line[7:-9])
                    json.write('{"title": "')
                    json.write(title)
                    json.write('", "categories":')
                if re.search("\[\[Category:", line):
                    category = clean_string(line[10:-1])
                    json.write(' "')
                    json.write(category)
                    json.write('",')

            json.seek(json.tell() - 1, os.SEEK_SET)
            json.write('')
            json.write('}')
            json.flush()
    
def download():
    print('downloading...')
    subprocess.run(["wget", "https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles1.xml-p10p30302.bz2"])
    print('extracting...')
    subprocess.run(["bzip2", "-d",
        "enwiki-latest-pages-articles1.xml-p10p30302.bz2"])

if __name__ == '__main__':
    if not os.path.isfile('enwiki-latest-pages-articles1.xml-p10p30302'):
        download()
    print('preprocessing...')
    process()

