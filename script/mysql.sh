#!/bin/bash
docker run --name some-mysql \
  -P \
  -l SERVICE_TAGS=master \
  -l SERVICE_NAME=my_mysql_instance \
  -l SERVICE_TYPE=mysql \
  -e MYSQL_ROOT_PASSWORD=my-secret-pw \
  --rm \
  mysql

