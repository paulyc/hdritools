# Macro to add the Qt 4.4 static image plugins to the given target
# if it is static. Call in from the directory of the desired target
# after FindQt4 has been run.

macro(QT_ADD_IMG_PLUGINS target)

  IF(QT_CONFIG MATCHES "static")
    add_definitions(-DQT_STATICPLUGIN)
   
    # Links against the static plugins
    FIND_RELEASE_AND_DEBUG(QJPEG qjpeg qjpegd "${QT_PLUGINS_DIR}/imageformats")
    FIND_RELEASE_AND_DEBUG(QGIF qgif qgifd "${QT_PLUGINS_DIR}/imageformats")
    FIND_RELEASE_AND_DEBUG(QMNG qmng qmngd "${QT_PLUGINS_DIR}/imageformats")
    FIND_RELEASE_AND_DEBUG(QICO qico qicod "${QT_PLUGINS_DIR}/imageformats")
    FIND_RELEASE_AND_DEBUG(QTIFF qtiff qtiffd "${QT_PLUGINS_DIR}/imageformats")
  
    target_link_libraries(${target} ${QJPEG_LIBRARY} ${QGIF_LIBRARY}
      ${QMNG_LIBRARY} ${QICO_LIBRARY} ${QTIFF_LIBRARY} )
  
  endif(QT_CONFIG MATCHES "static")

endmacro(QT_ADD_IMG_PLUGINS)
