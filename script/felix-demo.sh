#!/bin/bash
docker run -it --rm -p 8080:8080  \
  --name felix.demo dexels/felix:1.0.0
