#pragma once

#include "JPEG2000.h"

#include "ImageBase.h"
#include <vector>
#include <string>

#include "Utils.h"
#include "FormatSamplingBlur.h"
#include "TransformationQuantification.h"
#include "RLE.h"
#include "Huffman.h"

#include <iostream>
#include <thread>
#include <android/log.h>

/*
Avec JPEG2000 on ne fait pas des blocks de 8 par 8 mais des Tiles
Ici on va faire des Tiles de taille 128*128 ou 256*256
*/

std::vector<Tile> getTiles(ImageBase &imIn, int tileWidth, int tileHeight) {
    std::vector<Tile> tiles;
    int height = imIn.getHeight();
    int width = imIn.getWidth();

    // Le nombre de tiles dans chaque direction
    int numTilesX = (width + tileWidth - 1) / tileWidth;  // Arrondir vers le haut
    int numTilesY = (height + tileHeight - 1) / tileHeight;  // Arrondir vers le haut

    // Remplir le vecteur de tiles
    for (int i = 0; i < numTilesY; i++) {
        for (int j = 0; j < numTilesX; j++) {
            // Calculer la largeur et la hauteur du tile en fonction de la position
            int currentTileWidth = std::min(tileWidth, width - j * tileWidth);
            int currentTileHeight = std::min(tileHeight, height - i * tileHeight);

            // Créer un tile
            Tile tile(currentTileWidth, currentTileHeight, j * tileWidth, i * tileHeight);

            // Remplir le tile avec les données de l'image
            for (int k = 0; k < currentTileHeight; k++) {
                for (int l = 0; l < currentTileWidth; l++) {
                    tile.data[k][l] = imIn[i * tileHeight + k][j * tileWidth + l];
                }
            }

            // Ajouter le tile au vecteur
            tiles.push_back(tile);
        }
    }

    return tiles;
}

//==========================================================================================================================
//wavelet transform CDF97 

void applyCDF97(std::vector<std::vector<int>>& data) {
    int height = data.size();
    int width = data[0].size();

    // Application de la transformation CDF 9/7 sur les lignes
    for (int i = 0; i < height; ++i) {

        for (int j = 1; j < width - 1; ++j) {
            int temp1 = data[i][j];
            int temp2 = data[i][j + 1];

            data[i][j] = (data[i][j] + data[i][j - 1]) / 2;
            data[i][j + 1] = temp2 - data[i][j + 1];
        }
    }

    // meme chose sur les colones
    for (int j = 0; j < width; ++j) {
        
        for (int i = 1; i < height - 1; ++i) {
            int temp1 = data[i][j];
            int temp2 = data[i + 1][j];

            
            data[i][j] = (data[i][j] + data[i - 1][j]) / 2;
            data[i + 1][j] = temp2 - data[i + 1][j];
        }
    }
}

void applyWaveletTransformToTiles(std::vector<Tile>& tiles) {
    for (auto& tile : tiles) {
        // Appliquer la transformation CDF 9/7 à chaque tile
        applyCDF97(tile.data);
    }
}


void inverseCDF97(std::vector<std::vector<int>>& data) {
    int height = data.size();
    int width = data[0].size();

    // Inverse de la transformation sur les colonnes
    for (int j = 0; j < width; ++j) {
        for (int i = height - 2; i >= 1; --i) {
            data[i + 1][j] = data[i + 1][j] + data[i][j];  // Restaurer la valeur
            data[i][j] = 2 * data[i][j] - data[i - 1][j];  // Annuler l'opération moyenne
        }
    }

    // Inverse de la transformation sur les lignes
    for (int i = 0; i < height; ++i) {
        for (int j = width - 2; j >= 1; --j) {
            data[i][j + 1] = data[i][j + 1] + data[i][j];  // Restaurer la valeur
            data[i][j] = 2 * data[i][j] - data[i][j - 1];  // Annuler l'opération moyenne
        }
    }
}

void inverseWaveletTransformToTiles(std::vector<Tile>& tiles) {
    for (auto& tile : tiles) {
        inverseCDF97(tile.data);
    }
}
//==========================================================================================================================



