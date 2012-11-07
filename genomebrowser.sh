#!/bin/bash

java -Djava.rmi.server.codebase=file:///home/weiju/Projects/ISB/gaggle2/java/gaggle-core/target/gaggle-core-2.0-SNAPSHOT.jar -cp lib/genomebrowser-core_2.9.2-1.0.jar:lib/scala-library.jar:lib/colorpicker.jar:lib/glazedlists-1.8.0_java15.jar:ui.jar:lib/commons-lang3-3.1.jar:lib/gaggle.jar:lib/sqlitejdbc-v056.jar:lib/log4j-1.2.14.jar:dist/genomebrowser.jar org.systemsbiology.genomebrowser.Main
