#!/bin/bash
docker run -P \
  -l SERVICE_NAME=tomcat \
  -l SERVICE_APPLICATION=MY_JSON_API \
  -l SERVICE_TYPE=http.json \
  --rm -v "/dev/urandom:/dev/random" \
  --name tomcat1 tomcat