//==========================================================================================================================
//bout de code pratique

std::vector<int> getFlatTile(Tile & tile) {
    std::vector<int> res;
    int width = tile.width;
    int height = tile.height;
    for (int i = 0; i < height; i++) { // Parcourir les lignes (height)
        for (int j = 0; j < width; j++) { // Parcourir les colonnes (width)
            res.push_back(tile.data[i][j]);
        }
    }
    return res;

    /* std::vector<int> res;
    int width = tile.width;
    int height = tile.height;

    int halfWidth = width / 2;
    int halfHeight = height / 2;

    // LL

    for (int i = 0; i < halfHeight; i++) {
        for (int j = 0; j < halfWidth; j++) {
            res.push_back(tile.data[i][j]);
        }
    }

    // LH
    for (int i = 0; i < halfHeight; i++) {
        for (int j = halfWidth; j < width; j++) {
            res.push_back(tile.data[i][j]);
        }
    }

    // HL
    for (int i = halfHeight; i < height; i++) {
        for (int j = 0; j < halfWidth; j++) {
            res.push_back(tile.data[i][j]);
        }
    }

    // HH
    for (int i = halfHeight; i < height; i++) {
        for (int j = halfWidth; j < width; j++) {
            res.push_back(tile.data[i][j]);
        }
    }

    return res; */
}

//==========================================================================================================================

void decompressTilesRLE(const std::vector<std::pair<int, int>>& tilesYRLE, std::vector<Tile>& tilesY, int tileWidth, int tileHeight) {
    std::vector<int> decompressedData;
    
    // Décompression des données RLE en un tableau linéaire
    for (const auto& pair : tilesYRLE) {
        int count = pair.first;
        int value = pair.second;
        decompressedData.insert(decompressedData.end(), count, value);
    }
    
    // Vérification de la taille des données
    int numTiles = decompressedData.size() / (tileWidth * tileHeight);
    if (numTiles * tileWidth * tileHeight != decompressedData.size()) {
        std::cerr << "Erreur: Données RLE mal formées ou incorrectes." << std::endl;
        return;
    }
    
    // Remplissage des Tiles
    tilesY.clear();
    int index = 0;
    for (int t = 0; t < numTiles; ++t) {
        Tile tile(tileWidth, tileHeight, 0, 0);
        for (int i = 0; i < tileHeight; ++i) {
            for (int j = 0; j < tileWidth; ++j) {
                tile.data[i][j] = decompressedData[index++];
            }
        }
        tilesY.push_back(tile);
    }

    /* int halfWidth = tileWidth / 2;
    int halfHeight = tileHeight / 2;

    tilesY.clear();
    int index = 0;
    for(int t = 0; t < numTiles; ++t) {
        Tile tile(tileWidth, tileHeight, 0, 0);
        
        for (int i = 0; i < halfHeight; i++) {
            for (int j = 0; j < halfWidth; j++) {
                tile.data[i][j] = decompressedData[index++];
            }
        }
        // LH quadrant (top-right)
        for (int i = 0; i < halfHeight; i++) {
            for (int j = halfWidth; j < tileWidth; j++) {
                tile.data[i][j] = decompressedData[index++];
            }
        }
        // HL quadrant (bottom-left)
        for (int i = halfHeight; i < tileHeight; i++) {
            for (int j = 0; j < halfWidth; j++) {
                tile.data[i][j] = decompressedData[index++];
            }
        }
        // HH quadrant (bottom-right)
        for (int i = halfHeight; i < tileHeight; i++) {
            for (int j = halfWidth; j < tileWidth; j++) {
                tile.data[i][j] = decompressedData[index++];
            }
        }

        tilesY.push_back(tile);
    } */


}

//==========================================================================================================================

