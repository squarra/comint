# How to run

There are three different modes of running. One for development with all the beauty of quarkus hot reloading, one using docker-compose (will probably be removed soon) and one using kubernetes.

## Development with Quarkus hot-reloading

Make sure you have a RabbitMQ container running. You can either use the provided run configuration (*rabbitmq*) or run this command:
```shell
$ docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management
```

This will expose the AMQP port (5672) as well as the RabbitMQ management console (15672).

Now you can start the Quarkus application using the provided run configuration (*quarkus*) or this command:
```shell
./mvnw quarkus:dev
```

## Docker compose

Either use the provided run configuration (*docker-compose*) or run:
```shell
docker compose up
```

This will also expose every interesting port (8080, 5762, 15672) to localhost.

## Kubernetes

This is done using minikube and kubectl. You can also use Docker Desktop or any other kubernetes cluster for it.

1. Start minikube:
   ```shell
    minikube start
   ```
2. Point your terminal to use Minikube's Docker daemon (this needs to be done in each new terminal window you want to
   build images in):
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
