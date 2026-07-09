#include <QApplication>
#include <QDialog>
#include <QFile>
#include <QTextStream>

#include "ui_hello.h"

int main(int argc, char *argv[])
{
   QApplication app(argc, argv);

   QDialog dialog;
   Ui::HelloDialog ui;
   ui.setupUi(&dialog);

   // loading the greeting from the qrc resource proves the rcc output
   // works at runtime; the .ui form proves the uic output works
   QFile greeting(":/greeting.txt");

   if (greeting.open(QFile::ReadOnly)) {
      QTextStream stream(&greeting);
      ui.greetingLabel->setText(stream.readAll().trimmed());
   }

   dialog.show();

   return app.exec();
}