void reconstructImage(std::vector<Tile> & tiles, ImageBase & imIn, int tileWidth, int tileHeight) {
    int height = imIn.getHeight();
    int width = imIn.getWidth();
    
    int numTilesX = width / tileWidth;
    int numTilesY = height / tileHeight;

    if (tiles.size() != numTilesX * numTilesY) {
        std::cerr << "Erreur : Nombre de tiles incorrect (" << tiles.size() << " au lieu de " << numTilesX * numTilesY << ")" << std::endl;
        return;
    }

    for (int i = 0; i < height; i += tileHeight) {  // Correction ici
        for (int j = 0; j < width; j += tileWidth) {  // Correction ici

            int tileX = j / tileWidth;
            int tileY = i / tileHeight;
            int tileIndex = tileY * numTilesX + tileX;

            if (tileIndex >= tiles.size()) {
                std::cerr << "Erreur : tileIndex hors limites (" << tileIndex << ")" << std::endl;
                continue;
            }

            Tile& tile = tiles[tileIndex];

            for (int k = 0; k < tileHeight; k++) {  // Correction ici
                for (int l = 0; l < tileWidth; l++) {  // Correction ici
                    int pixelX = j + l;
                    int pixelY = i + k;

                    // Vérifier que l'on reste dans les limites de l'image
                    if (pixelX >= width || pixelY >= height) continue;

                    int value = tile.data[k][l];
                    value = std::max(0, std::min(255, value)); // Clamp entre 0 et 255

                    imIn[pixelY][pixelX] = value;
                }
            }
        }
    }
}


//==========================================================================================================================
//==========================================================================================================================
//fonction a adapter

