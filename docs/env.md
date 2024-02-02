# Loading secrets in .env files from 1password

* For `dev`
```shell
ENV=dev op inject -i .env.template -o .env
```

* For `prod`
```shell
ENV=prod op inject -i .env.template -o .env
```
