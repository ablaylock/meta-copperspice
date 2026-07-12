// Copyright (c) 2026 Allen Blaylock
// Render path copied from KitchenSink 2.1.0 src/svg_view.cpp
// Copyright (c) 2012-2026 Barbara Geller, (c) 2012-2026 Ansel Sermersheim
// SPDX-License-Identifier: BSD-2-Clause

#ifndef SVGREPRO_RENDER_H
#define SVGREPRO_RENDER_H

#include <QByteArray>
#include <QImage>
#include <QString>
#include <QStringList>
#include <QTextFormat>

namespace SvgRepro {

// text-object plumbing shared by the window and the text object handler
constexpr const int SvgTextFormat = QTextFormat::UserObject + 1;
constexpr const int SvgImageId    = 1;

// the six kitchensink SVG resources, combo-box order
QStringList svgResources();

// ":/resources/cake1.svg" -> "cake1"
QString baseName(const QString &resourcePath);

// kitchensink svg_view.cpp insertTextObject() render path, verbatim:
// the buffer is deliberately NOT cleared before rendering
QImage renderRaw(const QByteArray &svgData);

// identical except the buffer is cleared to transparent first
// (control variant; simultaneously the candidate fix)
QImage renderControl(const QByteArray &svgData);

// the svgtextobject.cpp drawObject() path: scale to 150 px height via
// QPainter::drawImage onto a white canvas
QImage renderDrawn(const QImage &buffer);

// write <name>.<iter>.{raw,control,drawn}.png for every SVG x iteration;
// returns false and sets errorMsg on the first failure
bool dumpAll(const QString &dir, int iterations, QString &errorMsg);

} // namespace SvgRepro

#endif
