#!/bin/bash

# readJarFileName.
readJar() {
    CLASSPATH=
    for DIR in $*; do
        if [ "x`ls $DIR`" != "x" ]; then
            for JAR in ` ls $DIR`; do
                FILEPATH="${DIR}/${JAR}"
                if [ ${FILEPATH##*.} = "jar" ]; then
                    if [ -f "${DIR}/${JAR}" ]; then
                        if [ "x$CLASSPATH" = "x" ]; then
                            CLASSPATH=${DIR}/${JAR}
                        else
                            CLASSPATH=$CLASSPATH:${DIR}/${JAR}
                        fi
                    fi
                fi
            done
        fi
    done
    echo $CLASSPATH
}

# out execute file name.
OUT_NAME=./ncrim

# project version.
VERSION=0.0.1

# project name.
PROJECT_NAME=rim

# project dir.
PROJECT_DIR=./project

# execute class.
EXECUTE_CLAZZ=rim.command.CreateRimCommand

# JAR_DIR.
THIS_JAR_DIR=.
JAR_DIR=${PROJECT_DIR}/lib

# libs.
LIB_FILES=`readJar ${THIS_JAR_DIR}`:`readJar ${JAR_DIR}`

# options.
OPTIONS=
OPTIONS="--no-fallback"
#OPTIONS="${OPTIONS} --verbose"
#OPTIONS="${OPTIONS} -Dgraal.LogFile=./nativeLog.log"
#OPTIONS="${OPTIONS} -H:Log=registerResource"
OPTIONS="${OPTIONS} -H:+ReportExceptionStackTraces"
OPTIONS="${OPTIONS} -H:+AddAllCharsets"
OPTIONS="${OPTIONS} -H:ReflectionConfigurationFiles=${PROJECT_DIR}/lib/reflection.json"
OPTIONS="${OPTIONS} -H:JNIConfigurationFiles=${PROJECT_DIR}/lib/jni.json"
OPTIONS="${OPTIONS} -H:ResourceConfigurationFiles=${PROJECT_DIR}/lib/resourcesLinuxAmd64.json"

rm -f ${OUT_NAME}

echo native-image -cp jar:${LIB_FILES} ${OPTIONS} ${EXECUTE_CLAZZ} ${OUT_NAME}
native-image -cp jar:${RIM_JAR_FILE}:${LIB_FILES} ${OPTIONS} ${EXECUTE_CLAZZ} ${OUT_NAME}

rm -f ${OUT_NAME}.build_artifacts.txt
