// Copyright (c) 2026 Allen Blaylock
// Insertion sequence copied from KitchenSink 2.1.0 src/svg_view.cpp
// Copyright (c) 2012-2026 Barbara Geller, (c) 2012-2026 Ansel Sermersheim
// SPDX-License-Identifier: BSD-2-Clause

#include "svgrepro_window.h"
#include "svgrepro_render.h"
#include "svgtextobject.h"

#include <QFile>
#include <QTextCharFormat>
#include <QTextCursor>
#include <QTextEdit>
#include <QVBoxLayout>

SvgReproWindow::SvgReproWindow()
{
   m_textEdit = new QTextEdit;
   m_textEdit->setFontPointSize(12.0);

   QObject *svgInterface = new SvgTextObject;
   m_textEdit->document()->documentLayout()->registerHandler(SvgRepro::SvgTextFormat, svgInterface);

   QTextCursor cursor = m_textEdit->textCursor();

   for (const QString &resource : SvgRepro::svgResources()) {
      QFile file(resource);

      if (! file.open(QIODevice::ReadOnly)) {
         cursor.insertText("could not open " + resource + "\n");
         continue;
      }

      QImage buffer = SvgRepro::renderRaw(file.readAll());

      cursor.insertText(resource + "\n");

      QTextCharFormat svgCharFormat;
      svgCharFormat.setObjectType(SvgRepro::SvgTextFormat);
      svgCharFormat.setProperty(SvgRepro::SvgImageId, buffer);

      cursor.insertText(QString(QChar::ObjectReplacementCharacter), svgCharFormat);
      cursor.insertText("\n");
   }

   m_textEdit->setTextCursor(cursor);

   QVBoxLayout *mainLayout = new QVBoxLayout;
   mainLayout->addWidget(m_textEdit);
   setLayout(mainLayout);

   setWindowTitle("SVG Corruption Repro");
   resize(560, 700);
}
