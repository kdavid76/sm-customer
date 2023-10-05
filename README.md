# Sport Management System - Customer Handling Service

## How to deploy Helm chart to remote repository
- First step is packaging the helm chart. Cd into the helm folder and then run the packaging
```shell
cd helm
```
```shell
helm package ./
```
This will generate a **sm-customer-VERSION-NUMBER.tgz** file in the same folder. Let's suppose the **VERSION-NUMBER** is **0.1.0**.
- Second step is uploading the generated file to repository.
```shell
curl -umikehammer1902@gmail.com:cmVmdGtuOjAxOjE3MTM3NDMyMTM6dG11VjV0cWZzZ1VHN3lXc09aMm1tMFY2QmVY -T sm-customer-0.1.0.tgz "https://sportsmanagement.jfrog.io/artifactory/bkk-helm-local/sm-customer-0.1.0.tgz"
```
If everything is fine this will generate an output similar to this one:
```json
{
  "repo" : "bkk-helm-local",
  "path" : "/sm-customer-0.1.0.tgz",
  "created" : "2023-04-23T00:20:23.374Z",
  "createdBy" : "mikehammer1902@gmail.com",
  "downloadUri" : "https://sportsmanagement.jfrog.io/artifactory/bkk-helm-local/sm-customer-0.1.0.tgz",
  "mimeType" : "application/x-gzip",
  "size" : "4063",
  "checksums" : {
    "sha1" : "00036e12b11b034140797f2d060aee62123d66c5",
    "md5" : "67ea94bc48d564e9b7d942f4f2063789",
    "sha256" : "bd39dd1a14fcb70fb79425b54fac10f61e93c70f24792fe842bcea6ebb781f78"
  },
  "originalChecksums" : {
    "sha256" : "bd39dd1a14fcb70fb79425b54fac10f61e93c70f24792fe842bcea6ebb781f78"
  },
  "uri" : "https://sportsmanagement.jfrog.io/artifactory/bkk-helm-local/sm-customer-0.1.0.tgz"
}
```
## How to install service
- First, you need to add the remote Helm repository to your local store
```shell
helm repo add bkk-helm-local https://sportsmanagement.jfrog.io/artifactory/api/helm/bkk-helm-local --username mikehammer1902@gmail.com --password cmVmdGtuOjAxOjE3MTM3NDMyMTM6dG11VjV0cWZzZ1VHN3lXc09aMm1tMFY2QmVY
helm repo update
```
- Create a **secret.yaml** and add it to the Kubernetes cluster. This secret file will contain the superuser and the Mongo database users names and passwords. This is a sample content for that file. This secret must have the name and namespace defined in the **values.yaml** at **env.userSecret.name** for the name, and **deployments.namespace** for the namespace. 
```yaml
```
- Create a **configMap.yaml** and add it to the Kubernetes cluster. This config map file must have the name **environment-config**. This secret must have the name and namespace defined in the **values.yaml** at **env.userSecret.name** for the name, and **deployments.namespace** for the namespace. The necessary environment variables are listed in the following example.
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: environment-config
  namespace: sm-customer
data:
  MONGO_URL: Mongo_database_url
  CUSTOMER_SUPER_USER_FIRSTNAME: Firstname
  CUSTOMER_SUPER_USER_LASTNAME: Lastname
  CUSTOMER_SUPER_USER_EMAIL: Firstname.Lastname@email.com
  ```
