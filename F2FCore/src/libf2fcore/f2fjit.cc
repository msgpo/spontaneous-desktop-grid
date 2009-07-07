/*
 * f2fjit.cc
 *
 *  Created on: Jul 5, 2009
 *      Author: olegknut
 */

#include <iostream>

#include "f2fjit.hh"
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

using namespace std;

f2fJIT::f2fJIT() {


}

llvm::Function* f2fJIT::create(llvm::Module** out, std::string input) {

	std::string error;
	llvm::Module* jit;

	// Load in the bitcode file
	llvm::MemoryBuffer* buffer = llvm::MemoryBuffer::getFileOrSTDIN(input, &error);
	std::cout << error << std::endl;

	jit = llvm::ParseBitcodeFile(buffer, &error);
	std::cout << error << std::endl;

	delete buffer; // no need it anymore

	llvm::Function* main;

	try {

		// Get the function from object
		main =  jit->getFunction(std::string("main"));

	} catch (char* e) {

		std::cout << "There was a problem with running main function." << std::endl;

	}

	llvm::BasicBlock *bb = llvm::BasicBlock::Create("entry", main);

	std::vector<llvm::Value*> params;
	llvm::CallInst* call = llvm::CallInst::Create(main, params.begin(), params.end(), "", bb);

	llvm::ReturnInst::Create(call,bb);

	*out = jit;
	return main;

}

void f2fJIT::run(std::string input) {

	// Create!
	llvm::Module* jit;
	llvm::Function* func = create(&jit,input);

	// JIT start here
	llvm::ExistingModuleProvider* mp = new llvm::ExistingModuleProvider(jit);
	llvm::ExecutionEngine* engine = llvm::ExecutionEngine::create(mp);


	// optimizations start here
	/*
	PassManager p;

	p.add(new TargetData(*engine->getTargetData()));
	p.add(llvm::createVerifierPass());
	p.add(llvm::createLowerSetJmpPass());
	p.add(llvm::createRaiseAllocationsPass());
	p.add(llvm::createCFGSimplificationPass());
	p.add(llvm::createPromoteMemoryToRegisterPass());
	p.add(llvm::createGlobalOptimizerPass());
	p.add(llvm::createGlobalDCEPass());
	p.add(llvm::createFunctionInliningPass());

	// Run these optimizations on our Module
	p.run(*jit);
	*/

	// output
	//std::cout << "Created\n" << *jit;

	// Ask JIT for our function
	int (*callMe)() = (int (*)())engine->getPointerToFunction(func);

	callMe();

}
