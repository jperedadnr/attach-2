/*
 * Copyright (c) 2020, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "pushnotifications.h"

static JavaVM* graalVM;
static JNIEnv *graalEnv;

// Graal handles
static jclass jGraalPushNotificationsClass;
jmethodID jGraalSetTokenMethod;

static void initializeGraalHandles(JNIEnv* env) {
    jGraalPushNotificationsClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/gluonhq/attach/pushnotifications/impl/AndroidPushNotificationsService"));
    jGraalSetTokenMethod = (*env)->GetStaticMethodID(env, jGraalPushNotificationsClass, "setToken", "(Ljava/lang/String;)V");
}

// Dalvik handles
static jobject jDalvikPushNotificationsService;
jmethodID jDalvikPushNotificationsServiceEnableDebug;
jmethodID jDalvikPushNotificationsServiceGetPackageName;
jmethodID jDalvikPushNotificationsServiceIsGooglePlayServicesAvailable;
jmethodID jDalvikPushNotificationsServiceGetErrorString;
jmethodID jDalvikPushNotificationsServiceInitializeFirebase;

static void initializeDalvikHandles() {
    androidVM = substrateGetAndroidVM();
    jclass activityClass = substrateGetActivityClass();
    jobject jActivity = substrateGetActivity();
    jclass jPushNotificationsServiceClass = substrateGetPushNotificationsServiceClass();

    if ((*androidVM)->GetEnv(androidVM, (void **)&androidEnv, JNI_VERSION_1_6) != JNI_OK) {
        ATTACH_LOG_FINE("initializeDalvikHandles, thread is not linked to JNIEnv, doing that now.\n");
        (*androidVM)->AttachCurrentThread(androidVM, (void **)&androidEnv, NULL);
    }
    jmethodID jPushNotificationsServiceServiceInitMethod = (*androidEnv)->GetMethodID(androidEnv, jPushNotificationsServiceClass, "<init>", "(Landroid/app/Activity;)V");
    jDalvikPushNotificationsServiceEnableDebug = (*androidEnv)->GetMethodID(androidEnv, jPushNotificationsServiceClass, "enableDebug", "()V");
    jDalvikPushNotificationsServiceGetPackageName = (*androidEnv)->GetMethodID(androidEnv, jPushNotificationsServiceClass, "getPackageName", "()java/lang/String;");
    jDalvikPushNotificationsServiceIsGooglePlayServicesAvailable = (*androidEnv)->GetMethodID(androidEnv, jPushNotificationsServiceClass, "isGooglePlayServicesAvailable", "()I");
    jDalvikPushNotificationsServiceGetErrorString = (*androidEnv)->GetMethodID(androidEnv, jPushNotificationsServiceClass, "getErrorString", "(I)java/lang/String;");
    jDalvikPushNotificationsServiceInitializeFirebase = (*androidEnv)->GetMethodID(androidEnv, jPushNotificationsServiceClass, "initializeFirebase", "(Ljava/lang/String;Ljava/lang/String;)V");
    jthrowable t = (*androidEnv)->ExceptionOccurred(androidEnv);
    if (t) {
        ATTACH_LOG_INFO("EXCEPTION occured when dealing with dalvik handles\n");
        (*androidEnv)->ExceptionClear(androidEnv);
    }

    jobject jObj = (*androidEnv)->NewObject(androidEnv, jPushNotificationsServiceClass, jPushNotificationsServiceServiceInitMethod, jActivity);
    jDalvikPushNotificationsService = (jobject)(*androidEnv)->NewGlobalRef(androidEnv, jObj);
}

///////////////////////////
// From native to dalvik //
///////////////////////////

void dalvikEnableDebug() {
    if ((*androidVM)->GetEnv(androidVM, (void **)&androidEnv, JNI_VERSION_1_6) != JNI_OK) {
        ATTACH_LOG_WARNING("dalvikEnableDebug called from not-attached thread\n");
        (*androidVM)->AttachCurrentThread(androidVM, (void **)&androidEnv, NULL);
    }  else {
        ATTACH_LOG_FINE("dalvikEnableDebug called from attached thread %p\n", androidEnv);
    }

    (*androidEnv)->CallVoidMethod(androidEnv, jDalvikPushNotificationsService, jDalvikPushNotificationsServiceEnableDebug);
}

jstring dalvikGetPackageName(JNIEnv *env) {
    if ((*androidVM)->GetEnv(androidVM, (void **)&androidEnv, JNI_VERSION_1_6) != JNI_OK) {
        ATTACH_LOG_WARNING("dalvikGetPackageName called from not-attached thread\n");
        (*androidVM)->AttachCurrentThread(androidVM, (void **)&androidEnv, NULL);
    }  else {
        ATTACH_LOG_FINE("dalvikGetPackageName called from attached thread %p\n", androidEnv);
    }

    jstring dalvikPackageName = (jstring) (*androidEnv)->CallObjectMethod(androidEnv, jDalvikPushNotificationsService, jDalvikPushNotificationsServiceGetPackageName);
    const char *packageNameChars = (*androidEnv)->GetStringUTFChars(androidEnv, dalvikPackageName, NULL);
    jstring graalPackageName = (*env)->NewStringUTF(env, packageNameChars);
    (*androidEnv)->ReleaseStringUTFChars(androidEnv, dalvikPackageName, packageNameChars);
    return graalPackageName;
}

jint dalvikIsGooglePlayServicesAvailable() {
    if ((*androidVM)->GetEnv(androidVM, (void **)&androidEnv, JNI_VERSION_1_6) != JNI_OK) {
        ATTACH_LOG_WARNING("dalvikIsGooglePlayServicesAvailable called from not-attached thread\n");
        (*androidVM)->AttachCurrentThread(androidVM, (void **)&androidEnv, NULL);
    }  else {
        ATTACH_LOG_FINE("dalvikIsGooglePlayServicesAvailable called from attached thread %p\n", androidEnv);
    }

    return (*androidEnv)->CallIntMethod(androidEnv, jDalvikPushNotificationsService, jDalvikPushNotificationsServiceIsGooglePlayServicesAvailable);
}

jstring dalvikGetErrorString(JNIEnv *env, jint resultCode) {
    if ((*androidVM)->GetEnv(androidVM, (void **)&androidEnv, JNI_VERSION_1_6) != JNI_OK) {
        ATTACH_LOG_WARNING("dalvikGetErrorString called from not-attached thread\n");
        (*androidVM)->AttachCurrentThread(androidVM, (void **)&androidEnv, NULL);
    }  else {
        ATTACH_LOG_FINE("dalvikGetErrorString called from attached thread %p\n", androidEnv);
    }

    jstring dalvikErrorString = (jstring) (*androidEnv)->CallObjectMethod(androidEnv, jDalvikPushNotificationsService, jDalvikPushNotificationsServiceGetErrorString,
            resultCode);
    const char *errorStringChars = (*androidEnv)->GetStringUTFChars(androidEnv, dalvikErrorString, NULL);
    jstring graalErrorString = (*env)->NewStringUTF(env, errorStringChars);
    (*androidEnv)->ReleaseStringUTFChars(androidEnv, dalvikErrorString, errorStringChars);
    return graalErrorString;
}

void dalvikInitializeFirebase(JNIEnv *env, jstring applicationId, jstring projectNumber) {
    if ((*androidVM)->GetEnv(androidVM, (void **)&androidEnv, JNI_VERSION_1_6) != JNI_OK) {
        ATTACH_LOG_WARNING("dalvikInitializeFirebase called from not-attached thread\n");
        (*androidVM)->AttachCurrentThread(androidVM, (void **)&androidEnv, NULL);
    }  else {
        ATTACH_LOG_FINE("dalvikInitializeFirebase called from attached thread %p\n", androidEnv);
    }

    const char *applicationIdChars = (*env)->GetStringUTFChars(env, applicationId, NULL);
    const char *projectNumberChars = (*env)->GetStringUTFChars(env, projectNumber, NULL);
    jstring dalvikApplicationId = (*androidEnv)->NewStringUTF(androidEnv, applicationIdChars);
    jstring dalvikProjectNumber = (*androidEnv)->NewStringUTF(androidEnv, projectNumberChars);
    (*androidEnv)->CallVoidMethod(androidEnv, jDalvikPushNotificationsService, jDalvikPushNotificationsServiceInitializeFirebase,
            dalvikApplicationId, dalvikProjectNumber);
    (*androidEnv)->DeleteLocalRef(androidEnv, dalvikApplicationId);
    (*androidEnv)->DeleteLocalRef(androidEnv, dalvikProjectNumber);
    (*env)->ReleaseStringUTFChars(env, applicationId, applicationIdChars);
    (*env)->ReleaseStringUTFChars(env, projectNumber, projectNumberChars);
}

//////////////////////////
// From Graal to native //
//////////////////////////

JNIEXPORT jint JNICALL
JNI_OnLoad_PushNotifications(JavaVM *vm, void *reserved)
{
#ifdef JNI_VERSION_1_8
    graalVM = vm;
    if ((*vm)->GetEnv(vm, (void **)&graalEnv, JNI_VERSION_1_8) != JNI_OK) {
        ATTACH_LOG_WARNING("Error initializing native push notifications from OnLoad");
        return JNI_FALSE;
    }
    ATTACH_LOG_FINE("Initializing native push notifications from OnLoad");
    initializeGraalHandles(graalEnv);
    initializeDalvikHandles();
    ATTACH_LOG_FINE("Initializing native push notifications from OnLoad Done");
    return JNI_VERSION_1_8;
#else
    #error Error: Java 8+ SDK is required to compile Attach
#endif
}

JNIEXPORT void JNICALL Java_com_gluonhq_attach_pushnotifications_impl_AndroidPushNotificationsService_enableDebug
(JNIEnv *env, jobject service) {
    dalvikEnableDebug();
}

JNIEXPORT jstring JNICALL Java_com_gluonhq_attach_pushnotifications_impl_AndroidPushNotificationsService_getPackageName
(JNIEnv *env, jobject service) {
    return dalvikGetPackageName(env);
}

JNIEXPORT jint JNICALL Java_com_gluonhq_attach_pushnotifications_impl_AndroidPushNotificationsService_isGooglePlayServicesAvailable
(JNIEnv *env, jobject service) {
    return dalvikIsGooglePlayServicesAvailable();
}

JNIEXPORT jstring JNICALL Java_com_gluonhq_attach_pushnotifications_impl_AndroidPushNotificationsService_getErrorString
(JNIEnv *env, jobject service, jint resultCode) {
    return dalvikGetErrorString(env, resultCode);
}

JNIEXPORT void JNICALL Java_com_gluonhq_attach_pushnotifications_impl_AndroidPushNotificationsService_initializeFirebase
(JNIEnv *env, jobject service, jstring applicationId, jstring projectNumber) {
    dalvikInitializeFirebase(env, applicationId, projectNumber);
}
