cmake_minimum_required(VERSION 3.21)
project(stone LANGUAGES CXX)

if(NOT WIN32)
    message(FATAL_ERROR "Stone targets Windows x64 only")
endif()
if(NOT CMAKE_SIZEOF_VOID_P EQUAL 8)
    message(FATAL_ERROR "Stone targets x64 only; configure with -A x64")
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

set(COLD_JAVA_HOME "$ENV{JAVA_HOME}" CACHE PATH "JDK 17+ providing javac, jni.h and jvmti.h")
if(NOT EXISTS "${COLD_JAVA_HOME}/include/jni.h")
    message(FATAL_ERROR "COLD_JAVA_HOME does not contain include/jni.h: ${COLD_JAVA_HOME}")
endif()

set(COLD_DIST_DIR "${CMAKE_SOURCE_DIR}/dist")

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${COLD_DIST_DIR}")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${COLD_DIST_DIR}")
foreach(config DEBUG RELEASE RELWITHDEBINFO MINSIZEREL)
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_${config} "${COLD_DIST_DIR}")
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_${config} "${COLD_DIST_DIR}")
endforeach()

set(COLD_LIB_DIR "${CMAKE_SOURCE_DIR}/infra/lib")

set(COLD_MCP_JAR "${COLD_LIB_DIR}/minecraft-1.8.9-mcp.jar" CACHE FILEPATH
    "Minecraft 1.8.9 client with MCP names")
if(NOT EXISTS "${COLD_MCP_JAR}")
    message(FATAL_ERROR "missing ${COLD_MCP_JAR}: copy an MCP-named 1.8.9 client jar there "
        "(JadeClient/lib ships one) or pass -DCOLD_MCP_JAR=<path>")
endif()

function(cold_fetch_jar url sha256)
    get_filename_component(name "${url}" NAME)
    file(DOWNLOAD "${url}" "${COLD_LIB_DIR}/${name}" EXPECTED_HASH SHA256=${sha256} TLS_VERIFY ON)
endfunction()

set(COLD_MAVEN "https://repo1.maven.org/maven2")
set(COLD_MOJANG "https://libraries.minecraft.net")
cold_fetch_jar("${COLD_MAVEN}/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar"
    8cadd43ac5eb6d09de05faecca38b917a040bb9139c7edeb4cc81c740b713281)
cold_fetch_jar("${COLD_MAVEN}/org/ow2/asm/asm-tree/9.7.1/asm-tree-9.7.1.jar"
    9929881f59eb6b840e86d54570c77b59ce721d104e6dfd7a40978991c2d3b41f)
cold_fetch_jar("${COLD_MAVEN}/org/luaj/luaj-jse/3.0.1/luaj-jse-3.0.1.jar"
    9b1f0a3e8f68427c6d74c2bf00ae0e6dbfce35994d3001fed4cef6ecda50be55)
cold_fetch_jar("${COLD_MAVEN}/com/google/guava/guava/17.0/guava-17.0.jar"
    8c36a80ea613d0b6b8040a17cf837c5bbe3677bc1b06a058a6c174fdb787ebbc)
cold_fetch_jar("${COLD_MAVEN}/com/google/code/gson/gson/2.2.4/gson-2.2.4.jar"
    c0328cd07ca9e363a5acd00c1cf4afe8cf554bd6d373834981ba05cebec687fb)
cold_fetch_jar("${COLD_MOJANG}/com/mojang/authlib/1.5.21/authlib-1.5.21.jar"
    ce7a9c86ebebf30b89929026f4a0a3c58ce0f4d47937d6dc086ae4fd0c77544a)
cold_fetch_jar("${COLD_MOJANG}/io/netty/netty-all/4.0.23.Final/netty-all-4.0.23.Final.jar"
    50510e9c9874b539b98ef931fe7eeeb002f6b5b1976ea37669b9493426294561)
cold_fetch_jar("${COLD_MOJANG}/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar"
    833e721817f70d1445eec13d8ce5a86e12af70efac5014356302e2bbc68b3fe2)
cold_fetch_jar("${COLD_MOJANG}/org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar"
    034456b132a336b72757325bb7aaf99e075681ee5650d2bea21717bd485de42f)

set(COLD_JAVA "${COLD_JAVA_HOME}/bin/java.exe")
set(COLD_JAVAC "${COLD_JAVA_HOME}/bin/javac.exe")
set(COLD_JAVA_DIR "${CMAKE_CURRENT_BINARY_DIR}/java")

set(COLD_API_JAR "${COLD_JAVA_DIR}/minecraft-api.jar")
add_custom_command(
    OUTPUT "${COLD_API_JAR}"
    COMMAND "${COLD_JAVA}" -cp "${COLD_LIB_DIR}/asm-9.7.1.jar"
        "${CMAKE_SOURCE_DIR}/tools/Widen.java" "${COLD_MCP_JAR}" "${COLD_API_JAR}"
    DEPENDS "${COLD_MCP_JAR}" "${CMAKE_SOURCE_DIR}/tools/Widen.java"
    COMMENT "widening the Minecraft jar"
    VERBATIM
)

