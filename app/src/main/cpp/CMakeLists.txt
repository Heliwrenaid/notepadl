cmake_minimum_required(VERSION 3.10.2)
project(aeslib)

include_directories(include)

add_library(crypto STATIC IMPORTED)
set_target_properties(crypto
        PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libcrypto.a)

add_library(ssl STATIC IMPORTED)
set_target_properties(ssl
        PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libssl.a)

add_library(aeslib SHARED aes.c)
find_library(log-lib log)
target_link_libraries(aeslib crypto ssl ${log-lib})
