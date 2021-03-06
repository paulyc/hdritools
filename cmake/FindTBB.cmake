# - Find TBB(Intel Threading Building Blocks)
# Find the native TBB includes and libraries
# This module defines the following read-only variables:
#  TBB_INCLUDE_DIR - where to find tbb/task.h, etc.
#  TBB_LIBRARIES   - libraries to link against to use TBB.
#  TBB_FOUND       - if false, do not try to use TBB.
# 
# These variables alter the behavior of the module when defined before calling
# find_package(TBB):
#  TBB_ROOT_DIR - Base location of the TBB distribution (e.g where the files
#                 were unzipped)
#
#=============================================================================
# Edgar Vel�zquez-Armend�riz, Cornell University (cs.cornell.edu - eva5)
# Distributed under the OSI-approved MIT License (the "License")
# 
# Copyright (c) 2008-2010 Program of Computer Graphics, Cornell University
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#=============================================================================


# Sugar for Linux dependent stuff
if(${CMAKE_SYSTEM_NAME} STREQUAL "Linux")
  set(TBB_LINUX TRUE)
endif()

find_path(TBB_INCLUDE_DIR tbb/task.h
  HINTS ${TBB_ROOT_DIR}/include
  PATHS /usr/include
        /opt/include
)
find_file(TBB_STDDEF_H tbb_stddef.h
  HINTS ${TBB_INCLUDE_DIR}
  PATH_SUFFIXES tbb Headers
  DOC "TBB Header with version information"
  NO_CMAKE_ENVIRONMENT_PATH
  NO_CMAKE_PATH
)
mark_as_advanced(TBB_INCLUDE_DIR TBB_STDDEF_H)
  
# Set the actual locations for Windows
if(WIN32)
  if(CMAKE_CL_64)
    set(TBB_PLATFORM em64t intel64)
  else()
    set(TBB_PLATFORM ia32)
  endif()
  
  if(MSVC_VERSION)
    # MSVC_VERSION:
    #   1500 == vc9
    #   1600 == vc10
    #   1700 == vc11
    #   1800 == vc12
    # **1900 == vc14**, ...
    if(MSVC_VERSION LESS 1900)
      set(VC_VER_DELTA 600)
    else()
      set(VC_VER_DELTA 500)
    endif()
    math(EXPR VC_VER_MAJOR "(${MSVC_VERSION} - ${VC_VER_DELTA}) / 100")
    math(EXPR VC_VER_MINOR "(${MSVC_VERSION} - ${VC_VER_MAJOR}*100 - ${VC_VER_DELTA}) / 10")
    if (${VC_VER_MINOR} EQUAL 0)
      set(TBB_COMPILER "vc${VC_VER_MAJOR}")
    else()
      set(TBB_COMPILER "vc${VC_VER_MAJOR}.${VC_VER_MINOR}")
    endif()
    unset(VC_VER_DELTA)
    unset(VC_VER_MAJOR)
    unset(VC_VER_MINOR)
  endif()
  
  foreach(platform ${TBB_PLATFORM})
    list(APPEND TBB_LIB_SEARCH "${TBB_ROOT_DIR}/${platform}/${TBB_COMPILER}")
    list(APPEND TBB_LIB_SEARCH "${TBB_ROOT_DIR}/lib/${platform}/${TBB_COMPILER}")
  endforeach()
  
endif()

