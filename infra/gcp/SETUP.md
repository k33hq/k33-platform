# Infra setup

## Identity Platform with Cloud Run
Ref: https://cloud.google.com/run/docs/tutorials/identity-platform

## Install and initialize `gcloud`  

Ref: https://cloud.google.com/sdk/docs/quickstart  
Ref: https://cloud.google.com/sdk/docs/initializing  

Since `gcloud` is not yet setup, we have to create `gcp-service-account` in GCP web console manually and place key.json file at `infra/gcp/secrets/gcp-service-account.json`.  
```shell
gcloud components update

gcloud config configurations create $GCP_PROJECT_ID 
gcloud auth login activate-service-account --key-file=infra/gcp/secrets/gcp-service-account.json
gcloud config set project $GCP_PROJECT_ID
gcloud services enable compute.googleapis.com
gcloud config set compute/region europe-west1
gcloud config set compute/zone europe-west1-b
gcloud config set run/region europe-west1
```

Output of `gcloud config list`
```text
[auth]
service_account_use_self_signed_jwt = None
[compute]
region = europe-west1
zone = europe-west1-b
[core]
account = gcp-service-account@<GCP_PROJECT_ID>.iam.gserviceaccount.com
disable_usage_reporting = True
project = <GCP_PROJECT_ID>
[run]
region = europe-west1

Your active configuration is: [<GCP_PROJECT_ID>]
```

## Enable GCP services

Needed for dev & prod

```shell
gcloud services enable compute.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable secretmanager.googleapis.com
gcloud services enable servicemanagement.googleapis.com
gcloud services enable servicecontrol.googleapis.com
gcloud services enable endpoints.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable certificatemanager.googleapis.com
gcloud services enable firestore.googleapis.com
gcloud alpha firestore databases update --type=firestore-native
gcloud services enable storage.googleapis.com
gcloud services enable drive.googleapis.com
gcloud services enable dns.googleapis.com
```

Needed for prod only.

```shell
gcloud services enable iam.googleapis.com
gcloud services enable iamcredentials.googleapis.com
gcloud services enable cloudscheduler.googleapis.com
```

## Add docker repository to GCP Artifact Registry

```shell
gcloud services enable artifactregistry.googleapis.com

gcloud artifacts repositories list
gcloud artifacts repositories create backend --location=europe --repository-format=DOCKER
gcloud artifacts repositories create web --location=europe --repository-format=DOCKER
```

## Service Accounts

### Create service accounts for both the cloud run services.

```shell
gcloud iam service-accounts create k33-backend \
    --description="Service Account for k33-backend cloud run service" \
    --display-name="k33-backend"

gcloud iam service-accounts create k33-backend-gateway \
    --description="Service Account for k33-backend-gateway cloud run service" \
    --display-name="k33-backend-gateway"
```

### Additional roles

Assign role to service accounts so that it can report monitoring metrics
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:k33-backend-gateway@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/monitoring.metricWriter

gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:k33-backend@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/monitoring.metricWriter
```

Assign role to service accounts so that it can report traces
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:k33-backend-gateway@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/cloudtrace.agent

gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:k33-backend@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/cloudtrace.agent
```

#### For k33-backend-gateway
Assign role to service account so that it can report Endpoint stats
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:k33-backend-gateway@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/servicemanagement.serviceController
```

#### For k33-backend
Assign role to service account so that it can access GCP Secret manager.
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:k33-backend@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/secretmanager.secretAccessor
```

Assign role to service account so that it can get and create users in Firebase Authentication / Customer IAM.
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:k33-backend@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/firebaseauth.admin
```

Assign role to service account so that it can access Firebase custom tokens.
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:k33-backend@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/iam.serviceAccountTokenCreator
```

Assign role to service account so that it can access Firestore database.
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:k33-backend@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/datastore.user
```

Assign role to service account so that it can access GCS.
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:k33-backend@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/storage.objectAdmin
```

## Cloud Run Services

Check if `.env.gcp` is having correct ENV vars.  
`GCP_BACKEND_HOST` will be missing. We will get that value after `k33-backend` is deployed for the first time.

### Deploy k33-backend

This will deploy `k33-backend` without access to secrets.

```shell
gcloud services enable run.googleapis.com
gcloud services enable firestore.googleapis.com
gcloud alpha firestore databases update --type=firestore-native

./infra/gcp/deploy.sh
```

