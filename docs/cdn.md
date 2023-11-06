# GCP Cloud CDN

* [Invalidate cache](https://cloud.google.com/cdn/docs/invalidating-cached-content#invalidate_everything)

```shell
LOAD_BALANCER_NAME="web-url-map"
gcloud compute url-maps invalidate-cdn-cache "${LOAD_BALANCER_NAME}" \
  --path "/*"
  --async
```