# On linux the paths are slightly different
if(TBB_LINUX)
  # To match the TBB makefile, find the output of uname -m (cmake provides uname -p)
  execute_process(COMMAND uname -m
    OUTPUT_VARIABLE UNAME_M
    ERROR_QUIET
    OUTPUT_STRIP_TRAILING_WHITESPACE)

  if(${UNAME_M} STREQUAL "i686")
    set(TBB_PLATFORM ia32)
  elseif(${UNAME_M} STREQUAL "ia64")
    set(TBB_PLATFORM itanium)
  elseif(${UNAME_M} STREQUAL "x86_64")
    set(TBB_PLATFORM em64t intel64)
  elseif(${UNAME_M} STREQUAL "sparc64")
    set(TBB_PLATFORM sparc)
  else()
    message(FATAL_ERROR "Unknown machine type: ${UNAME_M}")
  endif()

  # This works only with gcc so far
  execute_process(COMMAND pwd)
  execute_process(COMMAND ${CMAKE_HOME_DIRECTORY}/cmake/tbb_runtime.sh
    OUTPUT_VARIABLE TBB_RUNTIME
    ERROR_QUIET
    OUTPUT_STRIP_TRAILING_WHITESPACE)

  foreach(platform ${TBB_PLATFORM})
    list(APPEND TBB_LIB_SEARCH "${TBB_ROOT_DIR}/${platform}/${TBB_RUNTIME}")
    list(APPEND TBB_LIB_SEARCH "${TBB_ROOT_DIR}/lib/${platform}/${TBB_RUNTIME}")
  endforeach()

  # Also add the default build locations
  if (EXISTS "${TBB_ROOT_DIR}/build")
    foreach(platform ${TBB_PLATFORM})
      list(APPEND TBB_LIB_SEARCH ${TBB_ROOT_DIR}/build/linux_${platform}_icc_${TBB_RUNTIME}_release)
      list(APPEND TBB_LIB_SEARCH ${TBB_ROOT_DIR}/build/linux_${platform}_icc_${TBB_RUNTIME}_debug)
      list(APPEND TBB_LIB_SEARCH ${TBB_ROOT_DIR}/build/linux_${platform}_gcc_${TBB_RUNTIME}_release)
      list(APPEND TBB_LIB_SEARCH ${TBB_ROOT_DIR}/build/linux_${platform}_gcc_${TBB_RUNTIME}_debug)
    endforeach()
  endif()

elseif(APPLE)
  # Fixed path with the commercial-aligned binary release
  set(TBB_LIB_SEARCH "${TBB_ROOT_DIR}/lib" 
                     "${TBB_ROOT_DIR}/ia32/cc4.0.1_os10.4.9")

endif()



# Try to get the compile-time version
if(TBB_INCLUDE_DIR AND TBB_STDDEF_H)
  file(READ "${TBB_STDDEF_H}" TBB_STDDEF_H_TXT)
  if (TBB_STDDEF_H_TXT MATCHES "#define TBB_VERSION_MAJOR ([0-9]+).*#define TBB_VERSION_MINOR ([0-9]+)")
    set(TBB_VERSION_MAJOR ${CMAKE_MATCH_1})
    set(TBB_VERSION_MINOR ${CMAKE_MATCH_2})
    set(TBB_VERSION "${TBB_VERSION_MAJOR}.${TBB_VERSION_MINOR}")
  endif()
elseif(TBB_INCLUDE_DIR)
  message(WARNING "The TBB version is too old to extract the version.")
endif()



include(FindReleaseAndDebug)

# Tries to find the required libraries
FIND_RELEASE_AND_DEBUG(TBB_MAIN NAMES tbb DBG_SUFFIXES _debug 
  PATHS ${TBB_LIB_SEARCH})
FIND_RELEASE_AND_DEBUG(TBB_MALLOC NAMES tbbmalloc DBG_SUFFIXES _debug
  PATHS ${TBB_LIB_SEARCH})


if (TBB_VERSION)
  FIND_PACKAGE_HANDLE_STANDARD_ARGS(TBB
    REQUIRED_VARS TBB_INCLUDE_DIR TBB_MAIN_LIBRARY TBB_MALLOC_LIBRARY
    VERSION_VAR TBB_VERSION)
else()
  FIND_PACKAGE_HANDLE_STANDARD_ARGS(TBB
    DEFAULT_MSG TBB_INCLUDE_DIR TBB_MAIN_LIBRARY TBB_MALLOC_LIBRARY)
endif()


if(TBB_FOUND)
  set(TBB_LIBRARIES ${TBB_MAIN_LIBRARY} ${TBB_MALLOC_LIBRARY} )
  if(TBB_LINUX)
    list(APPEND TBB_LIBRARIES "rt")
  endif()
endif()
