# mod-lists
Copyright (C) 2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

- [Introduction](#introduction)
- [Architecture](#architecture)
- [Compiling](#compiling)
- [Environment Variables](#environment-variables)
- [Installing the module](#installing-the-module)
  - [System user](#system-user)
  - [Resource requirements](#resource-requirements)
  - [Installation](#installation)
- [Deploying the module](#deploying-the-module)
- [Interacting with list-app](#interacting-with-list-app)
  - [Create a new list](#create-a-new-list)
  - [Get all the available lists](#get-all-the-available-lists)
  - [Get comprehensive details about a particular list](#get-comprehensive-details-about-a-particular-list)
  - [Update list](#update-list)
  - [Delete list](#delete-list)
  - [Refresh List](#refresh-list)
  - [Contents of a list](#contents-of-a-list)
  - [Exporting List](#exporting-list)
  - [Export Status](#export-status)
  - [Download export](#download-export)
- [Additional information](#additional-information)
  - [Issue tracker](#issue-tracker)
  - [Code of Conduct](#code-of-conduct)
  - [ModuleDescriptor](#moduledescriptor)
  - [API documentation](#api-documentation)
  - [Code analysis](#code-analysis)
  - [Download and configuration](#download-and-configuration)

## Introduction
mod-lists is responsible for persisting the metadata and the contents (IDs) of lists.

## Architecture
The "mod-lists" module is responsible for handling lists within the system. It provides a set of REST endpoints that enable users to perform CRUD (Create, Read, Update, Delete) operations on lists. To efficiently query the data, the module leverages the "lib-fqm-query-processor" library, which streamlines the process of querying the underlying data storage, such as a database or file system.
## Compiling
```bash
mvn clean install
```
## Environment Variables

| Name                                            | Default Value            | Description                                                                                                 |
|-------------------------------------------------|--------------------------|-------------------------------------------------------------------------------------------------------------|
| DB_HOST                                         | localhost                | Postgres hostname                                                                                           |
| DB_PORT                                         | 5432                     | Postgres port                                                                                               |
| DB_USERNAME                                     | postgres                 | Postgres username                                                                                           |
| DB_PASSWORD                                     | postgres                 | Postgres password                                                                                           |
| DB_DATABASE                                     | postgres                 | Postgres database name                                                                                      |
| server.port                                     | 8081                     | Server port                                                                                                 |
| MAX_LIST_SIZE                                   | 1250000                  | max size of each list                                                                                       |
| LIST_EXPORT_BATCH_SIZE                          | 1000                     | batch size for exports                                                                                      |
| LIST_APP_S3_BUCKET                              | list-exports-us-west-2   | Name of the S3 bucket for exports                                                                           |
| AWS_REGION                                      | us-west-2                | region of the S3 bucket                                                                                     |
| AWS_URL                                         | https://s3.amazonaws.com | end point for the S3 bucket                                                                                 |
| USE_AWS_SDK                                     | false                    | Use the AWS SDK for S3 access                                                                               |
| S3_ACCESS_KEY_ID                                | -                        | access key for the S3 bucket                                                                                |
| S3_SECRET_ACCESS_KEY                            | -                        | secret key for the S3 bucket                                                                                |
| OKAPI_URL                                       | http://okapi:9130        | Okapi URL, used for system user authentication/management                                                   |
| SYSTEM_USER_PASSWORD                            | -                        | Password for the system user; **must be set**                                                               |
| SYSTEM_USER_ENABLED                             | true                     | Defines if system user must be created at service tenant initialization or used for egress service requests |
| SYSTEM_USER_RETRY_WAIT_MINUTES                  | 10                       | Max time to wait for the system user to be created, which is used in the Tenant API                         |
| mod-lists.list-export.s3-startup-check.enabled  | true                     | Verify that S3/MinIO is accessible on startup                                                               |
| spring.task.execution.pool.max-size             | 10                       | refresh/export/migrate thread pool's max size                                                               |
| mod-lists.general.refresh-query-timeout-minutes | 90                       | Max time to wait for an FQL query to run during a list refresh                                              |

> **Note on CSV storage**: MinIO remote storage or Amazon S3 can be used as storage for generated CSV files.
The storage is selected by specifying the url of S3-compatible storage by using ENV variable `AWS_URL`.
`USE_AWS_SDK` is used to specify client to communicate with storage.
It requires `true` in case if S3 usage or `false` in case if MinIO usage. By default it equals to `false`.
In addition, the following ENV variables can be
specified: `AWS_REGION`, `LIST_APP_AWS_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`.
By default, the module will attempt to uploaded and delete a small file to S3/MinIO on startup, to verify that the
service is reachable. This check can be disabled by setting `mod-lists.list-export.s3-startup-check.enabled` to `false`.

## Installing the module

Follow the guide of Deploying Modules sections of the [Okapi Guide](https://github.com/folio-org/okapi/blob/master/doc/guide.md#example-1-deploying-and-using-a-simple-module) and Reference, which describe the process in detail.

### System user

As part of installation, this module creates a system user with the username `mod-lists`, password as the environment
variable `SYSTEM_USER_PASSWORD`, and permissions to interact with `mod-fqm-manager`
(as specified [here](src/main/resources/system-user-permissions.txt)).

### Resource requirements

Most operations in mod-lists use very little memory, however more memory is required when serving up list exports to
users. Additionally, sufficient memory is required for good and consistent performance.
With the default settings, you should provide mod-lists with at least 1 gigabyte of heap space. This will allow it to
perform well and export any list up to the default maximum list size. For larger instances, where mod-lists is used
heavily, give it at least 2 gigabytes. In the most extreme case (still assuming the default max list size and concurrent
task limit), where the application is fully saturated with non-stop export requests of extremely large lists that happen
to all require a lot of memory at the same instant, 5.5 gigabytes of heap space should be sufficient to maintain
performance and stability.

In addition, at least 200 megabytes of free storage space should be available on the server for mod-lists to use for
staging temporary files during the list export process.

### Installation

First of all you need a running Okapi instance. (Note that specifying an explicit 'okapiurl' might be needed.)
```bash
   cd .../okapi
   java -jar okapi-core/target/okapi-core-fat.jar dev
```
We need to declare the module to Okapi:
```bash
  curl -w '\n' -X POST -D -   \
   -H "Content-type: application/json"   \
   -d @target/ModuleDescriptor.json \
   http://localhost:9130/_/proxy/modules
```
That ModuleDescriptor tells Okapi what the module is called, what services it provides, and how to deploy it.

## Deploying the module
Next we need to deploy the module. There is a deployment descriptor in target/DeploymentDescriptor.json. It tells Okapi to start the module on 'localhost'.

Deploy it via Okapi discovery:

```bash
   curl -w '\n' -D - -s \
  -X POST \
  -H "Content-type: application/json" \
  -d @target/DeploymentDescriptor.json  \
  http://localhost:9130/_/discovery/modules
```
Then we need to enable the module for the tenant:

```bash
  curl -w '\n' -X POST -D -   \
    -H "Content-type: application/json"   \
    -d @target/TenantModuleDescriptor.json \
    http://localhost:9130/_/proxy/tenants/<tenant_name>/modules
```
## Interacting with list-app
> **Note:** This may become outdated and that the source of record for the API is src/main/resources/swagger.api/list.yaml

### Create a new list
This API endpoint allows you to create a new list.
```bash
curl \
  -H 'Accept: application/json' \
  -H 'x-okapi-tenant: {{ tenant identifier }}' \
  -H 'x-okapi-token: {{ token }}' \
  -X POST {{ base-uri }}/lists
```
Request Body
```bash
{
"name": "Example List",
"description": " Creating an example list",
"entityTypeId": "xxxxxxxxxxxxxxxxxxxxxxxxx",
"isActive": false,
"isPrivate": false,
"fqlQuery": "{\"item_status\": {\"$eq\": \"missing\"}}"
}
```
Response
* Status Code : 201 Created
* Content Type: application/json.

The response from the API will contain the ID of the new list (uuid).

### Get all the available lists
Returns all the available lists.
```bash
curl \
  -H 'Accept: application/json' \
  -H 'x-okapi-tenant: {{ tenant identifier }}' \
  -H 'x-okapi-token: {{ token }}' \
  -X GET {{ base-uri }}/lists
```
Response

* Status Code: 200 OK
* Content Type: application/json.

The response will be a JSON object containing the information about all the available lists.

### Get comprehensive details about a particular list

Returns the information of a specific list.
```bash
curl \
-H 'Accept: application/json' \
-H 'x-okapi-tenant: {{ tenant identifier }}' \
-H 'x-okapi-token: {{ token }}' \
-X GET {{ base-uri }}/lists/{{ id }}
```
Response
* Status Code: 200 OK
* Content Type: application/json.

The response will be a JSON object containing the information about the specified list.

### Update list
This API endpoint allows the user to update the list
```bash
curl \
-H 'Accept: application/json' \
-H 'x-okapi-tenant: {{ tenant identifier }}' \
-H 'x-okapi-token: {{ token }}' \
-X PUT {{ base-uri }}/lists/{{ id }}
```
Request Body

The request body should be a JSON object with the following parameters:
* name (string)
* fqlQuery (optional - string)
* isActive (boolean)
* isPrivate (boolean)
* version
* query_id (Optional. If passed, the query results will be copied to the updated list)

Response
* Status Code: 200 OK
* Content Type: application/json.

The response will contain a JSON object with the updated details of the list.

### Delete list
This API endpoint allows the user to delete a list.
```bash
curl \
-H 'Accept: application/json' \
-H 'x-okapi-tenant: {{ tenant identifier }}' \
-H 'x-okapi-token: {{ token }}' \
-X DELETE {{ base-uri }}/lists/{{ id }}
```
Response
* Status Code : 204 No Content

### Refresh List
This API endpoint allows the user to refresh a list.
```bash
curl \
-H 'Accept: application/json' \
-H 'x-okapi-tenant: {{ tenant identifier }}' \
-H 'x-okapi-token: {{ token }}' \
-X POST {{ base-uri }}/lists/{{ id }}/refresh
```
Response
* Status Code : 200 OK

The response will be UUID of the refresh request.

### Contents of a list
Get contents of a report
```bash
curl \
-H 'Accept: application/json' \
-H 'x-okapi-tenant: {{ tenant identifier }}' \
-H 'x-okapi-token: {{ token }}' \
-X GET {{ base-uri }}/lists/{{ id }}/contents?offset={{ offset }}&size={{ size }}
```

> **Note:** To paginate through the results, you can utilize the optional `offset` and `size` query parameters. It's important to
> note that the offset parameter follows a zero-based index.

Response
* Status Code : 200 OK

The response will be page of JSON records.

### Exporting List
Start an asynchronous list export.

```bash
curl \
-H 'Accept: application/json' \
-H 'x-okapi-tenant: {{ tenant identifier }}' \
-H 'x-okapi-token: {{ token }}' \
-X POST {{ base-uri }}/lists/{{ id }}/export
```
Response
* Status Code: 201
* Content type: application/json

The response will contain a JSON object which will contain the UUID of the export and other details.

### Export Status
Get status of an export request.

```bash
curl \
-H 'Accept: application/json' \
-H 'x-okapi-tenant: {{ tenant identifier }}' \
-H 'x-okapi-token: {{ token }}' \
-X GET{{ base-uri }}/lists/{{ id }}/export/{{ export-id }}
```
Response
* Status Code: 200 OK
* Content Type: application/json

The response contains the `status` of the export `SUCCESS`, `INPROGRESS` OR `FAILED`.

### Download export
Download contents (CSV) of an export

```bash
curl \
-H 'Accept: application/json' \
-H 'x-okapi-tenant: {{ tenant identifier }}' \
-H 'x-okapi-token: {{ token }}' \
-X GET{{ base-uri }}/lists/{{ id }}/export/{{ export-id }}/download
```
Response

* Status Code: 200 OK
* Content Type: text/csv

The response the csv of the downloaded list.

## Additional information

### Issue tracker

See project [MODLISTS](https://issues.folio.org/browse/MODLISTS)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Code of Conduct

Refer to the Wiki
[FOLIO Code of Conduct](https://wiki.folio.org/display/COMMUNITY/FOLIO+Code+of+Conduct).

### ModuleDescriptor

See the [ModuleDescriptor](descriptors/ModuleDescriptor-template.json)
for the interfaces that this module requires and provides, the permissions,
and the additional module metadata.

### API documentation

API descriptions:

* [OpenAPI](src/main/resources/swagger.api/list.yaml)
* [Schemas](src/main/resources/swagger.api/schemas/)

Generated [API documentation](https://dev.folio.org/reference/api/#mod-lists)

### Code analysis

[SonarQube analysis](https://sonarcloud.io/project/overview?id=org.folio%3Amod-lists).

### Download and configuration

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the Docker images for [released versions](https://hub.docker.com/r/folioorg/mod-lists/)
and for [snapshot versions](https://hub.docker.com/r/folioci/mod-lists/).

