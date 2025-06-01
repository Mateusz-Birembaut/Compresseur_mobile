package com.example.compresseur_mobile;

public class CompressionResult {
    public byte[] compressedImage;
    public int quality;
    public int method;
    public int width;
    public int height;

    public long oldSize;
    public long newSize;
    public float compressionRatio;
    public float psnr;
}
