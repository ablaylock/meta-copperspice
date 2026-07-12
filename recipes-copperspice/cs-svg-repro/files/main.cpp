// Copyright (c) 2026 Allen Blaylock
// SPDX-License-Identifier: BSD-2-Clause

#include "svgrepro_render.h"
#include "svgrepro_window.h"

#include <QApplication>
#include <QString>

#include <cstdio>
#include <cstdlib>

int main(int argc, char *argv[])
{
   QString dumpDir;
   int iterations = 1;

   // parse plain argv before the QApplication constructor runs:
   // CS 2.1.0 QApplication::arguments() returns argv[1] duplicated into
   // every later position, and the constructor may also rewrite argv
   for (int i = 1; i < argc; ++i) {
      QString arg = QString::fromUtf8(argv[i]);

      if (arg == "--dump" && i + 1 < argc) {
         dumpDir = QString::fromUtf8(argv[++i]);

      } else if (arg == "--iterations" && i + 1 < argc) {
         iterations = std::atoi(argv[++i]);

         if (iterations < 1) {
            std::fprintf(stderr, "cs-svg-repro: --iterations requires a positive integer\n");
            return 2;
         }

      } else {
         std::fprintf(stderr, "usage: cs-svg-repro [--dump <dir> [--iterations <n>]]\n");
         return 2;
      }
   }

   QApplication app(argc, argv);

   if (! dumpDir.isEmpty()) {
      QString errorMsg;

      if (! SvgRepro::dumpAll(dumpDir, iterations, errorMsg)) {
         std::fprintf(stderr, "cs-svg-repro: %s\n", errorMsg.toUtf8().constData());
         return 1;
      }

      return 0;
   }

   SvgReproWindow window;
   window.show();

   return app.exec();
}
