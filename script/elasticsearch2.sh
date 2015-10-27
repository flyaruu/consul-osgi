docker run \
  -p 9201:9200 \
  --rm \
  --name=elasticsearch2\
  -l SERVICE_APPLICATION=ELASTICSEARCH_API \
  -l SERVICE_TYPE=http.json \
  -l SERVICE_NAME=elasticsearch2 \
elasticsearch
