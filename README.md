# JSAC "Online Capacity Planning in Serverless Computing"

This repository contains simulation code for the paper submitted to JSAC.

## Prerequisites

 * Recent version of [maven](https://maven.apache.org/), and recent version of 
   [Java SDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

## Preparation

 1. Clone repository:

    ```bash
    git clone https://github.com/jsac2017/serverless.git
    ```

 2. Compile the project

    ```bash
    cd serverless
    mvn package
    ```

 3. Follow [this](https://github.com/google/cluster-data) to
    download *Google Cluster* data

 4. Unpack `.csv` files in `task_events` directory

 5. Prepare the data for simulations by using the following (assuming that the
    current directory is this repository's root):

    ```bash
    cat google-cloud-sdk/task_events/part-000**-of-00500.csv | \
        java -jar ./target/*with-dependencies.jar \
        --summaries 000_100.test create-summaries
    ```

## Running simulations

All the supported command-line parameters can be learnt with `--help`:

```bash
java -jar ./target/*with-dependencies.jar --help
```

 *  To generate plots from figure 4, use the following:
    ```
    java -jar ./target/*with-dependencies.jar --summaries 000_001.test \
       --processing-uniform --vm-allocation-time 1 --deadline-disable \
       --allocation-cost 10 --buffer-size 100 --maintenance-cost 0.15 \
       --num-resources 100 --time-slot-duration 10 --value-scale 2 \
       --policy 'AAP(var)' --policy-param 1.01:20:40 \
       plot -y objective-percentage 
    ```


 *  To generate plots from figure 5 use the following:
   
    First, save common arguments:

    ```bash
    COMMON_ARGS=(--summaries 000_001.test --deadline-disable) 
    COMMON_ARGS+=(--policy 'PQ(v),AAP(1.01),AAP(3),AAP(13),')
    COMMON_ARGS+=(--policy 'Learn(v:average:25),Learn(v:median:25)')
    COMMON_ARGS+=(--policy 'Learn(v:average:100),Learn(v:median:100)')
    COMMON_ARGS+=(--processing-uniform --vm-allocation-time 1)
    ```

    Now, use the following to get plots (in `.tsv` format) in `./plots`:
    
    ```bash
    java -jar ./target/*with-dependencies.jar $COMMON_ARGS \
       --allocation-cost 10 --buffer-size 10:1000:20:exp --maintenance-cost 0.15 \
       --num-resources 100 --time-slot-duration 1 --value-scale 2 \
       plot -y objective-percentage 
    ```

    The variable should be specified in the range format: `start:end:num[:exp]`
    (`:exp` for exponential scale), only one variable is allowed.

 * To generate plots from figure 6 replace `-y` argument with `average-latency`:

    ```bash
    java -jar ./target/*with-dependencies.jar $COMMON_ARGS \
       --allocation-cost 10 --buffer-size 10:1000:20:exp --maintenance-cost 0.15 \
       --num-resources 100 --time-slot-duration 1 --value-scale 2 \
       plot -y average-latency
    ```
   
 * For the last series of plots (Figure 7) a different set of `COMMON_ARGS` is required:

    ```bash
    COMMON_ARGS=(--summaries 000_001.test) 
    COMMON_ARGS+=(--policy 'PQ(v),PQ(v/d),PQ(-d)')
    COMMON_ARGS+=(--policy 'Learn(v:average:100),Learn(v:median:100)')
    COMMON_ARGS+=(--policy 'Learn(v/d:average:100),Learn(v/d:median:100)')
    COMMON_ARGS+=(--policy 'Learn(-d:average:100),Learn(-d:median:100)')
    COMMON_ARGS+=(--processing-uniform --vm-allocation-time 1)
    ```

    The plots are generated similarly:
 
    ```bash
     java -jar ./target/*with-dependencies.jar $COMMON_ARGS \
        --allocation-cost 1 --deadline-cushions-scale 1 --maintenance-cost 0.05 \
        --num-resources 10:200:19 --time-slot-duration 10 --value-scale 2  \
        plot -y objective-percentage
    ```


