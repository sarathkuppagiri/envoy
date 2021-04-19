#!/bin/sh
java  ${XMS} ${XMX} ${JAVA_OPTS} -jar /app.jar &
envoy -c /etc/service-envoy.yaml --service-cluster {APP_NAME}
