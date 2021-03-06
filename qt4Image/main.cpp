/*============================================================================
  HDRITools - High Dynamic Range Image Tools
  Copyright 2008-2011 Program of Computer Graphics, Cornell University

  Distributed under the OSI-approved MIT License (the "License");
  see accompanying file LICENSE for details.

  This software is distributed WITHOUT ANY WARRANTY; without even the
  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the License for more information.
 ----------------------------------------------------------------------------- 
 Primary author:
     Edgar Velazquez-Armendariz <cs#cornell#edu - eva5>
============================================================================*/

#include <QApplication>

#include "ImageApp.h"

// Required when building with the static Qt
#if QT_STATICPLUGIN

#include <QtPlugin>
Q_IMPORT_PLUGIN(qjpeg)
Q_IMPORT_PLUGIN(qmng)
Q_IMPORT_PLUGIN(qico)
Q_IMPORT_PLUGIN(qtiff)

#endif /* QT_STATICPLUGIN */


int main(int argc, char *argv[])
{
#if QT_VERSION >= 0x050600
    QApplication::setAttribute(Qt::AA_EnableHighDpiScaling);
#endif
#if QT_VERSION >= 0x050000
    QApplication::setAttribute(Qt::AA_UseHighDpiPixmaps);
#endif
    ImageApp app(argc, argv);

    // The application accepts for now one optional argument: the file to load
    // (The first argument is the executable name)
    QStringList args = app.arguments();
    if (args.size() == 2) {
        app.open(args.at(1));
    }
    app.show();
    return app.exec();
}
