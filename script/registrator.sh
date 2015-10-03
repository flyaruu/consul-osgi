#!/bin/sh 
docker run --volume=/var/run/docker.sock:/tmp/docker.sock -h 192.168.99.102 registrator:v7-dev consul://192.168.99.102:8500?prefix=serviceAttributes
