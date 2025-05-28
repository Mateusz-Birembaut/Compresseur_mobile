#include <jni.h>
#include <string>
#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"
#include "ImageBase.h"
#include "Utils.h"
#include "JPEG.h"
#include <vector>
#include <android/log.h>




extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_compresseur_1mobile_CompressionFragment_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello world !";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_compresseur_1mobile_CompressionFragment_compressImageNative(
        JNIEnv *env,
    jobject,
    jbyteArray imageData,
            jint quality,
    jint method) {

    jsize length = env->GetArrayLength(imageData);
    jbyte *rawImageData = env->GetByteArrayElements(imageData, nullptr);

    std::vector<uint8_t> imageBytes(reinterpret_cast<uint8_t *>(rawImageData),reinterpret_cast<uint8_t *>(rawImageData + length));

    env->ReleaseByteArrayElements(imageData, rawImageData, JNI_ABORT);

    //charge l'image depuis les octets
    int width, height, channels;
    unsigned char *img = stbi_load_from_memory(imageBytes.data(), imageBytes.size(), &width, &height, &channels, 3);

    if (img) {

        ImageBase imIn(width, height, true);

        memcpy(imIn.data, img, width * height * 3);

        __android_log_print(ANDROID_LOG_INFO, "Compression", "ImageBase %dx%d", imIn.getWidth(), imIn.getHeight());

        CompressionSettings settings;
        settings.colorFormat = YCBCRFORMAT;
        settings.blurType = GAUSSIANBLUR;
        settings.samplingType = BILENARSAMPLING;
        settings.transformationType = DCTTRANSFORM;
        settings.QuantizationFactor = 100;
        settings.tileHeight = 1080;
        settings.tileWidth = 1920;
        settings.encodingType = RLE;
        settings.encodingWindowSize = 18;

        //pour l'instant ca plante
        //compression("input.jpg", "output.compressed", imIn, settings);

        imIn.reset(); //free les tableau

        __android_log_print(ANDROID_LOG_INFO, "Compression", "Compression finished");


        //liste les fichiers
        jclass contextClass = env->GetObjectClass(jobject(/* votre objet context ici */));
        jmethodID getFilesDir = env->GetMethodID(contextClass, "getFilesDir", "()Ljava/io/File;");
        jobject fileObj = env->CallObjectMethod(jobject(/* votre objet context ici */), getFilesDir);

        jclass fileClass = env->FindClass("java/io/File");
        jmethodID listFilesMethod = env->GetMethodID(fileClass, "listFiles", "()[Ljava/io/File;");
        jobjectArray files = (jobjectArray)env->CallObjectMethod(fileObj, listFilesMethod);

        jsize fileCount = env->GetArrayLength(files);
        for (jsize i = 0; i < fileCount; ++i) {
            jobject file = env->GetObjectArrayElement(files, i);
            jmethodID getName = env->GetMethodID(fileClass, "getName", "()Ljava/lang/String;");
            jstring fileName = (jstring)env->CallObjectMethod(file, getName);
            const char* name = env->GetStringUTFChars(fileName, nullptr);
            __android_log_print(ANDROID_LOG_INFO, "InternalStorage", "Fichier: %s", name);
            env->ReleaseStringUTFChars(fileName, name);
            env->DeleteLocalRef(fileName);
            env->DeleteLocalRef(file);
        }

    stbi_image_free(img);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Compression", "Impossible de charger l'image");
    }

    __android_log_print(ANDROID_LOG_INFO, "Compression", "Image decode: %dx%d channels: %d", width, height, channels);

}