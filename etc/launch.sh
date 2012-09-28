#!/bin/sh
java -cp ./bin:./lib/log4j-1.2.14.jar:./lib/colorpicker.jar:./lib/gaggle.jar:./lib/glazedlists-1.8.0_java15.jar:./lib/junit-4.4.jar:./lib/sqlitejdbc-v052.jar org.systemsbiology.genomebrowser.Main "$@"
exit 0


