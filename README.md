# left-over.com

Repo for left over's band website. URL tbd.

## Development

Run the app locally.

```bash
$ lein do clean, cooper
```

Visit [http://localhost:3449](http://localhost:3449) in your browser.

## Deployment

The front end is automatically deployed to [netlify](https://app.netlify.com/sites/left-over-band/deploys) via githooks.

The back-end is deployed as a series of [AWS lambdas](https://console.aws.amazon.com/lambda/home?region=us-east-1#/functions).
To build the zip files (to be uploaded manually):

```bash
$ bin/lambda-build.sh
```
