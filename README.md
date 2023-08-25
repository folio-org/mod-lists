# mod-lists

mod-lists is responsible for persisting the meta-data and the contents (IDs) of the lists.

## Architecture
The "mod-lists" module is responsible for handling lists within the system. It provides a set of REST endpoints that enable users to perform CRUD (Create, Read, Update, Delete) operations on lists. To efficiently query the data, the module leverages the "lib-fqm-query-processor" library, which streamlines the process of querying the underlying data storage, such as a database or file system.
## Compiling
```bash
mvn clean install
```
## Environment Variables
| Name                                | Default Value            | Description                           |
|-------------------------------------|--------------------------|---------------------------------------|
| DB_HOST                             | localhost                | Postgres hostname                     |
| DB_PORT                             | 5432                     | Postgres port                         |
| DB_HOST_READER                      | localhost                | Postgres hostname                     |
| DB_PORT_READER                      | 5432                     | Postgres port                         |
| DB_USERNAME                         | postgres                 | Postgres username                     |
| DB_PASSWORD                         | postgres                 | Postgres password                     |
| DB_DATABASE                         | postgres                 | Postgres database name                |
| server.port                         | 8081                     | Server port                           |
| MAX_LIST_SIZE                       | 1250000                  | max size of each list                 |
| LIST_EXPORT_BATCH_SIZE              | 1000                     | batch size for exports                |
| LIST_APP_S3_BUCKET                  | list-exports-us-west-2   | Name of the S3 bucket for exports     |
| AWS_REGION                          | us-west-2                | region of the S3 bucket               |
| AWS_URL                             | https://s3.amazonaws.com | end point for the S3 bucket           |
| USE_AWS_SDK                         | false                    | Use the AWS SDK for S3 access         |                     |
| S3_ACCESS_KEY_ID                    | -                        | access key for the S3 bucket          |
| S3_SECRET_ACCESS_KEY                | -                        | secret key for the S3 bucket          |
| spring.task.execution.pool.max-size | 10                       | refresh/export thread pool's max size |

> **Note:** `USE_AWS_SDK` is set to `false` when connected to minio.

## Installing the module
Follow the guide of Deploying Modules sections of the [Okapi Guide](https://github.com/folio-org/okapi/blob/master/doc/guide.md#example-1-deploying-and-using-a-simple-module) and Reference, which describe the process in detail.

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
note that the offset parameter follows a zero-based index.

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
