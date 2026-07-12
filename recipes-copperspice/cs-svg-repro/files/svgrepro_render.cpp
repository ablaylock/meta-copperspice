// Copyright (c) 2026 Allen Blaylock
// Render path copied from KitchenSink 2.1.0 src/svg_view.cpp
// Copyright (c) 2012-2026 Barbara Geller, (c) 2012-2026 Ansel Sermersheim
// SPDX-License-Identifier: BSD-2-Clause

#include "svgrepro_render.h"

#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QPainter>
#include <QSvgRenderer>

namespace SvgRepro {

QStringList svgResources()
{
   QStringList list;
   list << ":/resources/pineapple.svg"
        << ":/resources/watermelon.svg"
        << ":/resources/cake1.svg"
        << ":/resources/cake2.svg"
        << ":/resources/cup_cake.svg"
        << ":/resources/ice_cream.svg";

   return list;
}

QString baseName(const QString &resourcePath)
{
   return QFileInfo(resourcePath).baseName();
}

QImage renderRaw(const QByteArray &svgData)
{
   QSvgRenderer renderer(svgData);

   if (! renderer.isValid()) {
      return QImage();
   }

   // kitchensink svg_view.cpp:63-65 - the QImage pixel data is left
   // uninitialized on purpose, this is the suspected bug being reproduced
   QImage svgBufferImage(renderer.defaultSize(), QImage::Format_ARGB32);
   QPainter painter(&svgBufferImage);
   renderer.render(&painter, svgBufferImage.rect());

   return svgBufferImage;
}

QImage renderControl(const QByteArray &svgData)
{
   QSvgRenderer renderer(svgData);

   if (! renderer.isValid()) {
      return QImage();
   }

   QImage svgBufferImage(renderer.defaultSize(), QImage::Format_ARGB32);
   svgBufferImage.fill(Qt::transparent);

   QPainter painter(&svgBufferImage);
   renderer.render(&painter, svgBufferImage.rect());

   return svgBufferImage;
}

QImage renderDrawn(const QImage &buffer)
{
   if (buffer.isNull() || buffer.height() <= 0) {
      return QImage();
   }

   // svgtextobject.cpp intrinsicSize(): scale to 150 px height
   QSize size = buffer.size();
   size *= 150.0 / (double) size.height();

   QImage canvas(size, QImage::Format_ARGB32);
   canvas.fill(Qt::white);

   // svgtextobject.cpp drawObject(): plain drawImage into the layout rect
   QPainter painter(&canvas);
   painter.drawImage(QRectF(QPointF(0, 0), QSizeF(size)), buffer);

   return canvas;
}

// allocate and free a same-size buffer filled with opaque magenta: a
// following same-size uninitialized allocation usually lands on this
// freed block, so "uninitialized" garbage becomes deterministic and
// recognizable instead of depending on allocator luck (fresh mmap pages
// read back zeroed and would mask the bug)
static void churnHeap(const QSize &size)
{
   if (size.isEmpty()) {
      return;
   }

   QImage churn(size, QImage::Format_ARGB32);
   churn.fill(0xFFFF00FFu);
}

bool dumpAll(const QString &dir, int iterations, QString &errorMsg)
{
   QDir outDir(dir);

   if (! outDir.mkpath(".")) {
      errorMsg = "unable to create dump directory " + dir;
      return false;
   }

   for (int iter = 1; iter <= iterations; ++iter) {
      for (const QString &resource : svgResources()) {
         QFile file(resource);

         if (! file.open(QIODevice::ReadOnly)) {
            errorMsg = "unable to open resource " + resource;
            return false;
         }

         QByteArray svgData = file.readAll();

         {
            QSvgRenderer probe(svgData);

            if (! probe.isValid()) {
               errorMsg = "invalid svg " + resource;
               return false;
            }

            churnHeap(probe.defaultSize());
         }

         QImage raw = renderRaw(svgData);

         if (raw.isNull()) {
            errorMsg = "render failed for " + resource;
            return false;
         }

         QImage control = renderControl(svgData);
         QImage drawn   = renderDrawn(raw);

         QString stem = outDir.filePath(baseName(resource) + "." + QString::number(iter));

         if (! raw.save(stem + ".raw.png") || ! control.save(stem + ".control.png")
               || ! drawn.save(stem + ".drawn.png")) {
            errorMsg = "unable to save PNG set " + stem;
            return false;
         }
      }
   }

   return true;
}

} // namespace SvgRepro
