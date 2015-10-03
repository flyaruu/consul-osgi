#!/bin/bash
docker run -p 8080:8080 --rm -v "/dev/urandom:/dev/random" --name tomcat1 tomcat
