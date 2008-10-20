# The actual python/scons file
# this is not called SConstruct as it must be called from the build directory with
# scons -f <path to this file> <options>

import os, glob
import distutils.sysconfig

currentpath = os.path.realpath('.')

buildpath = os.path.join('..','build')
try:
    os.mkdir(buildpath)
except OSError:
    pass # could exist already
srcpath =  os.path.join('..', 'src')
f2fcorepath = 'libf2fcore'

os.chdir(srcpath) # Ensure that the names are relative
sources =  [os.path.join( f2fcorepath, 'f2fcore.i')] \
        + glob.glob( os.path.join( f2fcorepath, '*.c') ) \
        + glob.glob( os.path.join( f2fcorepath, 'mtwist' , '*.c') ) \
        + glob.glob( os.path.join( f2fcorepath, 'b64' , '*.c') )
os.chdir(currentpath) # back to buildpath

print "Using these sources:", sources

## go to the build path, to create all files there
#os.chdir(buildpath)
env = Environment(SWIGFLAGS=['-python'],
                  CPPPATH=[distutils.sysconfig.get_python_inc(), os.path.join(srcpath,f2fcorepath)],
                  SHLIBPREFIX="")

Repository( srcpath )
 
env.SharedLibrary( '_f2fcore', sources )
