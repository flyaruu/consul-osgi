#!/bin/bash
docker run -P --rm -v "/dev/urandom:/dev/random" --name tomcat1 tomcat
