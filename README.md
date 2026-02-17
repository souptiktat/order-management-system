Developer → GitHub → Jenkins → Docker → AWS EC2

Developer → Git Push
↓
Jenkins Pipeline
↓
1. Build (Maven)
2. Test (JUnit + JaCoCo)
3. Build Docker Image
4. Push to Registry
5. Deploy GREEN to K8s
6. Health Check
7. Switch Service (Blue → Green)
8. Optional Cleanup
   ↓
   Kubernetes Cluster

How Deployment Flow Works

kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f hpa.yaml
kubectl apply -f ingress.yaml

Users → Service → Blue Pods (v1)
→ Green Pods (v2)

Deploy Blue (Production)
kubectl apply -f blue-deployment.yaml
kubectl apply -f service.yaml
Users hit Blue (v1)

Deploy Green (New Version)
kubectl apply -f green-deployment.yaml
Users hit Blue (v1)

Test internally:
kubectl port-forward deployment/springboot-green 8081:8080

Switch Traffic
Update Service selector → version: green
kubectl apply -f service.yaml
Traffic now routed to Green.

Remove Blue (Optional)
kubectl delete deployment springboot-blue

Canary Deployment
Traffic Shift Strategy
To increase canary:
kubectl scale deployment springboot-canary --replicas=3
kubectl scale deployment springboot-stable --replicas=7
This approximates 30% traffic.


Using Helm chart Deployment Flow

Step 1 — Deploy Blue (Current Production)
helm install myapp-blue ./springboot-app \
--set deploymentColor=blue \
--set image.tag=v1

Step 2 — Deploy Green (New Version)
helm install myapp-green ./springboot-app \
--set deploymentColor=green \
--set image.tag=v2

Step 3 — Switch Traffic to Green
helm upgrade myapp-green ./springboot-app \
--set deploymentColor=green \
--set image.tag=v2

Or explicitly:
helm upgrade myapp-blue ./springboot-app \
--set deploymentColor=green

Traffic switches instantly.
Zero downtime.

Rollback
If something fails:
helm rollback myapp-blue 1
Instant Rollback
This is why Helm is powerful.

🧠 How This Works Architecturally
User → Service
↓
color: blue
OR
color: green

Service selector decides active version.
Both deployments always exist.