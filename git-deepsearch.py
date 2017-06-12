import sys
import os
import subprocess
import glob
import shutil

def checkId(id, searchTerm):
    result = subprocess.run(["git", "cat-file", "-p", id], stdout=subprocess.PIPE)
    content = result.stdout.decode("utf-8")
    if searchTerm in content:
        print("HIT!!! " + content)
    
def unpack():
    if os.path.exists(".git/objects/pack") and os.listdir(".git/objects/pack")!=[]:
        if not os.path.exists("TMP_PACK"):
            os.makedirs("TMP_PACK")
        for file in glob.glob(".git/objects/pack/*.pack"):
            shutil.move(file, "TMP_PACK/")
            subprocess.run(["/bin/sh", "-c", "git unpack-objects < TMP_PACK/"+os.path.basename(file)], stdout=subprocess.PIPE)

print(sys.argv)
path = sys.argv[1]
searchTerm = sys.argv[2]

os.chdir(path)
unpack()

for root, dirs, files in os.walk(".git/objects"):
    path = root.split(os.sep)
    for file in files:
        id = os.path.basename(root) + file;
        checkId(id, searchTerm)