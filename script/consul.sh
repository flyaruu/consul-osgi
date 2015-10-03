#!/bin/sh
docker run -p 8500:8500 -h $HOSTNAME progrium/consul -server -bootstrap -advertise 192.168.99.102
