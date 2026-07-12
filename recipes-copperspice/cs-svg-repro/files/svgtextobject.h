/***********************************************************************
*
* Copied from KitchenSink 2.1.0 (src/svgtextobject.h)
* Copyright (c) 2012-2026 Barbara Geller
* Copyright (c) 2012-2026 Ansel Sermersheim
* Copyright (c) 2015 The Qt Company Ltd.
*
* Released under the BSD 2-Clause license
* https://opensource.org/licenses/BSD-2-Clause
*
***********************************************************************/

#ifndef SVGTEXTOBJECT_H
#define SVGTEXTOBJECT_H

#include <QTextFormat>
#include <QTextObjectInterface>

class SvgTextObject : public QObject, public QTextObjectInterface
{
   CS_OBJECT(SvgTextObject)
   CS_INTERFACES(QTextObjectInterface)

 public:
   QSizeF intrinsicSize(QTextDocument *doc, int posInDocument, const QTextFormat &format) override;
   void drawObject(QPainter *painter, const QRectF &rect,
         QTextDocument *doc, int posInDocument, const QTextFormat &format) override;
};

#endif