void compression2000( char * cNomImgLue,  char * cNomImgOut, ImageBase & imIn, CompressionSettings & settings){

    int width = imIn.getWidth();
    int height = imIn.getHeight();

    //Transformation des couleurs
    ImageBase luminance(imIn.getWidth(), imIn.getHeight(), false);
    ImageBase colorChannel1(imIn.getWidth(), imIn.getHeight(), false);
    ImageBase colorChannel2(imIn.getWidth(), imIn.getHeight(),false);

    RGB_to_YCbCr(imIn, luminance, colorChannel1, colorChannel2);

    ImageBase imColorChannel1Flou(imIn.getWidth(), imIn.getHeight(), false);
    ImageBase imColorChannel2Flou(imIn.getWidth(), imIn.getHeight(), false);

    gaussianBlur(colorChannel1, imColorChannel1Flou);
    gaussianBlur(colorChannel2, imColorChannel2Flou);

    ImageBase downSampledColor1(imIn.getWidth() / 2, imIn.getHeight() /2, false);
    ImageBase downSampledColor2(imIn.getWidth() / 2, imIn.getHeight() /2, false);

    down_sampling_bilinear(imColorChannel1Flou, downSampledColor1);
    down_sampling_bilinear(imColorChannel2Flou, downSampledColor2);



    printf("  Fini\n");

    // Jusqu'ici rien n'a changé ======================================

    //Découpage en tiles de tiles
    printf("Découpage en tiles de pixel \n");

    int tileWidth = settings.tileWidth;
    int tileHeight = settings.tileHeight;

    std::vector<Tile> tilesY = getTiles(luminance, tileWidth,tileHeight);
    std::vector<Tile> tilesCb = getTiles(downSampledColor1, tileWidth,tileHeight);
    std::vector<Tile> tilesCr = getTiles(downSampledColor2, tileWidth,tileHeight);

    printf("number of tiles for Y channel: %d\n", tilesY.size());
    printf("number of tiles for Cb channel: %d\n", tilesCb.size());
    printf("number of tiles for Cr channel: %d\n", tilesCr.size());


    printf("Wavelet transform et quantification : \n"); // wavelet

    applyWaveletTransform53ToTiles(tilesY);

    applyWaveletTransform53ToTiles(tilesCr);

    applyWaveletTransform53ToTiles(tilesCb);

    std::pair<int,int> steps = getQuantificationStep(settings.QuantizationFactor);
    int stepLow = steps.first;
    int stepHigh = steps.second;

    for (auto& tile : tilesY) {
        quantificationuniforme(tile, stepLow,stepHigh);
    }

    for (auto& tile : tilesCb) {
        quantificationuniforme(tile, stepLow,stepHigh);
    }

    for (auto& tile : tilesCr) {
        quantificationuniforme(tile, stepLow,stepHigh);
    }

    std::vector<int> flatDataY;
    std::vector<int> flatDataCb;
    std::vector<int> flatDataCr;

    std::vector<LZ77Triplet> blocksLuminanceLZ77; //les blocs applatis et encodés en LZ77
    std::vector<LZ77Triplet> blocksColor1LZ77;
    std::vector<LZ77Triplet> blocksColor2LZ77;

    for(Tile & tile : tilesY){
        std::vector<int> flatTile = getFlatTile(tile);
        flatDataY.insert(flatDataY.end(), flatTile.begin(), flatTile.end());
    }

    for(Tile & tile : tilesCb){
        std::vector<int> flatTile = getFlatTile(tile);
        flatDataCb.insert(flatDataCb.end(), flatTile.begin(), flatTile.end());
    }

    for(Tile & tile : tilesCr){
        std::vector<int> flatTile = getFlatTile(tile);
        flatDataCr.insert(flatDataCr.end(), flatTile.begin(), flatTile.end());
    }

    LZ77Compression(flatDataY,blocksLuminanceLZ77, settings.encodingWindowSize);
    LZ77Compression(flatDataCb,blocksColor1LZ77, settings.encodingWindowSize);
    LZ77Compression(flatDataCr,blocksColor2LZ77, settings.encodingWindowSize);

    std::vector<LZ77Triplet> allBlocksLZ77; //on fusionne les 3 canaux
    allBlocksLZ77.insert(allBlocksLZ77.end(), blocksLuminanceLZ77.begin(), blocksLuminanceLZ77.end());
    allBlocksLZ77.insert(allBlocksLZ77.end(), blocksColor1LZ77.begin(), blocksColor1LZ77.end());
    allBlocksLZ77.insert(allBlocksLZ77.end(), blocksColor2LZ77.begin(), blocksColor2LZ77.end());

    std::vector<huffmanCodeSingleLZ77> codeTableLZ77;
    HuffmanEncodingLZ77(allBlocksLZ77, codeTableLZ77);

    writeHuffmanEncodedLZ77(allBlocksLZ77, codeTableLZ77,
                            imIn.getWidth(), imIn.getHeight(), downSampledColor1.getWidth(),downSampledColor2.getHeight(),
                            blocksLuminanceLZ77.size(), blocksColor1LZ77.size(), blocksColor2LZ77.size(),
                            cNomImgOut, settings);

    __android_log_print(ANDROID_LOG_INFO, "compression2000", "writeHuffmanEncodedLZ77: width=%d, height=%d, downSampledColor1Width=%d, downSampledColor2Height=%d, blocksLuminanceLZ77.size=%zu, blocksColor1LZ77.size=%zu, blocksColor2LZ77.size=%zu, cNomImgOut=%s",
            imIn.getWidth(), imIn.getHeight(), downSampledColor1.getWidth(), downSampledColor2.getHeight(),
            blocksLuminanceLZ77.size(), blocksColor1LZ77.size(), blocksColor2LZ77.size(), cNomImgOut);

}


//==========================================================================================================================
//==========================================================================================================================


