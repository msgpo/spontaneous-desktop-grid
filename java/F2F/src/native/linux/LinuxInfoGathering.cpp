#include <jni.h>
#include <stdio.h>
#include <string.h>

#include "linuxinf.h"


JNIEXPORT jobject JNICALL Java_ee_ut_f2f_gatherer_parameters_LinuxSoInformation_findData
  (JNIEnv *env, jobject obj, jobject obj2)
{
	long memtotal, memfree, swaptotal, swapfree;
	FILE* fp;
	char buffer[1024];
	size_t bytes_read;
	char* match;
	jfieldID fid;
	jclass clss = env->FindClass("ee/ut/f2f/gatherer/model/LinuxAttributes");
	fp = fopen ("/proc/meminfo", "r");
	bytes_read = fread (buffer, 1, sizeof (buffer), fp);
	fclose (fp);
	if (!(bytes_read == 0 || bytes_read == sizeof (buffer))) {
		buffer[bytes_read] = '\0';
		match = strstr (buffer, "MemTotal");
		if (match != NULL) {
			sscanf (match, "MemTotal : %ld", &memtotal);
			memtotal *= 1024; //to bytes
			fid = env->GetFieldID( clss, "totalPhysicalMemory", "J");
			env->SetLongField(obj2, fid, memtotal);
		}
		match = strstr (buffer, "MemFree");
		if (match != NULL) {
			sscanf (match, "MemFree : %ld", &memfree);
			memfree *= 1024; //to bytes
			fid = env->GetFieldID( clss, "freePhysicalMemory", "J");
			env->SetLongField(obj2, fid, memfree);
		}
		match = strstr (buffer, "SwapTotal");
		if (match != NULL) {
			sscanf (match, "SwapTotal : %ld", &swaptotal);
			swaptotal *= 1024; //to bytes
			fid = env->GetFieldID( clss, "totalSwapFile", "J");
			env->SetLongField(obj2, fid, swaptotal);
		}
		match = strstr (buffer, "SwapFree");
		if (match != NULL) {
			sscanf (match, "SwapFree : %ld", &swapfree);
			swapfree *= 1024; //to bytes
			fid = env->GetFieldID( clss, "freeSwapFile", "J");
			env->SetLongField(obj2, fid, swapfree);
		}
	}
	fid = env->GetFieldID( clss, "totalDiskSpace", "J");
	env->SetLongField(obj2, fid, 0);
	fid = env->GetFieldID( clss, "freeDiskSpace", "J");
	env->SetLongField(obj2, fid, 0);
	return obj2;
}