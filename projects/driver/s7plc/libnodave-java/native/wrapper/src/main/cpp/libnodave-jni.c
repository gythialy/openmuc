/*
 * libnodave-jni.c
 *
 *  Created on: Aug 11, 2009
 *      Author: mzillgit
 */

#include <jni.h>
#include "nodave.h"
#include "openSocket.h"
#include "com_libnodave_Interface.h"

JNIEXPORT jint JNICALL Java_com_libnodave_Interface_daveOpenSocket
  (JNIEnv * jni_env, jclass js, jstring hostname, jint port)
{
	// _daveOSserialType fds;
	int sock;
	char* hostname_str;

	hostname_str = (*jni_env)->GetStringUTFChars(jni_env, hostname, NULL);
	sock = openSocket(port, hostname_str);

#ifdef DEBUG
	printf("Socket: %i\n", sock);
#endif

	return sock;
}

JNIEXPORT void JNICALL Java_com_libnodave_Interface_daveCloseSocket
  (JNIEnv * jni_env, jclass js, jint socket)
{
	close(socket);
}

JNIEXPORT jlong JNICALL Java_com_libnodave_Interface_daveNewInterface
  (JNIEnv * jni_env, jclass jc, jstring name, jint sock)
{
	_daveOSserialType fds;
	daveInterface* di;

	fds.wfd = sock;
	fds.rfd = sock;

	if (fds.rfd > 0) {
		di = daveNewInterface (fds, name, 0, daveProtoISOTCP, 0);
		return (long) di;
	}
	else
		return (long) NULL;
} /* daveNewInterface() */

JNIEXPORT void JNICALL Java_com_libnodave_Interface_daveSetTimeout
  (JNIEnv * jni_env, jclass jc, jlong di, jlong timeout)
{
	daveSetTimeout((daveInterface*) di, (int) timeout);
} /* daveSetTimeout() */

JNIEXPORT jlong JNICALL Java_com_libnodave_Interface_daveNewConnection
  (JNIEnv * jni_env, jclass jc, jlong di, jint mpi_addr, jint rack, jint slot)
{
	daveConnection* dc;

	dc = daveNewConnection((daveInterface*) di, mpi_addr, rack, slot);

	return (long) dc;
}

JNIEXPORT jint JNICALL Java_com_libnodave_Interface_daveConnectPLC
  (JNIEnv * jni_env, jclass jc, jlong dc)
{
	return daveConnectPLC((daveConnection*) dc);
}

JNIEXPORT jint JNICALL Java_com_libnodave_Interface_daveDisconnectPLC
  (JNIEnv * jni_env, jclass jc, jlong dc)
{
	return daveDisconnectPLC((daveConnection*) dc);
}

JNIEXPORT void JNICALL Java_com_libnodave_Interface_daveWriteBytes
  (JNIEnv* jni_env, jclass jc, jlong dc, jint area, jint area_addr,
		  jint start_addr, jint length, jbyteArray buffer)
{
	unsigned char* buf;
	int res;

	buf = (*jni_env)->GetByteArrayElements(jni_env, buffer, NULL);

	res = daveWriteBytes((daveConnection*) dc, area, area_addr, start_addr, length, buf);

	(*jni_env)->ReleaseByteArrayElements(jni_env, buffer, buf, JNI_ABORT);

	if (res != 0) {
		// fprintf(stderr, "daveReadBytes-> error %d\n", res);

		jclass exception = (*jni_env)->FindClass(jni_env, "java/io/IOException");
		(*jni_env)->ThrowNew(jni_env, exception, daveStrerror(res));
	}
}

JNIEXPORT jbyteArray JNICALL Java_com_libnodave_Interface_daveReadBytes
  (JNIEnv * jni_env, jclass jc, jlong dc, jint area, jint area_addr, jint start_addr, jint length)
{
	jbyteArray jb = NULL;

	static unsigned char buf[1024];

	jclass exception;

	int i;

	int res;

	// buf = malloc(length);

	jb = (*jni_env)->NewByteArray(jni_env, length);

	res = daveReadBytes((daveConnection*) dc, area, area_addr, start_addr, length, buf);

	if (res != 0) {
		// fprintf(stderr, "daveReadBytes-> error %d\n", res);
		daveStrerror(res);
		
		exception = (*jni_env)->FindClass(jni_env, "java/io/IOException");
		(*jni_env)->ThrowNew(jni_env, exception, "Read bytes failed!");
		
		return NULL;
	}

	(*jni_env)->SetByteArrayRegion(jni_env, jb, 0, length, (jbyte*) buf);


	// free(length);

	return jb;
}

JNIEXPORT jint JNICALL Java_com_libnodave_Interface_daveSetBit
  (JNIEnv * jni_env, jclass jc, jlong dc, jint area, jint area_addr, jint byte_addr, jint bit_addr)
 {
	jclass exception;
 	jint res = daveSetBit((daveConnection*) dc, area, area_addr, byte_addr, bit_addr);
 	
 	if (res != 0) {
		// fprintf(stderr, "daveReadBytes-> error %d\n", res);
		daveStrerror(res);
		
		exception = (*jni_env)->FindClass(jni_env, "java/io/IOException");
		(*jni_env)->ThrowNew(jni_env, exception, "Read bytes failed!");
	}
 	
 	return res;
 }

JNIEXPORT jint JNICALL Java_com_libnodave_Interface_daveClrBit
  (JNIEnv * jni_env, jclass jc, jlong dc, jint area, jint area_addr, jint byte_addr, jint bit_addr)
 {
	jclass exception;
 	jint res = daveClrBit((daveConnection*) dc, area, area_addr, byte_addr, bit_addr);
 	
 	 	if (res != 0) {
		// fprintf(stderr, "daveReadBytes-> error %d\n", res);
		daveStrerror(res);
		
		exception = (*jni_env)->FindClass(jni_env, "java/io/IOException");
		(*jni_env)->ThrowNew(jni_env, exception, "Read bytes failed!");
	}
 	
 	return res;
 }
 

