// Copyright (c) 2026 Allen Blaylock
// SPDX-License-Identifier: BSD-2-Clause

#ifndef SVGREPRO_WINDOW_H
#define SVGREPRO_WINDOW_H

#include <QWidget>

class QTextEdit;

class SvgReproWindow : public QWidget
{
   CS_OBJECT(SvgReproWindow)

 public:
   SvgReproWindow();

 private:
   QTextEdit *m_textEdit;
};

#endif
