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
JNIEXPORT jobject JNICALL
Java_com_example_compresseur_1mobile_CompressionFragment_compressImageNative(
        JNIEnv *env,
    jobject thiz,
    jobject context,
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

    channels = 3; //on force rgb

    if (img) {
        //on charge l'image dans une ImageBase
        ImageBase imIn(width, height, true);

        memcpy(imIn.data, img, width * height * 3);

        __android_log_print(ANDROID_LOG_INFO, "Compression", "ImageBase %dx%d", imIn.getWidth(), imIn.getHeight());

        //on initialise les paramètres de compression
        CompressionSettings settings;
        settings.colorFormat = YCBCRFORMAT;
        settings.blurType = GAUSSIANBLUR;
        settings.samplingType = BILENARSAMPLING;
        settings.transformationType = DCTTRANSFORM;
        settings.QuantizationFactor = quality;
        settings.tileHeight = 8;
        settings.tileWidth = 8;
        settings.encodingType = RLE;
        settings.encodingWindowSize = 20;

        //on recupere le contexte pour obtenir le répertoire de fichiers
        jclass contextClass = env->GetObjectClass(context);
        jmethodID getFilesDir = env->GetMethodID(contextClass, "getFilesDir", "()Ljava/io/File;");
        jobject filesDir = env->CallObjectMethod(context, getFilesDir);

        jclass fileClass = env->GetObjectClass(filesDir);
        jmethodID getPath = env->GetMethodID(fileClass, "getAbsolutePath", "()Ljava/lang/String;");
        jstring pathStr = (jstring)env->CallObjectMethod(filesDir, getPath);
        const char* dirPath = env->GetStringUTFChars(pathStr, nullptr);

        std::string outputPath = std::string(dirPath) + "/output.compressed";

        env->ReleaseStringUTFChars(pathStr, dirPath);
        env->DeleteLocalRef(pathStr);

        //on compresse
        compression("", outputPath.data(), imIn, settings); //first argument not used

        __android_log_print(ANDROID_LOG_INFO, "Compression", "Compression finished");

        //on affiche les fichiers dans le répertoire
        jmethodID listFiles = env->GetMethodID(fileClass, "listFiles", "()[Ljava/io/File;");
        jobjectArray fileArray = (jobjectArray)env->CallObjectMethod(filesDir, listFiles);

        jsize fileCount = env->GetArrayLength(fileArray);
        __android_log_print(ANDROID_LOG_INFO, "Compression", "Files list count: %d", fileCount);
        jmethodID getName = env->GetMethodID(fileClass, "getName", "()Ljava/lang/String;");
        for (jsize i = 0; i < fileCount; ++i) {
            jobject file = env->GetObjectArrayElement(fileArray, i);
            jstring nameStr = (jstring)env->CallObjectMethod(file, getName);
            const char* nameCStr = env->GetStringUTFChars(nameStr, nullptr);

            __android_log_print(ANDROID_LOG_INFO, "Compression", "File: %s", nameCStr);

            env->ReleaseStringUTFChars(nameStr, nameCStr);
            env->DeleteLocalRef(nameStr);
            env->DeleteLocalRef(file);
        }

        env->DeleteLocalRef(fileClass);

        stbi_image_free(img);

        __android_log_print(ANDROID_LOG_INFO, "Compression", "Starting decompression");

        //on décompresse l'image pour pouvoir l'afficher
        ImageBase imOut(width, height, true);

        decompression(outputPath.data(), "", imOut, settings);

        //on convertit en tableau d'octets
        int dataLength = width * height * 3;
        jbyteArray rgbArray = env->NewByteArray(dataLength);
        env->SetByteArrayRegion(rgbArray, 0, dataLength, reinterpret_cast<jbyte *>(imOut.data));

        __android_log_print(ANDROID_LOG_INFO, "Compression", "Instantiating Results");

        //on trouve la classe
        jclass resultClass = env->FindClass("com/example/compresseur_mobile/CompressionResult");
        if (resultClass == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Compression", "cannot find class");
            return nullptr;
        }

        //on instancie
        jobject resultObj = env->AllocObject(resultClass);
        if (resultObj == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "Compression", "Cannot allocate CompressionResult object");
            return nullptr;
        }

        //on trouve les champs
        jfieldID compressedImageField = env->GetFieldID(resultClass, "compressedImage", "[B");
        jfieldID widthField = env->GetFieldID(resultClass, "width", "I");
        jfieldID heightField = env->GetFieldID(resultClass, "height", "I");
        jfieldID psnrField = env->GetFieldID(resultClass, "psnr", "F");
        jfieldID newSizeField = env->GetFieldID(resultClass, "newSize", "J");

        if (!compressedImageField || !widthField || !heightField) {
            __android_log_print(ANDROID_LOG_ERROR, "Compression", "Cannot find one or more fields");
            return nullptr;
        }

        //on set les champs
        env->SetObjectField(resultObj, compressedImageField, rgbArray);
        env->SetIntField(resultObj, widthField, width);
        env->SetIntField(resultObj, heightField, height);
        jlong newSize = getFileSize(outputPath);
        env->SetLongField(resultObj, newSizeField, newSize);

        __android_log_print(ANDROID_LOG_INFO, "Compression", "PSNR");

        float psnr = PSNR(imIn, imOut);

        env->SetFloatField(resultObj, psnrField, psnr);

        imIn.reset();
        imOut.reset();

        __android_log_print(ANDROID_LOG_INFO, "Compression", "CPP finished");

        return resultObj;


    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Compression", "Impossible de charger l'image");
    }



    return nullptr;
}