#include "JPEG.h"
#include "ImageBase.h"
#include <thread>
#include <vector>

#include "Utils.h"
#include "FormatSamplingBlur.h"
#include "TransformationQuantification.h"
#include "RLE.h"
#include "Huffman.h"

#include <iostream>

#include <android/log.h>
#include <thread>

//pour debug 
std::vector<std::pair<int,int>> rlecompression;

std::vector<std::pair<int,int>> rledecompression;




//peut mener a des erreurs d'acces memoire si la taille de l'image n'est pas un multiple de blockSize
//problème reglé cette fonction marche peu importe la taille
std::vector<Block> getBlocks(ImageBase & imIn, int blockSize) {
    std::vector<Block> blocks;
    int height = imIn.getHeight();
    int width = imIn.getWidth();


    for (int i = 0; i < height; i += blockSize) {
        for (int j = 0; j < width; j += blockSize) {
            Block block(blockSize);

            for (int k = 0; k < blockSize; k++) {
                for (int l = 0; l < blockSize; l++) {

                    int x = i + k;
                    int y = j + l;

                    if (x < height && y < width) {
                        block.data[k][l] = imIn[x][y];
                    } else {
                        block.data[k][l] = 0;  // Remplissage avec 0
                    }
                }
            }

            blocks.push_back(block);
            
        }
    }

    return blocks;
}



//reconstruction de l'image en niveau de gris à partir des blocs 
void reconstructImage(std::vector<Block> & blocks, ImageBase & imIn, int blocksize){
    int height = imIn.getHeight();
    int width = imIn.getWidth();
    //float bls = 1.0/(float)blocksize;

    //__android_log_print(ANDROID_LOG_INFO, "Compression", "Reconstructing image of size %dx%d with block size %d", width, height, blocksize);

    for(int i = 0; i < height; i += blocksize){
        for(int j = 0; j < width; j += blocksize){
            //__android_log_print(ANDROID_LOG_INFO, "Compression", "Processing block starting at (%d, %d)", i, j);

            //Block block = blocks[i * bls * (width * bls) + j * bls];
            int blocksPerRow = width / blocksize;
            int blockIndex = (i / blocksize) * blocksPerRow + (j / blocksize);
            if (blockIndex >= blocks.size()) {
                __android_log_print(ANDROID_LOG_ERROR, "Compression", "reconstructing oob: %d >= %lu", blockIndex, blocks.size());
                return;
            }
            Block block = blocks[blockIndex];
            //__android_log_print(ANDROID_LOG_INFO, "Compression", "Accessing block at index: %d", blockIndex);

            for(int k = 0; k < blocksize; k++){
                for(int l = 0; l < blocksize; l++){
                    if (block.data[k][l] < 0) { //la valeur peut être négative on la seuille pour éviter des erreurs lors du cast en uchar dans l'image
                        block.data[k][l] = 0;
                        //std::cout << " aie "<< std::endl;
                    } else if (block.data[k][l] > 255) {
                        block.data[k][l] = 255;
                        //std::cout << "houla" << std::endl;
                    }
                    //__android_log_print(ANDROID_LOG_INFO, "Compression", "Setting pixel at (%d, %d) to value %d", i + k, j + l, block.data[k][l]);

                    if ((i + k) >= height || (j + l) >= width) {
                        __android_log_print(ANDROID_LOG_WARN, "Compression", "Pixel coordinate (%d, %d) out of bounds (max: %d, %d)", i + k, j + l, height - 1, width - 1);
                    }

                    imIn[i + k][j + l] = block.data[k][l];
                }
            }

        }
    }

    __android_log_print(ANDROID_LOG_INFO, "Compression", "reconstruction finished");

}









//compresse l'image a la manière de JPEG