set(COLD_MEMORY_LOADER_JAVA "${CMAKE_SOURCE_DIR}/bootstrap/MemoryLoader.java")
set(COLD_MEMORY_LOADER_CLASS "${COLD_JAVA_DIR}/loader/stone/boot/MemoryLoader.class")
add_custom_command(
    OUTPUT "${COLD_MEMORY_LOADER_CLASS}"
    COMMAND "${COLD_JAVAC}" --release 17 -encoding UTF-8 -d "${COLD_JAVA_DIR}/loader"
        "${COLD_MEMORY_LOADER_JAVA}"
    DEPENDS "${COLD_MEMORY_LOADER_JAVA}"
    COMMENT "compiling MemoryLoader"
    VERBATIM
)

set(COLD_COMPILE_JARS
    "${COLD_API_JAR}"
    "${COLD_LIB_DIR}/lwjgl-2.9.4-nightly-20150209.jar"
    "${COLD_LIB_DIR}/lwjgl_util-2.9.4-nightly-20150209.jar"
    "${COLD_LIB_DIR}/asm-9.7.1.jar"
    "${COLD_LIB_DIR}/asm-tree-9.7.1.jar"
    "${COLD_LIB_DIR}/luaj-jse-3.0.1.jar"
    "${COLD_LIB_DIR}/guava-17.0.jar"
    "${COLD_LIB_DIR}/gson-2.2.4.jar"
    "${COLD_LIB_DIR}/authlib-1.5.21.jar"
    "${COLD_LIB_DIR}/netty-all-4.0.23.Final.jar"
)
set(COLD_IMAGE_DIR "${COLD_JAVA_DIR}/image")
set(COLD_IMAGE_ZIP "${COLD_JAVA_DIR}/image.zip")

file(GLOB_RECURSE COLD_CLIENT_SOURCES CONFIGURE_DEPENDS "${CMAKE_SOURCE_DIR}/src/*.java")
set(COLD_CLIENT_ARGS_TEXT
    "--release 17\n-encoding UTF-8\n-cp \"${COLD_COMPILE_JARS}\"\n-d \"${COLD_IMAGE_DIR}\"\n")
foreach(source IN LISTS COLD_CLIENT_SOURCES)
    string(APPEND COLD_CLIENT_ARGS_TEXT "\"${source}\"\n")
endforeach()
set(COLD_CLIENT_ARGS "${COLD_JAVA_DIR}/client.args")
file(CONFIGURE OUTPUT "${COLD_CLIENT_ARGS}" CONTENT "${COLD_CLIENT_ARGS_TEXT}" @ONLY)

file(GLOB_RECURSE COLD_ASSET_FILES CONFIGURE_DEPENDS "${CMAKE_SOURCE_DIR}/assets/*")

add_custom_command(
    OUTPUT "${COLD_IMAGE_ZIP}"
    COMMAND "${CMAKE_COMMAND}" -E rm -rf "${COLD_IMAGE_DIR}"
    COMMAND "${COLD_JAVAC}" "@${COLD_CLIENT_ARGS}"
    COMMAND "${CMAKE_COMMAND}" -E chdir "${COLD_IMAGE_DIR}"
        "${CMAKE_COMMAND}" -E tar xf "${COLD_LIB_DIR}/asm-9.7.1.jar"
    COMMAND "${CMAKE_COMMAND}" -E chdir "${COLD_IMAGE_DIR}"
        "${CMAKE_COMMAND}" -E tar xf "${COLD_LIB_DIR}/asm-tree-9.7.1.jar"
    COMMAND "${CMAKE_COMMAND}" -E chdir "${COLD_IMAGE_DIR}"
        "${CMAKE_COMMAND}" -E tar xf "${COLD_LIB_DIR}/luaj-jse-3.0.1.jar"
    COMMAND "${CMAKE_COMMAND}" -E copy_directory
        "${CMAKE_SOURCE_DIR}/assets" "${COLD_IMAGE_DIR}/assets"
    COMMAND "${CMAKE_COMMAND}" -E chdir "${COLD_IMAGE_DIR}"
        "${CMAKE_COMMAND}" -E tar cf "${COLD_IMAGE_ZIP}" --format=zip stone org assets
    DEPENDS "${COLD_CLIENT_ARGS}" ${COLD_CLIENT_SOURCES} ${COLD_COMPILE_JARS} ${COLD_ASSET_FILES}
    COMMENT "compiling the client and packing its image"
    VERBATIM
)

