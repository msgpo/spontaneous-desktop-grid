#include <stdio.h>
#include <stdlib.h>

#include "jni.h"

int main() {
	JavaVM *jvm;
	JNIEnv *env;

	JavaVMInitArgs vm_args;
	JavaVMOption options[2];
	
	/*
	options[0].optionString = "-Xms4M";
	options[1].optionString = "-Xmx64M";
	options[2].optionString = "-Xss512K";
	options[3].optionString = "-Xoss400K";
	*/

	/* disable JIT */
	//options[4].optionString ="-Djava.compiler=NONE";

	/* user classes */
	//options[0].optionString = "-Djava.class.path=/home/mac/workspace/JVMTest/Debug/classes";
	options[0].optionString = "-Djava.class.path=./classes";

	/* native lib path */
	//options[6].optionString = "-Djava.library.path=c:\\mylibs";

	/* print JNI msgs */
	options[1].optionString = "-verbose:jni";

	vm_args.version = JNI_VERSION_1_6;
	vm_args.options = options;
	vm_args.nOptions = 2;
	vm_args.ignoreUnrecognized = JNI_FALSE;

	int ret = JNI_CreateJavaVM(&jvm, (void **)&env, &vm_args);
    if (ret < 0) {
        fprintf(stderr, "Can't create Java VM");
        goto destroy;
    }
	
	jclass cls = (*env)->FindClass(env, "Hallo");
	if (cls == 0) {
		fprintf(stderr, "Can't find Hallo class\n");
		goto destroy;
	}

	jmethodID mid = (*env)->GetStaticMethodID(env, cls, "main",
					"([Ljava/lang/String;)V");
	if (mid == 0) {
		fprintf(stderr, "Can't find Hallo.main\n");
		goto destroy;
	}

	jstring jstr = (*env)->NewStringUTF(env, " from C!");
	if (jstr == 0) {
		fprintf(stderr, "Out of memory\n");
		goto destroy;
	}
	jobjectArray args = (*env)->NewObjectArray(env, 1, (*env)->FindClass(env, "java/lang/String"), jstr);
	if (args == 0) {
		fprintf(stderr, "Out of memory\n");
		goto destroy;
	}
	(*env)->CallStaticVoidMethod(env, cls, mid, args);

	destroy: if ((*env)->ExceptionOccurred(env)) {
		(*env)->ExceptionDescribe(env);
	}

	(*jvm)->DestroyJavaVM(jvm);

	return 0;
}
