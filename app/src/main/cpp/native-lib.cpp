#include <jni.h>
#include <string>
#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"
#include "ImageBase.h"
#include "Utils.h"
#include "JPEG.h"
#include "JPEG2000.h"
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
        jint method,
        jstring filename) {

    __android_log_print(ANDROID_LOG_INFO, "Compression", "filename: %s", env->GetStringUTFChars(filename, nullptr));

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
        ImageBase imTemp(width, height, true);

        memcpy(imTemp.data, img, width * height * 3);

        __android_log_print(ANDROID_LOG_INFO, "Compression", "Image loaded: %dx%d with %d channels", width, height, channels);

        if(method == 1){
            //on est obligé de faire des dimensions multiples de 16 pour la compression JPEG
            if(width % 16 != 0){width += (16 - (width % 16));}
            if(height % 16 != 0){height += (16 - (height % 16));}
        }else if(method == 2){
            //on est obligé de faire des dimensions multiples de 256 pour la compression JPEG2000
            if(width % 256 != 0){width += (256 - (width % 256));}
            if(height % 256 != 0){height += (256 - (height % 256));}
        }


        ImageBase imIn(width, height, true);

        int oldWidth = imTemp.getWidth();
        int oldHeight = imTemp.getHeight();

        for (int y = 0; y < imIn.getHeight(); ++y) {
            for (int x = 0; x < imIn.getWidth(); ++x) {
                for (int c = 0; c < 3; ++c) {
                    if (x < oldWidth && y < oldHeight) {
                        imIn.data[(y * imIn.getWidth() + x) * 3 + c] = imTemp.data[(y * oldWidth + x) * 3 + c];
                    } else {
                        imIn.data[(y * imIn.getWidth() + x) * 3 + c] = 0;
                    }
                }
            }
        }

        __android_log_print(ANDROID_LOG_INFO, "Compression", "ImageBase %dx%d", imIn.getWidth(), imIn.getHeight());




        //on recupere le contexte pour obtenir le répertoire de fichiers
        jclass contextClass = env->GetObjectClass(context);
        jmethodID getFilesDir = env->GetMethodID(contextClass, "getFilesDir", "()Ljava/io/File;");
        jobject filesDir = env->CallObjectMethod(context, getFilesDir);

        jclass fileClass = env->GetObjectClass(filesDir);
        jmethodID getPath = env->GetMethodID(fileClass, "getAbsolutePath", "()Ljava/lang/String;");
        jstring pathStr = (jstring)env->CallObjectMethod(filesDir, getPath);
        const char* dirPath = env->GetStringUTFChars(pathStr, nullptr);

        const char* filenameCStr = env->GetStringUTFChars(filename, nullptr);
        std::string outputPath = std::string(dirPath) + "/" + filenameCStr;
        env->ReleaseStringUTFChars(filename, filenameCStr);

        __android_log_print(ANDROID_LOG_INFO, "Compression", "Output path: %s", outputPath.c_str());

        env->ReleaseStringUTFChars(pathStr, dirPath);
        env->DeleteLocalRef(pathStr);
        /////
        CompressionSettings settings;
        if(method == 1) {
            __android_log_print(ANDROID_LOG_INFO, "Compression", "Using JPEG compression");

            settings = JPEGSettings;
            settings.QuantizationFactor = quality;
            compression("", outputPath.data(), imIn, settings); //first argument not used

        } else {
            __android_log_print(ANDROID_LOG_INFO, "Compression", "Using JPEG2000 compression");

            settings = JPEG2000Settings;
            settings.QuantizationFactor = quality;
            compression2000("", outputPath.data(), imIn, settings);

        }

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
        //////

        ImageBase imOut(width, height, true);
        if(method == 1) {
            __android_log_print(ANDROID_LOG_INFO, "Compression", "Starting JPEG decompression");
            decompression(outputPath.data(), "", imOut, settings);
        } else {
            __android_log_print(ANDROID_LOG_INFO, "Compression", "Starting JPEG2000 decompression");
            decompression2000(outputPath.data() , "", imOut, settings);
        }



        //On recardre l'image à la taille d'origine
        ImageBase imCropped(oldWidth, oldHeight, true);
        for (int y = 0; y < oldHeight; ++y) {
            for (int x = 0; x < oldWidth; ++x) {
                for (int c = 0; c < 3; ++c) {
                    imCropped.data[(y * oldWidth + x) * 3 + c] = imOut.data[(y * width + x) * 3 + c];
                }
            }
        }

        width = imCropped.getWidth();
        height = imCropped.getHeight();


        //on convertit en tableau d'octets
        int dataLength = width * height * 3;
        jbyteArray rgbArray = env->NewByteArray(dataLength);
        env->SetByteArrayRegion(rgbArray, 0, dataLength, reinterpret_cast<jbyte *>(imCropped.data));

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

extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_compresseur_1mobile_CompressionFragment_decompressImageNative(
        JNIEnv *env,
        jobject thiz,
        jobject context,
        jstring filename) {

            const char* filenameCStr = env->GetStringUTFChars(filename, nullptr);

            jclass contextClass = env->GetObjectClass(context);
            jmethodID getFilesDir = env->GetMethodID(contextClass, "getFilesDir", "()Ljava/io/File;");
            jobject filesDir = env->CallObjectMethod(context, getFilesDir);

            jclass fileClass = env->GetObjectClass(filesDir);
            jmethodID getPath = env->GetMethodID(fileClass, "getAbsolutePath", "()Ljava/lang/String;");
            jstring pathStr = (jstring)env->CallObjectMethod(filesDir, getPath);
            const char* dirPath = env->GetStringUTFChars(pathStr, nullptr);

            std::string inputPath = filenameCStr;

            env->ReleaseStringUTFChars(filename, filenameCStr);
            env->ReleaseStringUTFChars(pathStr, dirPath);
            env->DeleteLocalRef(pathStr);
            env->DeleteLocalRef(fileClass);

            CompressionSettings settings;
            int width, height;

            readSettings(inputPath, settings, &width, &height);

            ImageBase imOut(width, height, true);

            if (settings.transformationType == DCTTRANSFORM) {
                decompression(inputPath.data(), "", imOut, settings);
            } else {
                decompression2000(inputPath.data(), "", imOut, settings);
            }

            int dataLength = width * height * 3;
            jbyteArray rgbArray = env->NewByteArray(dataLength);
            env->SetByteArrayRegion(rgbArray, 0, dataLength, reinterpret_cast<jbyte *>(imOut.data));

            jclass resultClass = env->FindClass("com/example/compresseur_mobile/CompressionResult");
            if (resultClass == nullptr) {
                return nullptr;
            }
            jobject resultObj = env->AllocObject(resultClass);
            if (resultObj == nullptr) {
                return nullptr;
            }

            jfieldID compressedImageField = env->GetFieldID(resultClass, "compressedImage", "[B");
            jfieldID widthField = env->GetFieldID(resultClass, "width", "I");
            jfieldID heightField = env->GetFieldID(resultClass, "height", "I");
            jfieldID qualityField = env->GetFieldID(resultClass, "quality", "I");
            jfieldID methodField = env->GetFieldID(resultClass, "method", "I");

            env->SetObjectField(resultObj, compressedImageField, rgbArray);
            env->SetIntField(resultObj, widthField, width);
            env->SetIntField(resultObj, heightField, height);
            env->SetIntField(resultObj, qualityField, settings.QuantizationFactor);
            env->SetIntField(resultObj, methodField, settings.transformationType == DCTTRANSFORM ? 1 : 2);

            imOut.reset();

            return resultObj;
}





