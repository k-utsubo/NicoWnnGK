

cmake_minimum_required(VERSION 3.4.1)
include_directories(../libwnnDictionary/include)
add_library(
             nicoWnnGJpnDic
             SHARED
             WnnJpnDic.c)


find_library(
              log-lib
              log )


target_link_libraries(
                       nicoWnnGJpnDic
                       ${log-lib} )