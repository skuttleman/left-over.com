# left-over.com

Repo for left over's band website. URL tbd.

## Development

Run the app locally.

```bash
$ lein do clean, cooper
```

Visit [http://localhost:3449](http://localhost:3449) in your browser.

## Deployment

### API
The API is deployed to heroku:

```bash
$ lein do clean, uberjar
$ heroku deploy:jar target/website-0.1.0-SNAPSHOT-standalone.jar --app left-over-api
```

### UI

The UI is automatically deployed with Netlify.
[![Netlify Status](https://api.netlify.com/api/v1/badges/54e3151e-a0cd-4b95-b8dc-39186253e134/deploy-status)](https://app.netlify.com/sites/left-over-band/deploys)