set(COLD_BOOTSTRAP_RC "${CMAKE_CURRENT_BINARY_DIR}/bootstrap.rc")
configure_file(bootstrap/bootstrap.rc.in "${COLD_BOOTSTRAP_RC}" @ONLY)
set_source_files_properties("${COLD_BOOTSTRAP_RC}" PROPERTIES
    OBJECT_DEPENDS "${COLD_MEMORY_LOADER_CLASS};${COLD_IMAGE_ZIP}")

add_library(stone SHARED
    bootstrap/Main.cpp
    "${COLD_BOOTSTRAP_RC}"
    "${COLD_MEMORY_LOADER_CLASS}"
    "${COLD_IMAGE_ZIP}"
)
set_target_properties(stone PROPERTIES OUTPUT_NAME "stone" PREFIX "")
target_include_directories(stone PRIVATE
    "${COLD_JAVA_HOME}/include"
    "${COLD_JAVA_HOME}/include/win32"
)
target_link_libraries(stone PRIVATE user32)
target_compile_definitions(stone PRIVATE WIN32_LEAN_AND_MEAN NOMINMAX UNICODE _UNICODE)
if(MSVC)
    target_compile_options(stone PRIVATE /W4 /permissive- /utf-8
        $<$<CONFIG:Release>:/O2>)
endif()

set(COLD_PAYLOAD_DLL "${COLD_DIST_DIR}/stone.dll")
file(GLOB COLD_SCRIPT_FILES CONFIGURE_DEPENDS "${CMAKE_SOURCE_DIR}/scripts/*.lua")
set(COLD_LOADER_RC "${CMAKE_CURRENT_BINARY_DIR}/injector.rc")
set(COLD_LOADER_HPP "${CMAKE_CURRENT_BINARY_DIR}/generated/EmbeddedScripts.generated.hpp")
set(COLD_LOADER_STAMP "${CMAKE_CURRENT_BINARY_DIR}/loader-embed.stamp")
add_custom_command(
    OUTPUT "${COLD_LOADER_RC}" "${COLD_LOADER_HPP}" "${COLD_LOADER_STAMP}"
    COMMAND "${CMAKE_COMMAND}"
        "-DCOLD_DLL=${COLD_PAYLOAD_DLL}"
        "-DCOLD_SCRIPTS_DIR=${CMAKE_SOURCE_DIR}/scripts"
        "-DCOLD_RC_IN=${CMAKE_SOURCE_DIR}/loader/injector.rc.in"
        "-DCOLD_RC_OUT=${COLD_LOADER_RC}"
        "-DCOLD_HPP_IN=${CMAKE_SOURCE_DIR}/loader/src/EmbeddedScripts.generated.hpp.in"
        "-DCOLD_HPP_OUT=${COLD_LOADER_HPP}"
        "-DCOLD_STAMP=${COLD_LOADER_STAMP}"
        -P "${CMAKE_SOURCE_DIR}/loader/ConfigureLoader.cmake"
    DEPENDS "${COLD_PAYLOAD_DLL}" ${COLD_SCRIPT_FILES}
    COMMENT "embedding payload + scripts into the loader"
    VERBATIM
)

set(COLD_IMGUI_DIR "${CMAKE_SOURCE_DIR}/infra/ext/imgui")
add_library(imgui STATIC
    "${COLD_IMGUI_DIR}/imgui.cpp"
    "${COLD_IMGUI_DIR}/imgui_draw.cpp"
    "${COLD_IMGUI_DIR}/imgui_tables.cpp"
    "${COLD_IMGUI_DIR}/imgui_widgets.cpp"
    "${COLD_IMGUI_DIR}/imgui_impl_dx11.cpp"
    "${COLD_IMGUI_DIR}/imgui_impl_win32.cpp"
)
target_include_directories(imgui PUBLIC "${COLD_IMGUI_DIR}")
target_link_libraries(imgui PUBLIC d3d11 d3dcompiler dwmapi)

add_executable(stone-injector WIN32
    loader/src/Main.cpp
    loader/src/LoaderUi.cpp
    loader/src/Inject.cpp
    loader/src/ProcessList.cpp
    loader/src/Embedded.cpp
    "${COLD_LOADER_RC}"
)
target_include_directories(stone-injector PRIVATE
    loader/src "${CMAKE_CURRENT_BINARY_DIR}/generated")
target_link_libraries(stone-injector PRIVATE imgui user32 gdi32)
target_compile_definitions(stone-injector PRIVATE
    WIN32_LEAN_AND_MEAN NOMINMAX _CRT_SECURE_NO_WARNINGS UNICODE _UNICODE)
if(MSVC)
    target_compile_options(stone-injector PRIVATE /W4 /permissive- /utf-8
        $<$<CONFIG:Release>:/O2>)
endif()
add_dependencies(stone-injector stone)

add_custom_command(TARGET stone POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_directory
        "${CMAKE_SOURCE_DIR}/scripts" "${COLD_DIST_DIR}/scripts"
    COMMENT "copying scripts/ to dist")
