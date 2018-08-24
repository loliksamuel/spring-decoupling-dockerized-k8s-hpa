# Autoscaling Spring Boot with the Horizontal Pod Autoscaler and custom metrics on Kubernetes
 see https://medium.freecodecamp.org/how-to-scale-microservices-with-message-queues-spring-boot-and-kubernetes-f691b7ba3acf

## prepare the cluster

You should have minikube installed.

You should start minikube with at least 4GB of RAM:

```bash
minikube start \
  --memory 8192 \
  --extra-config=controller-manager.horizontal-pod-autoscaler-upscale-delay=1m \
  --extra-config=controller-manager.horizontal-pod-autoscaler-downscale-delay=2m \
  --extra-config=controller-manager.horizontal-pod-autoscaler-sync-period=10s
```

 Run this command to configure your shell:
```bash
cd ..
eval $(minikube docker-env)
```

 validate the cluster exists   : 
```sh
kubectl config current-context 
or 
kubectl cluster-info
or 
kubectl get nodes
```
> If you're using a pre-existing minikube instance, you can resize the VM by destroying it an recreating it. Just adding the `--memory 4096` won't have any effect.

You should install `jq` — a lightweight and flexible command-line JSON processor.

You can find more [info about `jq` on the official website](https://github.com/stedolan/jq).



## create application image
make images and package the application as a container with:
```bash
docker build -t spring-boot-hpa .
```

## Deploying the application
choose 1 of the 3 options:
  1. docker run spring-boot-hpa  -d -p 80:80
  2. docker-compose up -d
  3. kubernetes - (k8s) 
      Deploy the application in Kubernetes with:
(to convert docker-compose.yml to kubernetes, u can use "kompose convert")
```bash
kubectl delete deployments --all &&  kubectl delete pods   --all &&  kubectl delete services --all
kubectl create -f kube/all.yaml
```
Deploy the Metrics Server in the `kube-system` namespace:

```bash
cd monitoring
kubectl create -f ./metrics-server
```

After 1 minute the metric-server starts reporting CPU and memory usage for nodes and pods.

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
 
minikube dashboard
```

##  play with the application
1. You can visit the kubernetes dashboard at http://minkube_ip:30000
2. You can visit the application backend  at http://minkube_ip:31000
3. You can visit the application frontend at http://minkube_ip:32000
4. You should be able to see the number of pending messages (jobs) at http://minkube_ip:32000/metrics and from the custom metrics endpoint:

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

when u r done with app. stop the cluster: using 
```sh
minikube stop 
or 
kops delete cluster --name=useast1.k8s.appychip.vpc --yes
```

## Issues

- bug in monitoring
- bug in hpa 