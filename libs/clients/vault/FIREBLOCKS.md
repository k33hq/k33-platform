# Fireblocks API

## Data model

![Data Model](https://files.readme.io/29869aa-FB_Object_Model_.jpg)

## Auth

https://developers.fireblocks.com/docs/quickstart#generating-a-csr-file

```shell
openssl req -new -newkey rsa:4096 -nodes -keyout fireblocks_secret.key -out fireblocks.csr -subj "/CN=K33 Backend Fireblocks Certificate"
```
## JWT

https://developers.fireblocks.com/reference/signing-a-request-jwt-structure