# Autoscaling Spring Boot with the Horizontal Pod Autoscaler and custom metrics on Kubernetes
# see https://medium.freecodecamp.org/how-to-scale-microservices-with-message-queues-spring-boot-and-kubernetes-f691b7ba3acf

## Prerequisites

You should have minikube installed.

You should start minikube with at least 4GB of RAM:

```bash
minikube start \
  --memory 8192 \
  --extra-config=controller-manager.horizontal-pod-autoscaler-upscale-delay=1m \
  --extra-config=controller-manager.horizontal-pod-autoscaler-downscale-delay=2m \
  --extra-config=controller-manager.horizontal-pod-autoscaler-sync-period=10s
```

> If you're using a pre-existing minikube instance, you can resize the VM by destroying it an recreating it. Just adding the `--memory 4096` won't have any effect.

You should install `jq` — a lightweight and flexible command-line JSON processor.

You can find more [info about `jq` on the official website](https://github.com/stedolan/jq).

## Installing Custom Metrics Api

Make sure you are in the `monitoring` folder:

```bash
cd monitoring
```

Deploy the Metrics Server in the `kube-system` namespace:

```bash
kubectl create -f ./metrics-server
```

After one minute the metric-server starts reporting CPU and memory usage for nodes and pods.

View nodes metrics:

```bash
kubectl get --raw "/apis/metrics.k8s.io/v1beta1/nodes" | jq .
```

View pods metrics:

```bash
kubectl get --raw "/apis/metrics.k8s.io/v1beta1/pods" | jq .
```

Create the monitoring namespace:

```bash
kubectl create -f ./namespaces.yaml
```

Deploy Prometheus v2 in the monitoring namespace:

```bash
kubectl create -f ./prometheus
```

Deploy the Prometheus custom metrics API adapter:

```bash
kubectl create -f ./custom-metrics-api
```

List the custom metrics provided by Prometheus:

```bash
kubectl get --raw "/apis/custom.metrics.k8s.io/v1beta1" | jq .
```

Get the FS usage for all the pods in the `monitoring` namespace:

```bash
kubectl get --raw "/apis/custom.metrics.k8s.io/v1beta1/namespaces/monitoring/pods/*/fs_usage_bytes" | jq .
```

## Package the application

 Run this command to configure your shell:
```bash
cd ..
eval $(minikube docker-env)
```

make images and package the application as a container with:
```bash
docker build -t spring-boot-hpa .
```

## Deploying the application

Deploy the application in Kubernetes with:

```bash
kubectl create -f kube/all.yaml
minikube dashboard
```

## play with the application
You can visit the kubernetes dashboard at http://<minkube ip>:30000
You can visit the application backend  at http://<minkube ip>:31000
You can visit the application frontend at http://<minkube ip>:32000
You should be able to see the number of pending messages (jobs) at http://<minkube ip>:32000/metrics and from the custom metrics endpoint:

```bash
kubectl get --raw "/apis/custom.metrics.k8s.io/v1beta1/namespaces/default/pods/*/messages" | jq .
```

##  scale the application 

You can scale the application (backend workers) in proportion to the number of messages in the queue with the Horizontal Pod Autoscaler. You can deploy the HPA with:

```bash
kubectl create -f kube/hpa.yaml
```

You can send more traffic to the application with:

```bash
while true; do sleep 0.5; curl -s http://<minikube ip>:32000/submit/1; done
```

When the application can't cope with the number of icoming messages, the autoscaler increases the number of pods only every 3 minutes.

You may need to wait three minutes before you can see more pods joining the deployment with:

```bash
kubectl get pods
```

The autoscaler will remove pods from the deployment every 5 minutes.

You can inspect the event and triggers in the HPA with:

```bash
kubectl get hpa spring-boot-hpa
```

## Appendix

Using the secrets checked in the repository to deploy the Prometheus adapter is not recommended.

You should generate your own secrets.

But before you do so, make sure you install `cfssl` - a command line tool and an HTTP API server for signing, verifying, and bundling TLS certificates
                      
You can find more [info about `cfssl` on the official website](https://github.com/cloudflare/cfssl).

Once `cfssl` is installed you generate a new Kubernetes secret with:

```bash
make certs
```

You should redeploy the Prometheus adapter.


## Issues

- bug in monitoring
- bug in hpa 