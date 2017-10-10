
cmake_minimum_required(VERSION 3.4.1)
include_directories(../libwnnDictionary/include)
add_library(
             nicoWnnGEngDic
             SHARED
             WnnEngDic.c)


find_library(
              log-lib
              log )


target_link_libraries(
                       nicoWnnGEngDic
                       ${log-lib} )