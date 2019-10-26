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

The UI is deployed with Netlify.

```bash
$ lein do clean, sass once, cljsbuild once min
$ netlify deploy --dir=dist --prod
```
