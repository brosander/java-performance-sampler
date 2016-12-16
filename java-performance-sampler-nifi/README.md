java-performance-sampler-nifi
--------------

This project aims to make it easier to find bottlenecks within Apache NiFi.  It is currently functional but still rough around the edges.

Copy java-performance-sampler-nifi-nar/target/java-performance-sampler-nifi-nar-0.0.1-SNAPSHOT.nar to your NiFi lib directory and start NiFi.

Create a benchmark controller service and start it.

Whenever you want to dump statistics and clear the benchmark state, stop the service and restart it.

There will be an output json file in your NIFI_HOME/benchmarks folder by default.  This needs some post-processing following the ../java-performance-sampler-analysis instructions to be more consumable.