Update `GCP_BACKEND_HOST` in `.env.gcp` with hostname excluding protocol (`https://`) from service URL.

### Create and apply secrets

Add secrets to GCP Secret Manager.  
The index value in `create-secrets.sh` has to be temporarily set to 0.
Then apply secrets to `k33-backend` which deploys a new version with secrets applied.

```shell
gcloud services enable secretmanager.googleapis.com

./infra/gcp/create-secrets.sh
./infra/gcp/apply-secrets.sh
```

For safety, revert the index value in `infra/gcp/create-secrets.sh` back to last index value plus one.
This disables `create-secrets.sh`.

### For prod only: Deploy canary k33-backend

```shell
./infra/gcp/canary-deploy.sh
```

### For dev only: Deploy test endpoints used for AT 

```shell
gcloud services enable servicemanagement.googleapis.com
gcloud services enable servicecontrol.googleapis.com
gcloud services enable endpoints.googleapis.com

./infra/gcp/deploy-test-endpoints-service.sh

gcloud services enable test.api.k33.com
```

### Deploy ESPv2

Ref: https://cloud.google.com/endpoints/docs/openapi/get-started-cloud-run
See: [deploy-espv2.sh](deploy-espv2.sh)

```shell
gcloud services enable servicemanagement.googleapis.com
gcloud services enable servicecontrol.googleapis.com
gcloud services enable endpoints.googleapis.com

gcloud services enable cloudbuild.googleapis.com

./infra/gcp/deploy-espv2.sh

gcloud services enable api.k33.com
```

* Map `api.k33.com` domain to esp
  Ref: https://cloud.google.com/run/docs/mapping-custom-domains#command-line
```shell
gcloud beta run domain-mappings create --service k33-backend-gateway --domain api.k33.com
```

### For prod only: Deploy canary ESPv2

```shell
./infra/gcp/deploy-canary-espv2.sh

gcloud services enable canary.api.k33.com
```

* Map `canary.api.k33.com` domain to esp
  Ref: https://cloud.google.com/run/docs/mapping-custom-domains#command-line

```shell
gcloud beta run domain-mappings create --service k33-backend-canary-gateway --domain canary.api.k33.com
```

## Restrict access to the k33-backend only via ESP

Ref: https://cloud.google.com/run/docs/tutorials/secure-services

```shell
gcloud run services add-iam-policy-binding k33-backend \
  --member serviceAccount:k33-backend-gateway@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/run.invoker \
  --region='europe-west1'
```

Options for `gcloud run deploy`:
* `--allow-unauthenticated` for `k33-backend-gateway`
* `--no-allow-unauthenticated` for `k33-backend`

