/*
 * f2fjit_wrapper.h
 *
 *  Created on: Jul 5, 2009
 *      Author: olegknut
 */

#ifndef F2FJIT_WRAPPER_H_
#define F2FJIT_WRAPPER_H_

typedef void Cf2fJIT;

#ifdef __cplusplus
extern "C" {
#endif
Cf2fJIT * f2fjit_new();
void f2fjit_run(const Cf2fJIT *t,char *filepath);
void f2fjit_delete(Cf2fJIT *t);
#ifdef __cplusplus
}
#endif


#endif /* F2FJIT_WRAPPER_H_ */
