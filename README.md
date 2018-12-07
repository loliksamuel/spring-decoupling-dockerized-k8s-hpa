# Autoscaling Spring Boot with the Horizontal Pod Autoscaler and custom metrics on Kubernetes
 see https://medium.freecodecamp.org/how-to-scale-microservices-with-message-queues-spring-boot-and-kubernetes-f691b7ba3acf

You should install `jq` — a lightweight and flexible command-line JSON processor.
You can find more [info about `jq` on the official website](https://github.com/stedolan/jq).

## prepare the cluster 
```bash
$ brew update
$ brew install kubernetes-cli kubernetes-helm
$ brew cask install minikube
$ brew upgrade kubernetes-cli kubernetes-helm
$ brew cask upgrade minikube
$ curl -Lo minikube https://storage.googleapis.com/minikube/releases/v0.28.2/minikube-darwin-amd64 && chmod +x minikube && sudo mv minikube /usr/local/bin/
$ brew install docker
You should have minikube and kubernetes-cli installed.
$ minikube start \
  --memory 8192 \
  --extra-config=controller-manager.horizontal-pod-autoscaler-upscale-delay=1m \
  --extra-config=controller-manager.horizontal-pod-autoscaler-downscale-delay=2m \
  --extra-config=controller-manager.horizontal-pod-autoscaler-sync-period=10s \
  --vm-driver="virtualbox" --insecure-registry=192.168.99.100:80
You should start minikube with at least 4GB of RAM. If you're using a pre-existing minikube instance, you can resize the VM by destroying it an recreating it. Just adding the `--memory 4096` won't have any effect.
  

$ cd ..
$ eval $(minikube docker-env)
Run this command to configure your shell:
 
$ kubectl config current-context 
or 
$ kubectl cluster-info
or 
$ kubectl get nodes
validate the cluster exists   
```

## create application image
```bash
$ docker   rmi    spring-boot-hpa3
$ docker build -t spring-boot-hpa3:v1 .
make images and package the application as a container
$ kubectl delete namespace monitoring app  && docker container prune -f  && docker kill $(docker ps -q)&& docker ps
clean old ps, running ps and all k8s ps and k8s resource
$ docker push $DOCKER_USER_ID/spring-boot-hpa3
```

## Deploying the application 
choose 1 of the 3 options: (prefer #3)
  1. docker run 
   ```bash
  
     $ docker run  -d --name "hpa-queue"    --cpu-quota=30000 -p 61616:61616  webcenter/activemq:5.14.3
     $ docker run  -d --name "hpa-backend"  --cpu-quota=30000 -p 31000:8080 -e ACTIVEMQ_BROKER_URL=tcp://queue:61616 -e STORE_ENABLED=false -e WORKER_ENABLED=true  spring-boot-hpa3
     $ docker run  -d --name "hpa-frontend" --cpu-quota=30000 -p 32000:8080 -e ACTIVEMQ_BROKER_URL=tcp://queue:61616 -e STORE_ENABLED=true -e WORKER_ENABLED=false  spring-boot-hpa3
``` 
 
            
  2. docker-compose up
   ```bash
     $ docker-compose up -d
       for run mode  
     $ docker-compose -f docker/debug/docker-compose.yml up -d 
      for debug mode
  ``` 
  
  
  3. k8s - (Deploy the application in Kubernetes ) 
  ```bash
  $ cd k8s 
  $ kubectl delete namespace monitoring app && kubectl delete deployments --all &&  kubectl delete pods   --all &&  kubectl delete services --all
  $ kubectl apply -f namespaces.yaml,application/all.yaml
  $ kubectl apply -f metrics-server,prometheus,custom-metrics-api,grafana
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
0. minikube ip : get the ip
1. http://192.168.99.100:30000   : kubernetes dashboard
2. http://192.168.99.100:31000   : application backend ( also can be called minikube service backend  --namespace=app)
3. http://192.168.99.100:32000   : application frontend( also can be called minikube service frontend --namespace=app)
4. http://192.168.99.100:31190   : prometheus monitoring
5. http://192.168.99.100:30003   : grafana monitoring
6. https://192.168.99.100:8443   : kubernetes-apiservers
7. You should be able to see the number of pending messages (jobs) at http://minkube_ip:32000/metrics and from the custom metrics endpoint:

```bash
kubectl get --raw "/apis/custom.metrics.k8s.io/v1beta1/namespaces/default/pods/*/messages" | jq .
```

##  scale the application 

```bash
$ kubectl scale deployment backend --replicas=5
You can scale the backend application manually  

$ kubectl create -f application/hpa.yaml
You can scale the backend application automaticlly in proportion to the number of messages in the queue 
with the Horizontal Pod Autoscaler (HPA). 

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
remote dubugging listenning on docker port(50505)
docker build -t spring-boot-hpa3-debug .
docker run  -e "JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,address=*:50505,server=y,suspend=y" \
            -p 50505:50505  \ 
            -p 31000:8080  \ 
             spring-boot-hpa3-debug
docker-compose -f docker/debug/docker-compose.yml up\
docker exec -it --user root <container_id> bash 
docker logs <container_id>
http://localhost:31000
same do on frontend
kubectl get events
kubectl get svc --namespace app
kubectl get pods --namespace app -o wide
kubectl logs <pod_id> --namespace app
see Kubectl Commands Cheat Sheet
Note: Minikube can only expose Services through NodePort. The EXTERNAL-IP is always pending.

```
