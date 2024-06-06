---
sidebar_position: 10
title: Login Configuration
---

# Pod Scorer Configuration

The Scorer is a system that rates pods and assigns them a score based on their health and performance. The score is 
used to determine which pod need attention and which pod is healthy. The score is calculated based on several Scorer
components. Scorers are configured in the `pod_scorer.yaml` file.

```yaml
alerts:
  error: 200
  warn: 100
restarts:
  enabled: true
  spread: 10
notrunning:
  enabled: true
  spread: 100
  age: 120
metrics:
  enabled: true
  cpu: 80
  cpuSpread: 5
  memory: 80
  memorySpread: 5
  spread: 90
```

The `alerts` section defines the thresholds for the error and warn alerts. The other sections configure the Scorer
components.

* restarts: The number of restarts a pod can have before it is considered unhealthy.
* notrunning: The number of seconds a pod can be not running before it is considered unhealthy.
* metrics: The thresholds for the CPU and memory usage of a pod.