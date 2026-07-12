/***********************************************************************
*
* Copied from KitchenSink 2.1.0 (src/svgtextobject.cpp)
* Copyright (c) 2012-2026 Barbara Geller
* Copyright (c) 2012-2026 Ansel Sermersheim
* Copyright (c) 2015 The Qt Company Ltd.
*
* Released under the BSD 2-Clause license
* https://opensource.org/licenses/BSD-2-Clause
*
***********************************************************************/

#include "svgtextobject.h"
#include "svgrepro_render.h"

#include <QImage>
#include <QPainter>
#include <QSizeF>

QSizeF SvgTextObject::intrinsicSize(QTextDocument *, int, const QTextFormat &format)
{
   QImage bufferedImage = format.property(SvgRepro::SvgImageId).value<QImage>();

   QSize size = bufferedImage.size();
   size *= 150.0 / (double) size.height();

   return QSizeF(size);
}

void SvgTextObject::drawObject(QPainter *painter, const QRectF &rect, QTextDocument *, int, const QTextFormat &format)
{
   QImage bufferedImage = format.property(SvgRepro::SvgImageId).value<QImage>();
   painter->drawImage(rect, bufferedImage);
}
