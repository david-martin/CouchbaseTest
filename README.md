# Setup

## Couchbase Server

```bash
docker run   --ulimit nofile=70000:70000   --ulimit core=100000000:100000000   --ulimit memlock=100000000:100000000   -p 8091-8094:8091-8094   -p 11210:11210   --rm   -v $PWD/couchbase_var:/opt/couchbase/var   couchbase/server:community-5.0.1
```

* setup server using web ui at http://localhost:8091
* create admin user called `admin` with password `password`
* setup bucket called `mobile_bucket`
* create a standard user called `test` and password `password`, with * access to buckets

## Sync Gateway

```bash
docker run   -p 4984-4985:4984-4985   --rm   -v $PWD:/tmp/config   couchbase/sync-gateway:1.5.1-community     -adminInterface     :4985     /tmp/config/nc-gateway-config.json
```


curl -vX POST http://localhost:4985/todos/_user/ \
  -H 'Content-Type: application/json' \
  -d '{"name": "adminaccount", "password": "password"}'

## Android App

Open the 'CouchbaseTest' folder in Android Studio.
Modify the below static to be the IP/Host of where the sync gateway is running, and the Mobile Device/Emulator can reach it at.

*MainActivity.java*
```java
private static final String SYNC_GATEWAY_HOST = "http://192.168.1.5:4984"
```

Build & Run it on device or an emulator.
Filter Logcat by 'D/app' to see a new document being created.
You should see the same document back in the couchbase server


# couchbase notes

By default, Sync Gateway uses a built-in, in-memory server called "Walrus" that can withstand most prototyping use cases, extending support to at most one or two users. In a staging or production environment, you must connect each Sync Gateway instance to a Couchbase Server cluster.


https://forums.couchbase.com/t/authentication-error-when-trying-to-connect-sync-gateway-to-couchbase-server/14559/2

With Role Based Access Control (RBAC) newly introduced in Couchbase Server 5.0, you’ll need to create a new user with authorized access to the bucket. Choose the Security > Add User option in the Couchbase Server Admin and select the Bucket Full Access and Read Only Admin roles.
Start Sync Gateway with the following configuration file.
```
{
“databases”: {
“db”: {
“bucket”: “my-bucket”,
> “username”: “my-user”,
> “password”: “my-password”,
“server”: “http://localhost:8091”,
“enable_shared_bucket_access”: true,
“import_docs”: “continuous”
}
}
}```
There are two properties to keep in mind. The enable_shared_bucket_access property is used to disable the default behaviour. And the import_docs property to specify that this Sync Gateway node should perform import processing of incoming documents. Note that in a clustered environment, only 1 node should use the import_docs property.