void decompression2000(const char * cNomImgIn, const char * cNomImgOut, ImageBase & imOut, CompressionSettings & settings){

    std::vector<huffmanCodeSingleLZ77> codeTableLZ77;
    std::vector<LZ77Triplet> BlocksLZ77Encoded;
    int channelYLZ77Size, channelCbLZ77Size, channelCrLZ77Size;

    int imageWidth, imageHeight;
    int downSampledWidth, downSampledHeight;

    std::vector<std::thread> threads;

    __android_log_print(ANDROID_LOG_INFO, "Decompression2000", "cNomImgIn: %s", cNomImgIn);

    readSettings(cNomImgIn, settings);
    //settings.printSettings();

    __android_log_print(ANDROID_LOG_INFO, "Decompression2000", "Settings: tileWidth=%d, tileHeight=%d, QuantizationFactor=%d, encodingWindowSize=%d", settings.tileWidth, settings.tileHeight, settings.QuantizationFactor, settings.encodingWindowSize);
    __android_log_print(ANDROID_LOG_INFO, "Decompression2000", "imOut dimensions: width=%d, height=%d", imOut.getWidth(), imOut.getHeight());

    readHuffmanEncodedLZ77(cNomImgIn,
                           codeTableLZ77, BlocksLZ77Encoded,
                           imageWidth, imageHeight, downSampledWidth, downSampledHeight,
                           channelYLZ77Size, channelCbLZ77Size, channelCrLZ77Size, settings);

    __android_log_print(ANDROID_LOG_INFO, "Decompression2000", "readHuffmanEncodedLZ77: imageWidth=%d, imageHeight=%d, downSampledWidth=%d, downSampledHeight=%d, channelYLZ77Size=%d, channelCbLZ77Size=%d, channelCrLZ77Size=%d", imageWidth, imageHeight, downSampledWidth, downSampledHeight, channelYLZ77Size, channelCbLZ77Size, channelCrLZ77Size);

    int numTilesX = (imageWidth + settings.tileWidth - 1) / settings.tileWidth;
    int numTilesY = (imageHeight + settings.tileHeight - 1) / settings.tileHeight;
    int totalTiles = numTilesX * numTilesY;

    //imOut = ImageBase(imageWidth, imageHeight, true);

    ImageBase imY(imageWidth, imageHeight, false);
    ImageBase imCb(downSampledWidth, downSampledHeight, false);
    ImageBase imCr(downSampledWidth, downSampledHeight, false);

    ImageBase upSampledCb(imageWidth, imageHeight, false);
    ImageBase upSampledCr(imageWidth, imageHeight, false);

    std::vector<LZ77Triplet> blocksYLZ77;
    std::vector<LZ77Triplet> blocksCbLZ77;
    std::vector<LZ77Triplet> blocksCrLZ77;

    std::vector<Tile> tilesY;
    std::vector<Tile> tilesCb;
    std::vector<Tile> tilesCr;


    for (int i = 0; i < channelYLZ77Size; i++) {
        blocksYLZ77.push_back(BlocksLZ77Encoded[i]);
    }

    for (int i = channelYLZ77Size; i < channelYLZ77Size + channelCbLZ77Size; i++) {
        blocksCbLZ77.push_back(BlocksLZ77Encoded[i]);
    }

    for (int i = channelYLZ77Size + channelCbLZ77Size; i < channelYLZ77Size + channelCbLZ77Size + channelCrLZ77Size; i++) {
        blocksCrLZ77.push_back(BlocksLZ77Encoded[i]);
    }

    decompressTilesLZ77(blocksYLZ77, tilesY, settings.tileWidth, settings.tileHeight, totalTiles);
    decompressTilesLZ77(blocksCbLZ77, tilesCb, settings.tileWidth, settings.tileHeight, totalTiles / 4);
    decompressTilesLZ77(blocksCrLZ77, tilesCr, settings.tileWidth, settings.tileHeight, totalTiles / 4);

    std::pair<int, int> steps = getQuantificationStep(settings.QuantizationFactor);
    int stepLow = steps.first;
    int stepHigh = steps.second;

    std::cout << "Quantification steps: Low = " << stepLow << ", High = " << stepHigh << std::endl;

    for (auto& tile : tilesCb) {
        inverseQuantificationuniforme(tile,stepLow,stepHigh);
    }
    for (auto& tile : tilesCr) {
        inverseQuantificationuniforme(tile,stepLow,stepHigh);
    }
    for (auto& tile : tilesY) {
        inverseQuantificationuniforme(tile,stepLow,stepHigh);
    }

    inverseWaveletTransform53ToTiles(tilesCb);
    inverseWaveletTransform53ToTiles(tilesCr);
    inverseWaveletTransform53ToTiles(tilesY);

    reconstructImage(tilesCb, imCb,settings.tileWidth,settings.tileHeight);
    reconstructImage(tilesCr, imCr,settings.tileWidth,settings.tileHeight);
    reconstructImage(tilesY, imY,settings.tileWidth,settings.tileHeight);

    up_sampling(imCb, upSampledCb);
    up_sampling(imCr, upSampledCr);

    YCbCr_to_RGB(imY, upSampledCb, upSampledCr, imOut);
}