void compression( char * cNomImgLue,  char * cNomImgOut, ImageBase & imIn, CompressionSettings & settings){

    std::vector<std::thread> threads;

    printf("Opening image : %s\n", cNomImgLue);

    //imIn.load(cNomImgLue);
    __android_log_print(ANDROID_LOG_INFO, "Compression", "Compression start");

    int pixel = imIn[0][0];
    __android_log_print(ANDROID_LOG_INFO, "Compression", "first pixel %d",pixel);
    //Transformation des couleurs
    __android_log_print(ANDROID_LOG_INFO, "Compression", "Transformation de l'espace couleur");
    ImageBase imY(imIn.getWidth(), imIn.getHeight(), false);
    ImageBase imCb(imIn.getWidth(), imIn.getHeight(), false);
    ImageBase imCr(imIn.getWidth(), imIn.getHeight(), false);

    RGB_to_YCbCr(imIn, imY, imCb, imCr);

    //imY.save("./img/out/Y.pgm");
    //imCb.save("./img/out/Cb.pgm");
    //imCr.save("./img/out/Cr.pgm");

    __android_log_print(ANDROID_LOG_INFO, "Compression", "  Fini");
    //Sous échantillonage
    __android_log_print(ANDROID_LOG_INFO, "Compression", "Sous échantillonage de Cb et Cr");

    __android_log_print(ANDROID_LOG_INFO, "Compression", "Flou Gaussien sur Cr et Cb");
    ImageBase imCbFlou(imIn.getWidth(), imIn.getHeight(), false);
    ImageBase imCrFlou(imIn.getWidth(), imIn.getHeight(), false);

    gaussianBlur(imCb,imCbFlou); //fonction dans Utils.h
    gaussianBlur(imCr,imCrFlou);


    ImageBase downSampledCb(imIn.getWidth() / 2, imIn.getHeight() /2, false);
    ImageBase downSampledCr(imIn.getWidth() / 2, imIn.getHeight() /2, false);


    down_sampling_bilinear(imCbFlou, downSampledCb);
    down_sampling_bilinear(imCrFlou, downSampledCr);

    // downSampledCb.save("./img/out/downSampledCb.pgm");
    //downSampledCr.save("./img/out/downSampledCr.pgm");

    __android_log_print(ANDROID_LOG_INFO, "Compression", "  Fini");

    //Découpage en blocs de pixel
    __android_log_print(ANDROID_LOG_INFO, "Compression", "Découpage en blocs de pixel");
    std::vector<Block> blocksY = getBlocks(imY, 8);
    std::vector<Block> blocksCb = getBlocks(downSampledCb, 8);
    std::vector<Block> blocksCr = getBlocks(downSampledCr, 8);

    __android_log_print(ANDROID_LOG_INFO, "Compression", "number of blocks for Y channel: %d", (int)blocksY.size());
    __android_log_print(ANDROID_LOG_INFO, "Compression", "number of blocks for Cb channel: %d", (int)blocksCb.size());
    __android_log_print(ANDROID_LOG_INFO, "Compression", "number of blocks for Cr channel: %d", (int)blocksCr.size());

    __android_log_print(ANDROID_LOG_INFO, "Compression", "  Fini");

    __android_log_print(ANDROID_LOG_INFO, "Compression", "DCT et quantification ");
    // pour chaque bloc : on fait la DCT, on quantifie le résultat de la DCT et on applatit la matrice de DCT

    //On peut surement utiliser encore plus de threads

    threads.emplace_back([&blocksY, &settings] {
        for(Block & block : blocksY){
            DCT(block);
            //quantification(block);
            quantification_better(block,quantificationLuminance , settings.QuantizationFactor);
            flattenZigZag(block);
        }
    });

    threads.emplace_back([&blocksCb, &settings] {
        for(Block & block : blocksCb){
            DCT(block);
            //quantification(block);
            quantification_better(block,quantificationChrominance, settings.QuantizationFactor);
            flattenZigZag(block);
        }
    });

    threads.emplace_back([&blocksCr, &settings] {
        for(Block & block : blocksCr){
            DCT(block);
            //quantification(block);
            quantification_better(block,quantificationChrominance, settings.QuantizationFactor);
            flattenZigZag(block);
        }
    });

    for (auto &thread : threads) {
        thread.join();
    }

    threads.clear();

    __android_log_print(ANDROID_LOG_INFO, "Compression", "  Fini");
    __android_log_print(ANDROID_LOG_INFO, "Compression", "Codage RLE");

    std::vector<std::pair<int,int>> blocksYRLE; //les blocs applatis et encodés en RLE
    std::vector<std::pair<int,int>> blocksCbRLE;
    std::vector<std::pair<int,int>> blocksCrRLE;

    // pour chaque bloc : on compresse en RLE la matrice applati

    std::mutex mutexY, mutexCb, mutexCr;

    threads.emplace_back([&blocksY, &blocksYRLE, &mutexY] {
        for(Block & block : blocksY){
            std::vector<std::pair<int,int>> RLEBlock;

            RLECompression(block.flatDctMatrix,RLEBlock);
            std::lock_guard<std::mutex> lock(mutexY);
            blocksYRLE.insert(blocksYRLE.end(), RLEBlock.begin(), RLEBlock.end());
        }
    });

    threads.emplace_back([&blocksCb, &blocksCbRLE, &mutexCb] {
        for(Block & block : blocksCb){
            std::vector<std::pair<int,int>> RLEBlock;

            RLECompression(block.flatDctMatrix,RLEBlock);
            std::lock_guard<std::mutex> lock(mutexCb);
            blocksCbRLE.insert(blocksCbRLE.end(), RLEBlock.begin(), RLEBlock.end());
        }
    });

    threads.emplace_back([&blocksCr, &blocksCrRLE, &mutexCr] {
        for(Block & block : blocksCr){
            std::vector<std::pair<int,int>> RLEBlock;

            RLECompression(block.flatDctMatrix,RLEBlock);
            std::lock_guard<std::mutex> lock(mutexCr);
            blocksCrRLE.insert(blocksCrRLE.end(), RLEBlock.begin(), RLEBlock.end());
        }
    });

    for (auto &thread : threads) {
        thread.join();
    }

    __android_log_print(ANDROID_LOG_INFO, "Compression", "size blocksRLE %zu %zu %zu", blocksYRLE.size(), blocksCbRLE.size(), blocksCrRLE.size());


    std::vector<std::pair<int, int>> allBlocksRLE; //on fusionne les 3 canaux
    allBlocksRLE.insert(allBlocksRLE.end(), blocksYRLE.begin(), blocksYRLE.end());
    allBlocksRLE.insert(allBlocksRLE.end(), blocksCbRLE.begin(), blocksCbRLE.end());
    allBlocksRLE.insert(allBlocksRLE.end(), blocksCrRLE.begin(), blocksCrRLE.end());


    __android_log_print(ANDROID_LOG_INFO, "Compression", "  Fini");
    __android_log_print(ANDROID_LOG_INFO, "Compression", "Huffman encoding ");

    std::vector<huffmanCodeSingle> codeTable;

    //on cree la table de codage
    HuffmanEncoding(allBlocksRLE, codeTable);

    __android_log_print(ANDROID_LOG_INFO, "Compression", "Code table size: %lu", codeTable.size());
    __android_log_print(ANDROID_LOG_INFO, "Compression", "  Fini");

    std::string outFileName = cNomImgOut;

    //on ecrit le fichier huffman encodé
    writeHuffmanEncoded(allBlocksRLE, codeTable,
                        imIn.getWidth(), imIn.getHeight(), downSampledCb.getWidth(), downSampledCb.getHeight() ,
                        blocksYRLE.size(), blocksCbRLE.size(),blocksCrRLE.size(),
                        outFileName, settings);

    __android_log_print(ANDROID_LOG_INFO, "Compression", "  File written to %s", outFileName.c_str());

}




