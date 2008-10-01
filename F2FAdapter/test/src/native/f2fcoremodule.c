/****************************************************************
 * A python extension module for F2FCore
 * (exampe)
 ****************************************************************/
#include <Python.h>
#include <structmember.h>
#include <stdlib.h>
#include <string.h>

/* simple example of python extension in C */
/* module functions */
static PyObject* message(PyObject *self, PyObject *args){
	char *fromPython;
	char result[64];

	if (! PyArg_Parse(args, "(s)", &fromPython) ){
		return NULL;
	} else {
		strcpy(result, "Hello, ");
		strcat(result, fromPython);
		return Py_BuildValue(result);
	}
}


/* registration table */
/*
static struct PyMemberDef methods[] = {
		{
				"message". 
				message, 
				METH_VARARGS, 
				"Print the hello message."
		},
		{NULL, NULL}        /* Sentinel */
/*
};

PyMODINIT_FUNC initf2fcore(void){
	(void) Py_InitModule("f2fcore", methods);
}
*/

