#define _WIN32_WINNT 0x0500 //minimum needed version is Windows 2000

#include <jni.h>
#include <stdio.h>
#include <windows.h>
#include <direct.h>

#include "windowsinf.h"
 
#define UNICODE 

unsigned __int64 i64FreeBytesToCaller, i64TotalBytes, i64FreeBytes;
unsigned __int64 freeBytesToCallerSum = 0, totalBytesSum = 0, freeBytesSum = 0;
MEMORYSTATUSEX statex;
BOOL  fResult;
UINT volumeType = 0;
char drive[80];

JNIEXPORT jobject JNICALL Java_ee_ut_f2f_gatherer_parameters_WinDllInformation_findWinData
  (JNIEnv *env, jobject obj, jobject obj2)
{	
	
	jclass clss = env->FindClass("ee/ut/f2f/gatherer/model/WindowsAttributes");
	
	statex.dwLength = sizeof (statex);
	GlobalMemoryStatusEx(&statex);	
	
	jfieldID fid = env->GetFieldID( clss, "memoryLoad", "I");
	if (env->ExceptionOccurred()){
		env->ExceptionDescribe();
		env->ExceptionClear();
	}
	env->SetIntField(obj2, fid, statex.dwMemoryLoad);
	
	fid = env->GetFieldID( clss, "totalPhysicalMemory", "J");
	env->SetLongField(obj2, fid, statex.ullTotalPhys);
	
	fid = env->GetFieldID( clss, "freePhysicalMemory", "J");
	env->SetLongField(obj2, fid, statex.ullAvailPhys);
	
	fid = env->GetFieldID( clss, "totalPagingFile", "J");
	env->SetLongField(obj2, fid, statex.ullTotalPageFile);
	
	fid = env->GetFieldID( clss, "freePagingFile", "J");
	env->SetLongField(obj2, fid, statex.ullAvailPageFile);
	
	fid = env->GetFieldID( clss, "totalVirtualMemory", "J");
	env->SetLongField(obj2, fid, statex.ullTotalVirtual);
	
	fid = env->GetFieldID( clss, "freeVirtualMemory", "J");
	env->SetLongField(obj2, fid, statex.ullAvailVirtual);

	char lpBuffer[1000];
	long retSize=GetLogicalDriveStrings(1000,lpBuffer);
	for(int i=0;i<=retSize;i++)
	{
		volumeType = GetDriveType(&lpBuffer[i]);
		if(lpBuffer[i]!='0' && volumeType == 3 && lpBuffer[i] != '\\' ) {
			//printf("HDD: %c\n",lpBuffer[i]);
			sprintf(drive, "%c:\\", lpBuffer[i]);
			BOOL fResult = GetDiskFreeSpaceEx (drive,
								(PULARGE_INTEGER)&i64FreeBytesToCaller,
								(PULARGE_INTEGER)&i64TotalBytes,
								(PULARGE_INTEGER)&i64FreeBytes);
				if(fResult) {
					//printf ("Available space to caller = %I64u bytes\n", i64FreeBytesToCaller );
					//printf ("Total space               = %I64u bytes\n", i64TotalBytes );
					//printf ("Free space on drive       = %I64u bytes\n", i64FreeBytes );
					freeBytesToCallerSum += i64TotalBytes; 
					freeBytesSum += i64FreeBytes;
					//printf("%u\n",i64TotalBytes);
				}
		}
	}

	fid = env->GetFieldID( clss, "totalDiskSpace", "J");
	env->SetLongField(obj2, fid, i64TotalBytes);

	fid = env->GetFieldID( clss, "freeDiskSpace", "J");
	env->SetLongField(obj2, fid, i64FreeBytes);

	//fid = env->GetFieldID( clss, "totalDiskSpace", "Ljava/lang/Long;");
	//env->SetLongField(obj2, fid, i64TotalBytes);
	
	return obj2;	
}
 