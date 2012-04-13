# ============================================================================
#   HDRITools - High Dynamic Range Image Tools
#   Copyright 2008-2012 Program of Computer Graphics, Cornell University
#
#   Distributed under the OSI-approved MIT License (the "License");
#   see accompanying file LICENSE for details.
#
#   This software is distributed WITHOUT ANY WARRANTY; without even the
#   implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#   See the License for more information.
#  ---------------------------------------------------------------------------
#  Primary author:
#      Edgar Velazquez-Armendariz <cs#cornell#edu - eva5>
# ============================================================================

# Small utilities

# All the include locations
include_directories(../ImageIO)
include_directories(../3rdparty/tclap/include)
  
add_executable(exrcompress exrcompress.cpp tclapUtil.h tclapUtil.cpp)
target_link_libraries(exrcompress ImageIO)
set_target_properties(exrcompress PROPERTIES
  VERSION "${HDRITOOLS_VERSION}")

# TODO Fix this nasty hack (adds the utils to the qt4Image bundle!)
if (APPLE AND BUILD_QT4IMAGE AND QT4IMAGE_BUNDLE)
  set (DESTINATION_DIR "qt4Image.app/Contents/MacOS")
else()
  set (DESTINATION_DIR "bin")
endif()
install(TARGETS exrcompress
  RUNTIME DESTINATION ${DESTINATION_DIR} COMPONENT "utils"
)