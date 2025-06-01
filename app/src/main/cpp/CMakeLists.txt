cmake_minimum_required(VERSION 3.10.2)

project("com_example_compresseur_mobile")

include_directories(include)

add_library(
        native-lib
        SHARED
        src/FormatSamplingBlur.cpp
        src/Huffman.cpp
        src/ImageBase.cpp
        src/JPEG.cpp
        src/JPEG2000.cpp
        src/LZ77.cpp
        src/RLE.cpp
        src/TransformationQuantification.cpp
        src/Utils.cpp
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        native-lib
        ${log-lib})

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -march=armv8-a -ffast-math")


