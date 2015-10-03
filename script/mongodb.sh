#!/bin/bash
docker run --name some-mongo \
  -P \
  -l SERVICE_27017_NAME=some.mongodb_instance \
  -l SERVICE_27017_TYPE=driver.mongo \
  -l SERVICE_27017_TAGS=master \
  --rm \
  mongo

