/*
 * f2fjit.hh
 *
 *  Created on: Jul 5, 2009
 *      Author: olegknut
 */

#ifndef F2FJIT_HH_
#define F2FJIT_HH_

#include "llvm/Module.h"
#include "llvm/PassManagers.h"
#include "llvm/Bitcode/ReaderWriter.h"
#include "llvm/Assembly/PrintModulePass.h"
#include "llvm/Analysis/Verifier.h"
#include "llvm/Analysis/LoopPass.h"
#include "llvm/Analysis/CallGraph.h"
#include "llvm/System/Signals.h"
#include "llvm/Support/ManagedStatic.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/PluginLoader.h"
#include "llvm/Support/Streams.h"
#include "llvm/Support/SystemUtils.h"
#include "llvm/ExecutionEngine/ExecutionEngine.h"
#include "llvm/ModuleProvider.h"
#include "llvm/Target/TargetData.h"
#include "llvm/LinkAllPasses.h"

class f2fJIT {
        public:
        	void run(std::string input);
        	f2fJIT();
        private:
        	llvm::Function* create(llvm::Module** out, std::string input);
};


#endif /* F2FJIT_HH_ */
