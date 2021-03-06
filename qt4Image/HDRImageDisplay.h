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

#if !defined HDRIMAGEDISPLAY_H
#define HDRIMAGEDISPLAY_H

#include <QWidget>
#include <QFuture>

// ImageIO includes
#include <rgbe.h>
#include <Rgba32F.h>
#include <LDRPixels.h>
#include <ImageSoA.h>
#include <ImageComparator.h>

#include <Reinhard02.h>
#include <ToneMapperSoA.h>

#include "ImageDataProvider.h"

using namespace pcg;

// Widget to encapsulate the loading and display of the tone mapped images
class HDRImageDisplay : public QWidget {

    Q_OBJECT

private:

    // The internal representation of the HDR Image
    RGBAImageSoA hdrImage;

    // The tone mapped version of the Image
    Image<Bgra8> ldrImage;

    // The abstraction to communicate information about the images
    //ImageIODataProvider dataProvider;

    // Our nice tonemapper
    ToneMapperSoA toneMapper;

    // QImage version of the stuff, uses implicit sharing
    QImage qImage;

    // A data provider for querying info
    ImageIODataProvider dataProvider;

    // Internal state variables
    qreal scaleFactor;
    bool needsToneMap;
    TmoTechnique technique;
    Reinhard02::Params reinhard02Params;


public:

    enum HdrResult {
        NoError,
        UnknownType,
        ExceptionError,
        SizeMissmatch,
        IllegalState
    };

    HDRImageDisplay(QWidget *parent);
    virtual ~HDRImageDisplay();

    virtual QSize sizeHint() const {

        return scaleFactor*QSize(ldrImage.Width(), ldrImage.Height());
    }

    QSizePolicy sizePolicy () const {
        return QSizePolicy(QSizePolicy::Fixed, QSizePolicy::Fixed);
    }


    bool open(const QString &fileName, HdrResult * result = 0);

    bool compareTo(const QString &fileName, ImageComparator::Type compareMethod,
        HdrResult * result = 0);


    bool save(const QString & fileName);

    // Returns true if the component is currently empty
    inline bool isEmpty() const {
        Q_ASSERT(hdrImage.Width() >= 0 && hdrImage.Height() >= 0);
        return hdrImage.Width() == 0 && hdrImage.Height() == 0;
    }


    void setScale(float scale)
    {
        Q_ASSERT(scale > 0);
        scaleFactor = scale;
        QSize sizeAux = scale * sizeOrig();
        resize(sizeAux);
        needsToneMap = true;
        update();
    }

    QSize sizeOrig() const 
    {
        return QSize(ldrImage.Width(), ldrImage.Height());
    }

    float scale() const
    {
        return scaleFactor;
    }

    const ImageDataProvider & imageDataProvider() const {
        return dataProvider;
    }

    void mouseMoveEvent(QMouseEvent * event);


public slots:
    // Slots related to the tone mapping settings
    void setGamma(float gamma);
    void setExposure(float exposure);
    void setSRGB(bool enable);
    void setWhitePoint(double value);
    void setKey(double value);
    void setReinhard02(bool enabled);

    // Clipboard slots
    void copyToClipboard();

protected:
    virtual void paintEvent(QPaintEvent *event);

signals:

    // This signal is like a "mouseOver" event, sending the 
    // absolute TopDown position, taking into account any resizing
    void mouseOverPixel( QPoint pos );

private:

    static bool loadHdr(const QString & fileName, RGBAImageSoA &hdr);

};

#endif /* HDRIMAGEDISPLAY_H */
