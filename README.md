# Skai README

## Introduction & motivation 
This repo is a clone of [memsql/deployment-docker](https://github.com/memsql/deployment-docker), customized for building the Skai KS base MemSQL image that is used for [building the automation image](https://jenkins-prod-search.internalk.com/job/automation-tests-db-upload/) for our local environments and testing environments on Jenkins. This process is comprised of (as of Dec 2022):
1. Starting a cluster-in-a-box memsql docker image.
2. Running several cucumbers to populate it with schemas, tables and data.
3. Committing the docker state and upload the image to an artifactory.

Then we can download the image in different automation jobs, start it and run tests that rely on the committed data.

Q: Aren't the [default SingleStore scripts](https://github.com/memsql/deployment-docker) sufficient for this use case?  
A: No, because they don't allow the retention of data after having populated the image with data and committed it.
Additionally, with the image built by the default scripts, it's impossible to start the image twice (i.e. start the committed image), due to the usage of the `sdb-toolbox` tool in the startup script.

## Differences with source repo
The main changes made to the repo are:
* Makefile: Explicitly specify our desired memsql version.
* Dockerfile-ciab: 
  * Specify the base image and other tool versions.
  * Add permissions to run installations on the image.
  * Skip the `VOLUME` command, presumably to allow data retention on the image once we use it in our Jenkins automations.
* Startup: 
  * Unregister host, as it prevents the committed image to be started again as a new docker in automation jobs.
  * Add custom memsql variable values to reduce the final committed image size.
  
## Building the base image

This section will be updated once we have created a jenkins job to do it for us.
For now, see instructions on how to create the image locally and pushing it to the artifactory in PR https://github.com/kenshoo/memsql-base-automation-image-builder/pull/1.
  
## Recommendations - discovery for next version upgrade

When it's time to upgrade from 7.6 to some new version, one of the steps will be to create a new base image for Skai's automation environment.

I suggest trying it like this:
1. Check what changes have been made on SingleStore's `deployment-docker` scripts repository since [the version we forked](https://github.com/memsql/deployment-docker/tree/c8cdbebe123d456940dd5fcb22f3b094563ca40b).  
2 methods for doing that:
    * Via Github: https://github.com/kenshoo/memsql-base-automation-image-builder/compare/master...memsql:deployment-docker:master
    * Locally:
      1. `git checkout` this repo.
      1. `git remote add -f memsql_src git@github.com:memsql/deployment-docker.git`
      1. `git remote update`
      1. `git diff master remotes/memsql_src/master`
      1. `git remote rm memsql_src`
  If they seem necessary, apply them to the repo on your local machine.
3. Try to apply the same changes that PR https://github.com/kenshoo/memsql-base-automation-image-builder/pull/1 contains (see description for summary) to the current version of the official repo. Adjust version numbers accordingly.
4. If the repo has changed so much that this PR's changes are no longer applicable, know that the main changes were skipping the 'VOLUME' command, and running `sdb-toolbox-config unregister-host`. All the others were workarounds to issues we ran into.
5. Good luck!
  
# Source Repo README
<details>

# deployment-docker

This repository contains our official deployment Docker images for various products.
If you are interested in contributing, please read `CONTRIBUTING.md`.

| Image        | SingleStore Packages Installed                                    |
|--------------|-------------------------------------------------------------------|
| ciab         | singlestoredb-server, singlestoredb-studio, singlestoredb-toolbox |
| ciab-redhat  | singlestoredb-server, singlestoredb-studio, singlestoredb-toolbox |
| dynamic-node | (none)                                                            |
| node         | singlestoredb-server                                              |
| node-redhat  | singlestoredb-server                                              |
| tools        | singlestoredb-toolbox                                             |

# Running the Cluster in a Box image

To initialize a new cluster in a box:

```bash
docker run -i --init \
    --name singlestore-ciab \
    -e LICENSE_KEY=${LICENSE_KEY} \
    -e ROOT_PASSWORD=${ROOT_PASSWORD} \
    -p 3306:3306 -p 8080:8080 \
    singlestore/cluster-in-a-box
```

To manage your cluster in a box:

```bash
To start the container:
    docker start singlestore-ciab

To read logs from the container:
    docker logs singlestore-ciab

To stop the container (must be started):
    docker stop singlestore-ciab

To restart the container (must be started):
    docker restart singlestore-ciab

To remove the container (all data will be deleted):
    docker rm singlestore-ciab
```

# Automatically run SQL when Cluster in a Box initializes

If you want to automatically run SQL commands when creating a Cluster in a Box
container, you can mount a SQL file into the Docker container like so:

```bash
docker run -i --init \
    --name singlestore-ciab \
    -e LICENSE_KEY=${LICENSE_KEY} \
    -e ROOT_PASSWORD=${ROOT_PASSWORD} \
    -v /PATH/TO/INIT.SQL:/init.sql \
    -p 3306:3306 -p 8080:8080 \
    singlestore/cluster-in-a-box
```

**Replace `/PATH/TO/INIT.SQL` with a valid path on your machine to the SQL file
you want to run when initializing Cluster in a Box.**

# Enable the HTTP API or External Functions

The [HTTP API][httpapi] and [External Functions][extfunc] features can be enabled when you create the container via passing environment variables.

**HTTP API:**

Add the following flags to your `docker run` command:

```bash
    -e HTTP_API=ON -p 9000:9000
```

By default, the HTTP API runs on port 9000. If you want to use a different port you can instead run:

```bash
    -e HTTP_API=ON -e HTTP_API_PORT=$PORT -p $PORT:$PORT
```

**External Functions:**

Add the following flag to your `docker run` command:

```bash
    -e EXTERNAL_FUNCTIONS=ON
```

[httpapi]: https://docs.singlestore.com/db/latest/en/reference/http-api.html
[extfunc]: https://docs.singlestore.com/db/latest/en/reference/sql-reference/procedural-sql-reference/create--or-replace--external-function.html
</details>
