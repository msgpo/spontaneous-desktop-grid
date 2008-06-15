#include <jni.h>
#include <string.h>
#include <iostream>
#include <fstream>
#include <sstream>

#include "linuxinf.h"

using namespace std;

JNIEXPORT jobject JNICALL Java_ee_ut_f2f_gatherer_parameters_LinuxSoInformation_findData
  (JNIEnv *env, jobject obj, jobject obj2)
{
	long memtotal, memfree, swaptotal, swapfree;
	unsigned long long dsktotal = 0, dskfree = 0;
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
	srand((unsigned)time(0));
	int random_integer = rand(); 
	//convert random integer to string
	std::string s;
	std::stringstream out;
	out << random_integer;
	s = out.str();
	out.flush();

	std::string strbeg = "df -T > ";
	std::string fileName = "df_tmp_output"+s+".txt";
	std::string instr = strbeg+fileName;

	if(system(instr.c_str()) == 0) { //successful
		std::string str;
		char str1[1024], type[1024];
		char excludeFs[] = "tmpfs"; //exclude this type of fs
		unsigned long long curtotal = 0, curfree = 0, dskused = 0;
		//this is not thread safe
		ifstream in(fileName.c_str()); 
		getline(in,str);  // first line is column descriptions
		getline(in,str);
		while ( in ) {  // Continue if the line was sucessfully read.
			sscanf (str.c_str(), "%s %s %llud %llud %llud", &str1, &type, &curtotal, &dskused, &curfree);
			if(strcmp(type,excludeFs) != 0) { //if not tmp filesystem
				dsktotal += curtotal;
				dskfree += curfree;
			}
			getline(in,str);   // Get another line.
		}
		system(("rm "+fileName).c_str()); //remove tmp file
	}	
	fid = env->GetFieldID( clss, "totalDiskSpace", "J");
	env->SetLongField(obj2, fid, dsktotal);
	fid = env->GetFieldID( clss, "freeDiskSpace", "J");
	env->SetLongField(obj2, fid, dskfree);
	return obj2;
}