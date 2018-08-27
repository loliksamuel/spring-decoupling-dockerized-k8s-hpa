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
docker   rmi    spring-boot-hpa3
docker build -t spring-boot-hpa3 .
```

## Deploying the application
choose 1 of the 3 options:
  1. docker run spring-boot-hpa3  -d -p 80:80
  2. docker-compose up -d
  3. k8s - (Deploy the application in Kubernetes ) 
  ```bash
  $ cd k8s 
  $ kubectl delete namespace monitoring app && kubectl delete deployments --all &&  kubectl delete pods   --all &&  kubectl delete services --all
  $ kubectl apply -f namespaces.yaml,application/all.yaml,metrics-server,prometheus,custom-metrics-api,grafana
  $ minikube dashboard
  to see kubernetes resources
  
  ``` 
   u can also deploy 1 step at a time
(note: to convert docker-compose.yml to kubernetes, u can use "kompose convert")
```bash
$ kubectl delete deployments --all &&  kubectl delete pods   --all &&  kubectl delete services --all
$ kubectl create -f application/all.yaml
```
Deploy the Metrics Server in the `kube-system` namespace:

```bash
$ kubectl create -f ./metrics-server
After 1 minute the metric-server starts reporting CPU and memory usage for nodes and pods.
```

```bash
$ kubectl get --raw "/apis/metrics.k8s.io/v1beta1/nodes" | jq .
View nodes metrics:
```

```bash
$ kubectl get --raw "/apis/metrics.k8s.io/v1beta1/pods" | jq .
View pods metrics
```

```bash
$ kubectl create -f ./namespaces.yaml
Create the monitoring namespace
```

```bash
$ kubectl create -f ./prometheus
Deploy Prometheus v2 in the monitoring namespace
```

```bash
$ kubectl create -f ./custom-metrics-api
 Deploy the Prometheus custom metrics API adapter:
```

```bash
$ kubectl get --raw "/apis/custom.metrics.k8s.io/v1beta1" | jq .
 get List the custom metrics provided by Prometheus
```

```bash
$ kubectl get --raw "/apis/custom.metrics.k8s.io/v1beta1/namespaces/monitoring/pods/*/fs_usage_bytes" | jq .
 Get the FS usage for all the pods in the `monitoring` namespace
```

```bash
$ kubectl create -f ./grafana
 Deploy the grafana
```


##  play with the application
### play locally
1. http://localhost:8080         : 
2. http://localhost:8080/metrics : metrics of queue 
3. http://localhost:8161/admin/browse.jsp?JMSDestination=mainQueue : active mq console ,admin admin  and validate messages
### play in docker-compose cluster
1. http://localhost:31000
2. http://localhost:32000
3. http://localhost:31000/metrics
4. http://localhost:32000/metrics
### play in kubectl cluster
1. http://192.168.99.100:30000   : kubernetes dashboard
2. http://192.168.99.100:31000   : application backend
3. http://192.168.99.100:32000   : application frontend
4. http://192.168.99.100:31190   : prometheus monitoring
5. http://192.168.99.100:30003   : grafana monitoring
6. https://192.168.99.100:8443   : kubernetes-apiservers
7. You should be able to see the number of pending messages (jobs) at http://minkube_ip:32000/metrics and from the custom metrics endpoint:

```bash
kubectl get --raw "/apis/custom.metrics.k8s.io/v1beta1/namespaces/default/pods/*/messages" | jq .
```

##  scale the application 
You can scale the application (backend workers) in proportion to the number of messages in the queue with the Horizontal Pod Autoscaler. You can deploy the HPA with:

```bash
kubectl create -f application/hpa.yaml
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
kubectl get hpa spring-boot-hpa3
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
kubectl delete namespace app monitoring
or
minikube stop 
or 
kops delete cluster --name=useast1.k8s.appychip.vpc --yes
```

## Issues

- bug in monitoring
- bug in hpa 

## Debugging
```sh
remote dubugging listenning on docker port(8000)
docker logs <container_id>
kubectl get events
kubectl get pods --namespace app
kubectl logs backend-dff7f9579-brhbd   --namespace app
kubectl logs frontend-6f555ff497-22kp4 --namespace app
```