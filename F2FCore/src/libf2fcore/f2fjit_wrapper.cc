/*
 * f2fjit_wrapper.cc
 *
 *  Created on: Jul 5, 2009
 *      Author: olegknut
 */

#include "f2fjit_wrapper.h"
#include "f2fjit.hh"

extern "C" {

Cf2fJIT * f2fjit_new() {
	   f2fJIT *t = new f2fJIT();

       return (Cf2fJIT *)t;
}

void f2fjit_run(const Cf2fJIT *test, char *filepath) {
        f2fJIT *t = (f2fJIT *)test;
        t->run(filepath);
}

void f2fjit_delete(Cf2fJIT *test) {

		Cf2fJIT *t = (f2fJIT *)test;

        delete t;
}
}
