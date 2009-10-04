# The actual python/scons file
# this is not called SConstruct as it must be called from the build directory with
# scons -f <path to this file> <options>

import os, glob
import distutils.sysconfig
import platform

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
        + glob.glob( os.path.join( f2fcorepath, '*.cc') ) \
        + glob.glob( os.path.join( f2fcorepath, 'mtwist' , '*.c') ) \
        + glob.glob( os.path.join( f2fcorepath, 'b64' , '*.c') )
os.chdir(currentpath) # back to buildpath

#print "Using these sources:", sources

if ARGUMENTS.get('debug', 0):
    print "Enabling debug information."
    ccflags = '-g '
else:
    ccflags = ''
    print "Disabling debug information."
    
#ccflags += "`llvm-config --cxxflags`";
ccflags = ccflags + " -D__STDC_LIMIT_MACROS -D__STDC_CONSTANT_MACROS ";

if platform.system() == 'Darwin':
    ldFlagsList = ['-framework','python']
else:
    ldFlagsList = []
    
#ldFlagsList = ldFlagsList + ['-flat_namespace','-undefined','suppress']
#ldFlagsList = ldFlagsList + ['-flat_namespace']
#ldFlagsList = ldFlagsList + ["../../lib/LLVMXCore.o","../../lib/LLVMSparcCodeGen.o","../../lib/LLVMSparcAsmPrinter.o","../../lib/LLVMPowerPCAsmPrinter.o","../../lib/LLVMPowerPCCodeGen.o","../../lib/LLVMPIC16.o","../../lib/LLVMMSIL.o","../../lib/LLVMMips.o","../../lib/libLLVMLinker.a","../../lib/libLLVMipo.a","../../lib/LLVMInterpreter.o","../../lib/libLLVMInstrumentation.a","../../lib/LLVMIA64.o","../../lib/libLLVMHello.a","../../lib/LLVMExecutionEngine.o","../../lib/LLVMJIT.o","../../lib/libLLVMDebugger.a","../../lib/LLVMCppBackend.o","../../lib/LLVMCellSPUCodeGen.o","../../lib/LLVMCellSPUAsmPrinter.o","../../lib/LLVMCBackend.o","../../lib/libLLVMBitWriter.a","../../lib/LLVMX86AsmPrinter.o","../../lib/LLVMX86CodeGen.o","../../lib/libLLVMAsmParser.a","../../lib/LLVMARMAsmPrinter.o","../../lib/LLVMARMCodeGen.o","../../lib/libLLVMArchive.a","../../lib/libLLVMBitReader.a","../../lib/LLVMAlphaCodeGen.o","../../lib/libLLVMSelectionDAG.a","../../lib/LLVMAlphaAsmPrinter.o","../../lib/libLLVMAsmPrinter.a","../../lib/libLLVMCodeGen.a","../../lib/libLLVMScalarOpts.a","../../lib/libLLVMTransformUtils.a","../../lib/libLLVMipa.a","../../lib/libLLVMAnalysis.a","../../lib/libLLVMTarget.a","../../lib/libLLVMCore.a","../../lib/libLLVMSupport.a","../../lib/libLLVMSystem.a"]

llvm_base = "../../llvm/eclipse/" 

_, out, _ = os.popen3( llvm_base + "bin/llvm-config --libs all" )
llvm_objects =  out.read().strip() # TODO: make blank proof
_, out, _ = os.popen3( llvm_base + "bin/llvm-config --ldflags" )
llvm_flags = out.read().strip()
llvm_flagsnobjects = llvm_flags + ' ' + llvm_objects
print "llvm_flagsnobjects:",llvm_flagsnobjects

env = Environment(SWIGFLAGS=['-python'],
                  LINKFLAGS=ldFlagsList,
                  CPPPATH=[distutils.sysconfig.get_python_inc(),
                           os.path.join(srcpath,f2fcorepath),
                           llvm_base + "/include",
                           llvm_base + "../llvm-2.5/include"],
                  #CPPPATH=[distutils.sysconfig.get_python_inc(), os.path.join(srcpath,f2fcorepath)],
                  SHLIBPREFIX="",
                  LIBPATH=["/usr/lib/python2.5",
                           llvm_base + "/lib"],
                  LIBS=["python2.5"],
                  CCFLAGS = ccflags 
                         )
env.Append(LINKCOM=' '+llvm_flagsnobjects)
env.Append(SHLINKCOM=' '+llvm_flagsnobjects)

Repository( srcpath )
 
env.SharedLibrary( '_f2fcore', sources ) 
#env.Program( 'f2fcore', sources )
