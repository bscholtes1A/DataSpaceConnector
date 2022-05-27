# HowTo deploy the connector using Minikube and Kubernetes

## Prerequisites

1. install Minikube
2. start Minikube: `minikube start`
3. enable Minikube's ingress addon: `minikube addons enable ingress`
4. use Minikube's docker daemon: `eval $(minikube -p minikube docker-env)`. This will direct all docker commands to
   Minikube's docker daemon.
5. create DNS entry for Minikube's IP:
   `sudo echo "$(minikube ip)  minikube" >> /etc/hosts`

#### Build connector jar file

`./gradlew clean :launchers:basic:shadowJar`

#### Create Docker image

`docker build -t edc-basic:latest launchers/basic`

This creates the image in Minikube's docker daemon, so that Kubernetes
has access to it.

#### Deploy Kubernetes resources
- Deploy app + service: `kubectl apply -f connector.yaml`
- Create Ingresses: `kubectl apply -f ingress.yaml`


## Access the deployed connector

#### URL rewrites
The following URL rewrites were specified. This means the left side is what
must be specified eg. in `curl`, the right side is what is targeted:

- `/management/*` -> `/api/v1/data/`
- `/*` -> `/api/check/`
- `/ids` -> `/api/v1/ids/`

#### Examples
This means, in order to access `/api/check/health` we need to type
```bash
curl -ik http://minikube/health
```

in order to access the data management api (GET all policies) we need to type 
```bash
curl -ik http://minikube/management/policies
```