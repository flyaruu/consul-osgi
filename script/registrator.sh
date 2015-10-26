#!/bin/sh 
docker run --volume=/var/run/docker.sock:/tmp/docker.sock -h 192.168.99.100 registrator:v7-dev 'consul://192.168.99.100:8500?prefix=service&containerInfo=container'
