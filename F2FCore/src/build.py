#! /usr/bin/env python
# Automation script for the build process
# Author ulno
#
# build.py [target]
# - build to build and build.py 
# - release to create a relase zip-archive
# - srcrelease to create a spource release
# - clean to clean


import os, stat, glob
from os.path import join
import sys
import shutil
import re

import killableprocess

sconspath = "scons"
zippath = "zip"
builddir = join("..","build")
pythonShell = sys.executable

def build():
    # make sure the build directory exists
    try: os.mkdir(builddir)
    except OSError: pass
    os.chdir(builddir)
    killableprocess.call([sconspath,"-f", join("..","src","sconsbuild.py"),"debug=1"])

def release():
    build() # changes path!!
    # copy everything together
    releasepath = "F2FComputing"
    try: os.mkdir(releasepath)
    except OSError: pass
    try: os.mkdir(join(releasepath,"f2f"))
    except OSError: pass
    try: os.mkdir(join(releasepath,"jabberpy"))
    except OSError: pass
    srcpath = join("..", "src")
    headlesssrcpath = join("..","..","F2FHeadless","src")
    copylist = [
                (glob.glob("_f2fcore.*")[0], releasepath),
                (join("libf2fcore", "f2fcore.py"), releasepath),
                (join(headlesssrcpath, "f2f", "__init__.py"), join(releasepath,"f2f")),
                (join(headlesssrcpath, "f2f", "adapter.py"), join(releasepath,"f2f")),
                (join(headlesssrcpath, "jabberpy", "__init__.py"), join(releasepath,"jabberpy")),
                (join(headlesssrcpath, "jabberpy", "debug.py"), join(releasepath,"jabberpy")),
                (join(headlesssrcpath, "jabberpy", "jabber.py"), join(releasepath,"jabberpy")),
                (join(headlesssrcpath, "jabberpy", "xmlstream.py"), join(releasepath,"jabberpy")),
                (join(headlesssrcpath, "headlessclient.py"), releasepath),
                (join(headlesssrcpath, "tests", "montecarlopi.py"), releasepath),
                (join(headlesssrcpath, "tests", "p2psimple.py"), releasepath),
                ]
    for (from_,to) in copylist:
        print "Copying %s to %s."%(from_, to)
        shutil.copy(from_, to)
    print "Packing."
    killableprocess.call([zippath, "-r", "F2FComputing.zip", "F2FComputing"])

def srcrelease():
    # pack all py, i, h, and c files
    clean()
    os.chdir("..")
    srcbuildpath = join("build","F2FComputingSrc") 
    dirlist = [(x,z) for (x,y,z) in os.walk(".")]
    os.mkdir(srcbuildpath)
    for (dirpath,filenames) in dirlist:
        if not re.search(r"\.svn\b",dirpath):
            if(dirpath=="."):
                destpath = join(srcbuildpath,"F2FCore")
            else:
                destpath = join(srcbuildpath,"F2FCore",dirpath)
            print "Creating directory: %s"%destpath
            os.mkdir(destpath)
            for file in filenames:
                if not re.search(r"\.pyc$",file):
                    print "Copying: %s"%file
                    shutil.copy(join(dirpath,file), destpath)
    os.chdir(join("..","F2FHeadless"))
    dirlist = [(x,z) for (x,y,z) in os.walk(".")]
    srcbuildpath = join("..","F2FCore","build","F2FComputingSrc") 
    for (dirpath,filenames) in dirlist:
        if not re.search(r"\.svn\b",dirpath):
            if(dirpath=="."):
                destpath = join(srcbuildpath,"F2FHeadless")
            else:
                destpath = join(srcbuildpath,"F2FHeadless",dirpath)
            print "Creating directory: %s"%destpath
            os.mkdir(destpath)
            for file in filenames:
               if not re.search(r"\.pyc$",file):
                   print "Copying: %s"%file
                   shutil.copy(join(dirpath,file), destpath)
    print "Packing."
    os.chdir(join("..","F2FCore","build"))
    killableprocess.call([zippath, "-r", "F2FComputingSrc.zip", "F2FComputingSrc"])
   

def clean():
    print "Doing clean."
    # just remove the build directory
    shutil.rmtree( builddir )
    os.mkdir(builddir)

def run(target):
    if target == "build":
        build()
    elif target == "release":
        release()
    elif target == "srcrelease":
        srcrelease()
    elif target == "clean":
        clean()
    else: usage() 

def main():
    def usage():
        print "%s: f2f build system. " % sys.argv[0]
        print "usage:"
        print "%s [build|release|srcrelease|clean]"   % sys.argv[0]
        print "            - Built f2fcore or build a release."
        print "              The build archive will end up in the build drectory."
        sys.exit(0)
    
    # check usage and read parameters
    if len(sys.argv) != 2: usage()
    target = sys.argv[1]
    run( target )

# allow this file to be called as module
if __name__ == "__main__":
    main()