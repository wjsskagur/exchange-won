#!/bin/sh
#
# Gradle 시작 스크립트 (Unix/macOS/Linux)
# 이 파일을 통해 Gradle을 별도 설치 없이 실행할 수 있음 (Wrapper 패턴)
# 첫 실행 시 gradle-wrapper.properties에 명시된 버전을 자동 다운로드
#

##############################################################################
# Environment validation
##############################################################################
die () {
    echo
    echo "ERROR: $*"
    echo
    exit 1
} >&2

# JAVA_HOME 또는 java 명령어 위치 확인
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    if ! command -v java > /dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found."
    fi
fi

# 스크립트 위치 기준으로 프로젝트 루트 탐색
APP_BASE_NAME="${0##*/}"
APP_HOME=$( cd "${0%"$APP_BASE_NAME"}" > /dev/null 2>&1 && pwd -P ) || exit

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec "$JAVACMD" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
