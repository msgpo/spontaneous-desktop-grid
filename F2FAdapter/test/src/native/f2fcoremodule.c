/****************************************************************
 * A python extension module for F2FCore
 *
 ****************************************************************/

#include <stdlib.h>
#include <string.h>

#include "python2.5/Python.h"

/* simple example of python extension in C */
/* module functions */
static PyObject*
message(PyObject *self, PyObject *args){
	char *fromPython;
	char result[64];
	int n = -1;

	if (! Py_Arg_Parse(args, "(s)", &fromPython) ){
		return NULL;
	} else {
		strcpy(result, "Hello, ");
		strcat(result, fromPython);
		return Py_BuildValue(result);
	}
}

/* registration table */
static struct PyMemberDef hello_methods[] = {
		{
				"message",		// function name
				message,		// C function pointer
				1				// always tuple
		},
		{ NULL, NULL }			// end of table marker
};

void initHello(){				// called on first import
	(void) Py_InitModule4("hello", hello_methods);
}
