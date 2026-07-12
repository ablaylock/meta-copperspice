// Copyright (c) 2026 Allen Blaylock
// SPDX-License-Identifier: BSD-2-Clause

#include "svgrepro_render.h"
#include "svgrepro_window.h"

#include <QApplication>
#include <QString>
#include <QStringList>

#include <cstdio>
#include <cstdlib>

int main(int argc, char *argv[])
{
   QApplication app(argc, argv);

   QString dumpDir;
   int iterations = 1;

   QStringList args = app.arguments();

   for (int i = 1; i < args.size(); ++i) {
      if (args[i] == "--dump" && i + 1 < args.size()) {
         dumpDir = args[++i];

      } else if (args[i] == "--iterations" && i + 1 < args.size()) {
         iterations = std::atoi(args[++i].toUtf8().constData());

         if (iterations < 1) {
            std::fprintf(stderr, "cs-svg-repro: --iterations requires a positive integer\n");
            return 2;
         }

      } else {
         std::fprintf(stderr, "usage: cs-svg-repro [--dump <dir> [--iterations <n>]]\n");
         return 2;
      }
   }

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
