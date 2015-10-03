docker run \
  -p 9200:9200 \
  --rm \
  --name=elasticsearch \
  -l SERVICE_APPLICATION=ELASTICSEARCH_API \
  -l SERVICE_TYPE=http.json \
elasticsearch
