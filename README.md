# left-over.com

Repo for left over's band website. URL tbd.

## Development

Run the app locally.

```bash
$ lein do clean, cooper
```

Run the tests

```bash
$ bin/tests.sh
```

Visit [http://localhost:3449](http://localhost:3449) in your browser.

## Deployment

[![Netlify Status](https://api.netlify.com/api/v1/badges/54e3151e-a0cd-4b95-b8dc-39186253e134/deploy-status)](https://app.netlify.com/sites/left-over-band/deploys)

The front end is automatically deployed to *netlify* via *githooks* whenever `master` is updated.

The back-end is deployed as a series of [AWS lambdas](https://console.aws.amazon.com/lambda/home?region=us-east-1#/functions).

To build and deploy the zip files to AWS lambda:

```bash
$ bin/deploy.sh
```
