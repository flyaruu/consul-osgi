#!/bin/bash
curl -XPOST 'http://docker.local:9200/logstash-1/example' -d '{ "name" : "Something","@timestamp":"2015-10-01T05:10:26.336Z" }'

