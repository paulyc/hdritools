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

// Implementation file

#if defined(__INTEL_COMPILER)
# include <mathimf.h>
#else
# include <cmath>
#endif

#include "HDRImageLabel.h"

#if QT_VERSION >= 0x050000
#include <QtWidgets>
#else
#include <QtGui>
#endif
#include <QtDebug>

HDRImageLabel::HDRImageLabel(QWidget *parent, Qt::WindowFlags f) : QLabel(parent, f)
{
    // By default we want to receive events whenever the mouse moves around
    setMouseTracking(true);
}

void HDRImageLabel::mouseMoveEvent(QMouseEvent * event)
{
    // The position of the event is relative to the image: this means that we
    // don't have to worry about scrollbars!
    mouseOver(event->pos());
}

int HDRImageLabel::heightForWidth(int w) const
{
    if (pixmap() != NULL && pixmap()->size().isValid()) {
        // Get the height while keeping the aspect ratio
        return (int)floor((float(pixmap()->size().height()*w) / pixmap()->size().width())+0.5f);
    }
    else {
        return QLabel::heightForWidth(w);
    }
}