### Verification
Direct access to `k33-backend` (https://"$GCP_BACKEND_HOST"/ping) should be blocked.  
Access via esp `k33-backend-gateway` (https://api.k33.com/ping) should be allowed.

## Cron jobs using GCP scheduler invoking Cloud Run

```shell
gcloud storage buckets create gs://"$GCP_PROJECT_ID"_analytics \
  --default-storage-class=standard \
  --location=europe-west1 \
  --project "$GCP_PROJECT_ID" \
  --public-access-prevention \
  --uniform-bucket-level-access

gcloud services enable cloudscheduler.googleapis.com

gcloud scheduler jobs delete update-firebase-users-stats-job \
  --location europe-west1

gcloud scheduler jobs create http update-firebase-users-stats-job \
  --location europe-west1 \
  --schedule="55 * * * *" \
  --uri=https://canary---"$GCP_BACKEND_HOST"/admin/jobs/update-firebase-users-stats \
  --oidc-service-account-email=k33-backend-gateway@"$GCP_PROJECT_ID".iam.gserviceaccount.com   \
  --oidc-token-audience=https://"$GCP_BACKEND_HOST"
```

```shell
gcloud storage buckets create gs://"$GCP_PROJECT_ID"_sendgrid-contacts-sync \
  --default-storage-class=standard \
  --location=europe-west1 \
  --project "$GCP_PROJECT_ID" \
  --public-access-prevention \
  --uniform-bucket-level-access

gcloud services enable cloudscheduler.googleapis.com

gcloud scheduler jobs delete sync-sendgrid-contacts-job \
  --location europe-west1

gcloud scheduler jobs create http sync-sendgrid-contacts-job \
  --location europe-west1 \
  --schedule="55 11 * * *" \
  --uri=https://canary---"$GCP_BACKEND_HOST"/admin/jobs/sync-sendgrid-contacts/${SENDGRID_CONTACT_LIST_ID} \
  --http-method=put \
  --oidc-service-account-email=k33-backend-gateway@"$GCP_PROJECT_ID".iam.gserviceaccount.com   \
  --oidc-token-audience=https://"$GCP_BACKEND_HOST"
```

```shell
gcloud services enable cloudscheduler.googleapis.com

gcloud scheduler jobs delete send-offer-to-users-without-subscription-job \
  --location europe-west1

gcloud scheduler jobs create http send-offer-to-users-without-subscription-job \
  --location europe-west1 \
  --schedule="5 * * * *" \
  --uri=https://canary---"$GCP_BACKEND_HOST"/admin/jobs/send-offer-to-users-without-subscription \
  --http-method=post \
  --oidc-service-account-email=k33-backend-gateway@"$GCP_PROJECT_ID".iam.gserviceaccount.com   \
  --oidc-token-audience=https://"$GCP_BACKEND_HOST"
```

```shell
gcloud services enable cloudscheduler.googleapis.com

gcloud scheduler jobs delete generate-vault-accounts-balance-reports-job \
  --location europe-west1

gcloud scheduler jobs create http generate-vault-accounts-balance-reports-job \
  --location europe-west1 \
  --schedule="0 0 1 * *" \
  --time-zone="Europe/Oslo" \
  --uri=https://canary---"$GCP_BACKEND_HOST"/admin/jobs/generate-vault-accounts-balance-reports?mode=FETCH_AND_STORE \
  --http-method=put \
  --oidc-service-account-email=k33-backend-gateway@"$GCP_PROJECT_ID".iam.gserviceaccount.com   \
  --oidc-token-audience=https://"$GCP_BACKEND_HOST"
```

## For dev only: Run AT in GitHub Action

Create `github` service account

```shell
gcloud iam service-accounts create github \
    --description="GitHub Service Account for AT" \
    --display-name="github"
```

### Additional roles for running AT in GitHub Actions

For Firebase Custom Tokens in AT
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:github@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/firebaseauth.admin
```

To run ESPv2 for AT
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:github@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/cloudtrace.agent

gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:github@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/servicemanagement.serviceController
```

## For prod only: GCP Workload Identity Federation for GitHub Actions

Create `github` service account
```shell
gcloud iam service-accounts create github \
    --description="Service Account for GitHub Actions" \
    --display-name="github"
```

## Additional roles for deploying in GitHub Actions

To push docker images to artifact registry
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:github@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/artifactregistry.repoAdmin
```

To deploy to GCP Cloud Run
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:github@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/run.admin

gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:github@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/iam.serviceAccountUser
```

To deploy to Firebase Hosting
```shell
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member serviceAccount:github@"$GCP_PROJECT_ID".iam.gserviceaccount.com \
  --role roles/firebasehosting.admin
```

### GCP Workload Identity Federation

Create workload id pool for GitHub Actions.
```shell
gcloud iam workload-identity-pools create "github-actions-workload-id-pool" \
  --project="${GCP_PROJECT_ID}" \
  --location="global" \
  --display-name="Github Actions workload ID pool"
```

Verify Pool ID
```shell
gcloud iam workload-identity-pools describe github-actions-workload-id-pool \
  --location="global" \
  --format="value(name)"
```

It should be of this format:

    projects/<GCP project id number>/locations/global/workloadIdentityPools/github-actions-workload-id-pool

Create ID provider for workload ID pool.
```shell
gcloud iam workload-identity-pools providers create-oidc "github-workload-id-provider" \
  --project="${GCP_PROJECT_ID}" \
  --location="global" \
  --workload-identity-pool="github-actions-workload-id-pool" \
  --display-name="Github workload ID provider" \
  --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.aud=assertion.aud,attribute.repository_owner=assertion.repository_owner,attribute.repository=assertion.repository" \
  --issuer-uri="https://token.actions.githubusercontent.com"
```

Allow authentication by GitHub workload ID providers to impersonate GCP service account.

```shell
GCP_PROJECT_NUMBER=$(gcloud projects describe "${GCP_PROJECT_ID}" --format="value(projectNumber)")

gcloud iam service-accounts add-iam-policy-binding "github@${GCP_PROJECT_ID}.iam.gserviceaccount.com" \
  --project="${GCP_PROJECT_ID}" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${GCP_PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-actions-workload-id-pool/attribute.repository_owner/k33hq"
```

## Setup Cloud Armor

### Setup Global External https load balancer with Cloud Run
Ref: https://cloud.google.com/load-balancing/docs/https/setup-global-ext-https-serverless

#### Reserve an external IP address

```shell
gcloud compute addresses create k33-web-ip \
  --network-tier=PREMIUM \
  --ip-version=IPV4 \
  --global

gcloud compute addresses describe k33-web-ip \
  --format="get(address)" \
  --global
```

#### Create load balancer

* [Enable GCP Cloud CDN for our Global Load Balancer.](https://cloud.google.com/cdn/docs/setting-up-cdn-with-serverless#gcloud_1)
* [Cache modes](https://cloud.google.com/cdn/docs/using-cache-modes)
* [Configure TTL](https://cloud.google.com/cdn/docs/using-ttl-overrides#gcloud_1)
* [Enable compression](https://cloud.google.com/cdn/docs/dynamic-compression)
* [Global SSL Policy for global target HTTPS proxy](https://cloud.google.com/load-balancing/docs/use-ssl-policies)

```shell
gcloud compute network-endpoint-groups create cloud-run-neg \
  --region=europe-west1 \
  --network-endpoint-type=serverless  \
  --cloud-run-service=k33-web-gateway

gcloud compute backend-services create web-backend-service \
  --load-balancing-scheme=EXTERNAL_MANAGED \
  --enable-cdn \
  --cache-mode=FORCE_CACHE_ALL \
  --default-ttl=300 \
  --client-ttl=300 \
  --compression-mode=AUTOMATIC \
  --custom-response-header='Cache-Status: {cdn_cache_status}' \
  --custom-response-header='Cache-ID: {cdn_cache_id}' \
  --global

gcloud compute backend-services update web-backend-service \
  --default-ttl=86400 \
  --client-ttl=86400 \
  --global

gcloud compute backend-services add-backend web-backend-service \
  --global \
  --network-endpoint-group=cloud-run-neg \
  --network-endpoint-group-region=europe-west1

gcloud compute url-maps create web-url-map \
  --default-service web-backend-service

gcloud compute ssl-certificates create k33-ssl-certs \
  --domains k33.com

gcloud compute ssl-policies create web-ssl-policy \
    --profile RESTRICTED \
    --min-tls-version 1.2

gcloud compute target-https-proxies create web-https-proxy \
  --ssl-certificates=k33-ssl-certs \
  --url-map=web-url-map \
  --ssl-policy web-ssl-policy

gcloud compute forwarding-rules create web-https-fwd-rule \
  --load-balancing-scheme=EXTERNAL_MANAGED \
  --network-tier=PREMIUM \
  --address=k33-web-ip \
  --target-https-proxy=web-https-proxy \
  --global \
  --ports=443
```

### Redirect www to non-www

Add redirect in Firebase Hosting.

### Redirect http to https

Ref: https://cloud.google.com/load-balancing/docs/https/setting-up-global-http-https-redirect#for_existing_load_balancers

```shell
gcloud compute url-maps validate \
  --source infra/gcp/http-redirect-url-map.yaml

gcloud compute url-maps import http-redirect-url-map \
  --source infra/gcp/http-redirect-url-map.yaml \
  --global

gcloud compute url-maps describe http-redirect-url-map

gcloud compute target-http-proxies create http-redirect-proxy \
  --url-map=http-redirect-url-map \
  --global

gcloud compute forwarding-rules create http-redirect-forwarding-rule \
  --load-balancing-scheme=EXTERNAL_MANAGED \
  --network-tier=PREMIUM \
  --address=k33-web-ip \
  --target-http-proxy=http-redirect-proxy \
  --global \
  --ports=80

curl -v http://k33.com

gcloud compute backend-services update web-backend-service \
  --global \
  --custom-response-header='Strict-Transport-Security:max-age=31536000; includeSubDomains; preload'
```
