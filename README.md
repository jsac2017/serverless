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

## Usage

 1. 