void decompression(const char * cNomImgIn, const char * cNomImgOut, ImageBase & imOut, CompressionSettings & settings){
    
    //printf("Decompression\n");

    std::string outFileName = cNomImgIn;
    std::vector<huffmanCodeSingle> codeTable;
    std::vector<std::pair<int, int>> BlocksRLEEncoded;

    int imageWidth, imageHeight;
    int downSampledWidth, downSampledHeight;
    int channelYRLESize, channelCbRLESize, channelCrRLESize;

    std::vector<std::thread> threads;

    int maxThreads = std::thread::hardware_concurrency();

    __android_log_print(ANDROID_LOG_INFO, "Decompression", "Reading huffman encoded file");

    readHuffmanEncoded(outFileName,
                        codeTable, BlocksRLEEncoded,
                        imageWidth, imageHeight, downSampledWidth, downSampledHeight,
                        channelYRLESize, channelCbRLESize, channelCrRLESize, settings);

    __android_log_print(ANDROID_LOG_INFO, "Decompression", "size downSampledWidth %d %d", downSampledWidth, downSampledHeight);

    __android_log_print(ANDROID_LOG_INFO, "Decompression", "Code table size: %lu", codeTable.size());

    std::vector<std::pair<int,int>> blocksYRLE; //les blocs applatis et encodés en RLE
    std::vector<std::pair<int,int>> blocksCbRLE;
    std::vector<std::pair<int,int>> blocksCrRLE;

    std::vector<Block> blocksY;
    std::vector<Block> blocksCb;
    std::vector<Block> blocksCr;

    ImageBase imY(imageWidth, imageHeight, false);

    ImageBase imCb(downSampledWidth, downSampledHeight, false);
    ImageBase imCr(downSampledWidth, downSampledHeight, false);

    ImageBase upSampledCb(imageWidth, imageHeight, false);
    ImageBase upSampledCr(imageWidth, imageHeight, false);

    //on sépare les 3 canaux
    threads.emplace_back([&BlocksRLEEncoded, &blocksYRLE, channelYRLESize, &blocksY, &imY, &settings] {
        for (int i = 0; i < channelYRLESize; i++) {
            blocksYRLE.push_back(BlocksRLEEncoded[i]);
        }

        decompressBlocksRLE(blocksYRLE, blocksY, quantificationLuminance, &settings);
        __android_log_print(ANDROID_LOG_INFO, "Decompression", "blocksY size: %lu", blocksY.size());

        __android_log_print(ANDROID_LOG_INFO, "Decompression", "Reconstructing Y channel");
        reconstructImage(blocksY, imY, 8);
        __android_log_print(ANDROID_LOG_INFO, "Decompression", "saving Y channel");
        //imY.save("./img/out/Y_decompressed.pgm");
    });

    threads.emplace_back([&BlocksRLEEncoded, &blocksCbRLE, channelYRLESize, channelCbRLESize, &blocksCb, &imCb, &upSampledCb, &settings] {
        for (int i = channelYRLESize; i < channelYRLESize + channelCbRLESize; i++) {
            blocksCbRLE.push_back(BlocksRLEEncoded[i]);
        }

        decompressBlocksRLE(blocksCbRLE, blocksCb, quantificationChrominance, &settings);
        __android_log_print(ANDROID_LOG_INFO, "Decompression", "blocksCb size: %lu", blocksCb.size());
        __android_log_print(ANDROID_LOG_INFO, "Decompression", "Reconstructing Cb channel");
        reconstructImage(blocksCb, imCb, 8);
        __android_log_print(ANDROID_LOG_INFO, "Decompression", "Cb reconstructed upsampling");
        up_sampling(imCb, upSampledCb);
        //upSampledCb.save("./img/out/Cb_decompressed.pgm");
    });

    threads.emplace_back([&BlocksRLEEncoded, &blocksCrRLE, channelYRLESize, channelCbRLESize, channelCrRLESize, &blocksCr, &imCr, &upSampledCr, &settings] {
        for (int i = channelYRLESize + channelCbRLESize; i < channelYRLESize + channelCbRLESize + channelCrRLESize; i++) {
            blocksCrRLE.push_back(BlocksRLEEncoded[i]);
        }

        decompressBlocksRLE(blocksCrRLE, blocksCr, quantificationChrominance, &settings);
        __android_log_print(ANDROID_LOG_INFO, "Decompression", "blocksCr size: %lu", blocksCr.size());
        __android_log_print(ANDROID_LOG_INFO, "Decompression", "Reconstructing Cr channel");
        reconstructImage(blocksCr, imCr, 8);
        __android_log_print(ANDROID_LOG_INFO, "Decompression", "Cr reconstructed upsampling");
        up_sampling(imCr, upSampledCr);
        //upSampledCr.save("./img/out/Cr_decompressed.pgm");
    });


    for (auto &thread : threads) {
        thread.join();
    }
    threads.clear();

    __android_log_print(ANDROID_LOG_INFO, "Decompression", "Blocks decoded");



    __android_log_print(ANDROID_LOG_INFO, "Decompression", "Reconstructing image from YCbCr");
    YCbCr_to_RGB(imY, upSampledCb, upSampledCr, imOut);

    //printf("Saving decompressed image\n");
    std::string cNomImgOutStr = cNomImgOut;
    //(*imOut).save(cNomImgOutStr.data());


}
