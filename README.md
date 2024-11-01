Download and run rabbitmq docker image:

```shell
docker run -d --name rabbitmq-localhost -p 5672:5672 -p 15672:15672 rabbitmq:management
```

```shell
docker run --name comint-localhost -i --rm -p 8080:8080 comint
```

## Kubernetes

This is done using minikube and kubectl. You can also use Docker Desktop or any other kubernetes cluster for it.

1. Start minikube:
   ```shell
    minikube start
   ```
2. Point your terminal to use Minikube's Docker daemon (this needs to be done in each new terminal window you want to build images in):
   ```shell
    eval $(minikube docker-env)
   ```
3. Build your Quarkus application image in Minikube's environment (from project root):
   ```shell
    docker build -t quarkus-app:latest .
   ```
4. Apply the Kubernetes configurations (development):
   ```shell
   kubectl apply -k kubernetes/overlays/development/
   ```
5. Check if everything is starting up properly:
   ```shell
   kubectl get pods
   kubectl get services
   ```
6. Wait for all pods to show "Running" status. You can watch them with:
   ```shell
   kubectl get pods --watch
   ```
7. To access your services, get the URLs:
   ```shell
   minikube service rabbitmq --url
   minikube service quarkus-app --url
   ```

When you're done working and want to shut everything down:

```shell
kubectl delete -k kubernetes/ .
minikube stop
```

To deploy production environment:

```shell
kubectl apply -k kubernetes/overlays/production
```
