<h1> ZeroTune - Parallel Query Plan Generator Flink </h1>

`zerotune-plan-generation`is a Apache Flink client application to generate parallel query plans for training and testing data to be used for zero-shot learning.


## Getting Started with Parallel Query Generator

1. [Previous step: Setup Cluster](#setup)
1. [Run Arguments](#runArguments)
    - [General arguments](#generalArguments)
    - [Additional arguments](#additionalArguments)
1. [Enumeration strategy](#enumerationStrategy)
1. [Search heuristic](#searchHeuristic)
1. [Reproduce evaluations data](#reproduceEvaluationData)
1. [Next steps: Zero-shot model](https://github.com/zerotune/ZeroTune/tree/main/zerotune-learning/flink_learning)

## Setup Cluster<a name="setup"></a>

To setup a cluster and run the client follow the readme from [ZeroTune-Management Setup](https://github.com/zerotune/ZeroTune/tree/main/zerotune-management) for detailed setup instruction.

## Run Arguments<a name="runArguments"></a>

There are several run arguments available to e.g. define which data should be generated or which model the search heuristic should use. 
You can use them in the local setup environment as well as in the cluster environment. 
In the following, they are explained.

#### General Arguments<a name="generalArguments"></a>

- `logdir`: Specify where the graph- and label files get stored. The directory is required to exist. 
- `environment`: Defines in which environment PlanGeneratorFlink is running. Can be `kubernetes` for remote cluster execution, `localCluster` for local cluster execution (i.e. started by `setupPlanGeneratorFlink.sh` -> `5) Run local cluster`) or `localCommandLine` (i.e. having a local cluster setup and running PlanGeneratorFlink from the command line like calling `./bin/flink run`)
- `mode`: Defines what training/testing data should be generated. Enter one or multiple out of: `train`, `test`, `randomspikedetection`, `filespikedetection`, `smartgrid` or `advertisement`.
- `numTopos`: Defines how much queries should run per type. For training, a `numTopos` value of 3 would mean you get 3 query runs of template1, 3 of template2 and 3 of template3. For other modes it can be a little bit different. Look up `createAllQueries()` in `AbstractQueryBuilder.java` for insights.

#### Additional Arguments<a name="additionalArguments"></a>

- `debugMode`: In debugMode several things are firmly defined (i.e. seed for randomness or max count of generated source records). Sometimes helpful in the development process.
- `duration`: Duration per query in ms - including warm up time. Can be necessary to adapt for large cluster queries because resource creation can take some time. 
- `UseAllOps`: Only relevant for synthetic training data generation (-> mode = train). With this option it is assured, that every possible operator is used and its not random if a filter operator is placed or not. 
- `interceptor`: Defines if and what experiment interceptor should be used. Currently only `eventrate` is available. An interceptor changes query parameter during the data generation. 
- `interceptorStart`: Needs `interceptor` to be set. Defines the starting point of the intercepted query parameter. 
- `interceptorStep`: Needs `interceptor` to be set. Defines the step size of the intercepted query parameter. 
- `sourceParallelism`: With this option you can define the parallelism of the source operators manually. 
- `minParallelism`: Minimum Parallelism to be used. Default: 1
- `maxParallelism`: Maximum Parallelism to be used. Default: Determine max. parallelism automatically (only available on kubernetes cluster)
- `deterministic`: With this option activated, it is secured that no randomness in the query generation is used and instead predefined parameters will define the query. You can define them in `Constants.java`. This option is only available for synthetic training data generation (-> mode = train).
- `templates`: Determines which templates should be executed (Format: "--templates template1,template3" (no space between)). Only usable with synthetic training and testing templates (mode = train and mode = test). 

<hr>

### Enumeration Strategy<a name="enumerationStrategy"></a>
`enumerationStrategy`: Defines the enumeration strategy to be used. Can be `RANDOM`, `EXHAUSTIV`, `RULEBASED`, `MINAVGMAX`, `INCREASING` or `PARAMETERBASED`. 
The following runtime parameters are intended for specific enumeration strategies:
- `parallelism`: Parallelism to be used in combination with `PARAMETERBASED` enumeration strategy.
- `exhaustiveParallelismStepSize`: Parallelism step size to be used in combination with `EXHAUSTIV` enumeration strategy. Default: 1. A value of e.g. 5 would mean, that you would have the parallelisms 1, 5, 10, 15, 20, 25, 30 and so on as possible values.
- `increasingStepSize`: Set the step size to define how much the parallelism should be increased, only in combination with `INCREASING` enumeration strategy. 

#### Concept of Enumeration Strategies

- `RANDOM`
The random enumeration strategy randomly chooses a parallelism from the given inclusive boundaries i.e. available maximum number of cores in physical node. The random enumeration strategy is also used as the default strategy in Plan Generator Flink.

- `RULEBASED`
Unlike random enumeration strategy, the rule-based enumeration strategy uses the charactereistics of workload (query and workload) and physical resources such incoming event rate and operator selectivity, outgoing rate, number of cores to enumerate parallelism of upstream and downstream operators.

- `EXHAUSTIV`
The exhaustive enumeration strategy should apply every unique parallelism degree combination possible. No combination occur twice. But unfortunately the strategy is not applicable for all queries, as the number of possible combinations increases very fast with more operators or parallelism degrees. This results in enormous amount of queries necessary to fulfill a full coverage of all possible combinations.

- `MINAVGMAX`
The MinAvgMax enumeration strategy taskes the approach of generating alternately queries with the minimum, the average and the maximum number of parallelism degree.

- `INCREASING`
The increasing enumeration strategy is a simple strategy useful for creating evaluations of the influence on parallelism degrees. It starts with the minimum parallelism degree and increases the parallelism degree of each operator by one until the maximum is reached. Use the runtime parameter `increasingStepSize` to define the step size. 

- `PARAMETERBASED`
For fast testing or execution of evaluations with a constant parallelism degree, the parameter based enumeration strategy is suitable, which allows to configure the parallelism degree of all operators except the source and sink operators. Use the runtime parameter `parallelism` to define the parallelism.

<hr>

## Search Heuristic<a name="searchHeuristic"></a>
- `searchHeuristic`: Determine the optimal parallelism set by using a search heuristic approach. Based on an existing query graph file the optimal parallelism set will be determined either by running only zero-shot model predictions, by actually running queries or by predicting queries and run a subset of them. Needs `searchHeuristicFile`, `searchHeuristicZerotuneLearningPath` and `searchHeuristicModelPath`. 
- `searchHeuristicPredictions`: Only in combination with `searchHeuristic`: With this parameter search heuristic will only use predictions. It does not run actual queries as long as `searchHeuristicPredictionComparison` isn't also set.
- `searchHeuristicPredictionComparison`: Only in combination with `searchHeuristic` and `searchHeuristicPredictions`. It will use the predictions after completion to run actual querys of the 100 top, 100 medium and 100 bad predicted queries.
- `searchHeuristicFile`: Only in combination with `searchHeuristic`. Filepath to a file containing the parameters of the search heuristic query. You can use a .graph file from a previous query run. The query is rebuilt using the data in the file. Some things cannot be influenced, e.g. the placement. 
- `searchHeuristicZerotuneLearningPath`: Only in combination with `searchHeuristic`: Location of zerotune-learning implementation, e.g. /home/user/dsps/zerotune-learning/flink_learning. This is needed for preparing the actual query runs to be compared with predictions, as those query runs needs to be cleaned. 
- `searchHeuristicModelPath`: Only in combination with `searchHeuristic`: Location of the zero-shot model which should be used for predictions of the costs for parallelism sets. 
- `searchHeuristicThreshold`: Only in combination with `searchHeuristic`: Defines the threshold for binarySearch search heuristic. If the model is improving below the threshold, search stops. This is not implemented so far. 

### Search Heuristic - Optimizer

With the Search Heuristic the optimal sets of parallelism degrees can be found. For this purpose, an existing .graph file is used as a template and various sets of parallelism degrees are evaluated.
To select the sets of parallelism degrees, the existing enumeration strategies can be used.
There are three different modes in which the search heuristic can be used:
- `Only Predictions`
- `Predictions + Actual Comparison with 100 top, 100 middle, 100 bad predicted queries`
- `Predictions + Actual Comparison of all queries`

The resulting prediction calculation depends: If actual a query runs, it uses the real graph file for prediction (thus including correct selectivity but also varying placement and other factors). If it uses only predictions the graph files are artificially created and keep all values (=placement, selectivity, ...) except the parallelism degree. Therefore they are better comparable but are not 100% realistic, as e.g. selectivity can maybe change depending on the computing power.

### Setup of a Search Heuristic Node

To setup a Search Heuristic Node, additional steps to the normal Kubernetes setup are required:
- Follow the *First time setup remote kubernetes cluster environment* steps from `zerotune-management`. You can skip the first three steps if you already has setup a cluster before.
- Make sure, that the query you want to use as template is placed in `~/dsps/zerotune-plan-generation/res/searchHeuristicQueries` and the model is placed in `~/dsps/zerotune-plan-generation/res/models`
- Run `setup_learning_node_search_heuristic_local.sh` from `zerotune-learning/flink_learning` on your local computer. It will ask for the server name of the master node (node0) and the corresponding username and uploads the graph file, the model and the setup script. 
- To run the setup script which installs the learning environment, connect to the server (you can use `setupPlanGeneratorFlink.sh` -> `8) Connect to master node`) and run the script `setup_learning_node_search_heuristic_remote.sh` in your home directory. This script will ask for credentials of the git repository. 
- Now you can run the Search Heuristic and later collect the results in the `logDir`.

### `Only Predictions` Mode

To only use the zero-shot model and infer the sets of parallelism degrees, use the *Only Predictions* mode.  
This means, that no actual query will be run and the optimal set of parallelism degrees are determined by the zero-shot model only. This is the fastest mode but you don't get any comparison data to evaluate if the determined set of parallelism degrees is actually one of the best.  
No kubernetes cluster is needed for this mode, but often it is still helpful to run it on a server because for a high amount of queries (like 100 000) it can take a few hours and needs some computation power.  
To use this mode, this example *Plan Generator Flink* runtime parameter can be used:
`--logdir mnt/dsps/pgf-results --environment kubernetes --numTopos 100000 --searchHeuristic --searchHeuristicZerotuneLearningPath /mnt/dsps/zerotune-learning/flink_learning --searchHeuristicModelPath ~/flink/models/random-template1+2+3 --searchHeuristicFile ~/flink/searchHeuristicQueries/2_way_join.graph --enumerationStrategy RANDOM --sourceParallelism 10 --searchHeuristicPredictions`


**Parameter Explanation**
Make sure that `logdir` is a directory that has enough space to store all query files (see setup of a search heuristic mode if you are in doubt how to do that on cloudlab). `numTopos` defines how many queries should be predicted. If you use the exhaustive enumeration strategy, `numTopos` will be ignored.  `searchHeuristic` defines, that we are in search heuristic mode, `searchHeuristicZerotuneLearningPath` defines the path of the flink learning implementation. It's needed to use the zero-shot model to infer the costs because PlanGeneratorFlink cannot handle the zero-shot model internally. `searchHeuristicModelPath` defines the path of the zero-shot model which should be used, e.g. if it is a rule-based model or random model or maybe a model that has only been trained on a specific query structure. The referenced directory should contain the statistic files at the top and the models in subdirectories `models/model`. Normally you can just reference the original `training_data` directory. `searchHeuristicFile` defines the template query file. Use i.e. an existing .graph query file from your training data generation and if needed modify it at will. PlanGeneratorFlink rebuilds the query based on this file. `enumerationStrategy` defines in which way the parallelism degrees for the predictions are determined. In the example, the random enumeration strategy is used, so 100 000 queries that are exactly the same except the randomly choosen parallelism degrees are generated and costs get infered. `sourceParallelism` is not needed here because the random enumeration strategy always use a source parallelism of 10 but for other enumeration strategies it can be helpful to set the source parallelism explicitly. `searchHeuristicPredictions` defines that the `only predictions` mode should be used and no actual queries should run.

**Evaluation**
The results of this mode are stored in `searchHeuristic.csv`. Every row contains one query with a set of parallelism degrees. It is structured as follows:
| name | predOverallCosts | predLatency | predThroughput | parallelismSet |
|------|------------------|-------------|----------------|----------------|
| searchHeuristicArtificial-1 | 0.3 | 1127.9835 | 1229.0105 | (S0->P10) (S1->P10) (F2->P10) (S3->P4) (J4->P10) (J5->P14) (A6->P7)
searchHeuristicArtificial-0 | 0.8 | 1371.9344 | 1316.0044 | (S0->P10) (S1->P10) (F2->P10) (S3->P18) (J4->P11) (J5->P30) (A6->P17)

`name` contains the name of the artificially created .graph file. It's not relevant for an evaluation.  
`predOverallCosts` contains the calculated overall costs of the zero-shot model. Thus the model only predicts latency and throughput, those two values get combined, weighted and normalized. Remember, that you cannot compare overall costs of different runs because of the normalization.  
`predLatency` contains the predicted latency of the zero-shot model of this query.  
`predThroughput` contains the predicted throughput of the zero-shot model of this query.  
`parallelismSet` displays the set of parallelism degrees of this query. This value important for an evaluation, as it shows which parallelism set leads to the mentioned costs.  
It contains multiple brackets with an arrow inside (e.g. `(S0->P10)`). Each bracket represents an operator. The left side of the arrow is the operator name, here it is `S0` which means `Source Operator` with Id `0`. The right side of the arrow is the parallelism degree, here it is a parallelism of `10`.  
Remember, that the prediction is not 100% accurate, as no placement or other variable parameters gets adapted to the changing parallelism degrees. 



### `Predictions + Actual Comparison with 100 top, 100 middle, 100 bad predicted queries` Mode
To use the predictions with a high number of queries and only run the 100 top, 100 middle and 100 worst predicted queries, use this mode.  
First, the amount of `numTopos` queries will be predicted by the zero-shot model. Thus it's only a zero-shot model inference and no actual query run, a high `numTopos` value can be choosen to evaluate many different sets of parallelism degrees.  
After the inference, the overall costs get computed out of the predicted latencies and throughputs and an order is created from the best to the worst queries.  
To evaluate how accurate the results are, 100 from the top of the ordered list, 100 from the middle and 100 from the bottom are picked and are actually let go.  
After those 300 queries, again the model predicts those 300 queries, but this time with the real .graph file and not with the artificial one. That can have some differences, for example in placement. 


To use this mode, this example *Plan Generator Flink* runtime parameter can be used:
`--logdir mnt/dsps/pgf-results --environment kubernetes --numTopos 100000 --searchHeuristic --searchHeuristicZerotuneLearningPath /mnt/dsps/zerotune-learning/flink_learning --searchHeuristicModelPath ~/flink/models/random-template1+2+3 --searchHeuristicFile ~/flink/searchHeuristicQueries/2_way_join.graph --enumerationStrategy RANDOM --sourceParallelism 10 --searchHeuristicPredictions --searchHeuristicPredictionComparison`

**Parameter Explanation.** The parameters are mostly the same as in the first mode, but important and special is here the parameter `searchHeuristicPredictionComparison` to start the 100 top / middle / worst prediction.

**Evaluation.**  To evaluate this mode, use the `searchHeuristicWithPredictiveComparison.csv` file. 

| name | dryPredOverallCosts | dryPredLatency | dryPredThroughput | wetPredOverallCosts | wetPredLatency | wetPredThroughput | actOverallCosts | actLatency | actThroughput | dryQErrorLatency | dryQErrorThroughput | wetQErrorLatency | wetQErrorThroughput | parallelismSet |
|------|------|------|------|------|------|------|------|------|------|------|------|------|------|------|
| searchHeuristicArtificial-84703 | 0.5353029437920763 | 14475.904 | 16.767267 | 0.7646735332376113 | 19028.826 | 19.011978 | 0.23509812348581874 | 3186.908411214953 | 53.48844623886786 | 4.542303113907599 | 3.1900515593189906 | 5.970935949409853 | 2.8134077495181122 | (S0->P10) (F1->P10) (S2->P20) (F3->P51) (J4->P37) (A5->P3) (F6->P39) | 
| searchHeuristicArtificial-74542 | 0.5355062877488106 | 12319.192 | 13.613733 | 0.5995238464664622 | 13394.18 | 18.991884 | 0.2489724710082421 | 1941.7116438356165 | 48.66512819163658 | 6.344501275001331 | 3.5747085822556226 | 6.898130339035006 | 2.562417093092849 | (S0->P10) (F1->P10) (S2->P36) (F3->P10) (J4->P9) (A5->P58) (F6->P32) | 

It contains the `dry` predictions, which are based on the inference with the artifical generated graph file without the actual placement of this set of parallelism degrees. It also contains the `wet` predictions, which are based on the actual .graph file and last but not least the `act` costs, which are the actual costs for comparison.  

`name` contains the name of the .graph file. It's not relevant for an evaluation but can help with the assignment.  
`[...]overallCosts` contains the overall costs, consisting of latency and throughput, those two values get combined, weighted and normalized. Remember, that you cannot compare overall costs of different runs because of the normalization. You can also not compare the overall costs of `dry` to `wet` or `act`, because they are also normalized within there own values.  
`[...]latency` contains the latency of this query.  
`[...]throughput` contains the throughput of this query.  
`[...]QErrorLatency` contains the q-error of the actual costs to the mentioned prediction value of latency.
`[...]QErrorThroughput` contains the q-error of the actual costs to the mentioned prediction value of throughput.
`parallelismSet` displays the set of parallelism degrees of this query. This value important for an evaluation, as it shows which parallelism set leads to the mentioned costs.  
It contains multiple brackets with an arrow inside (e.g. `(S0->P10)`). Each bracket represents an operator. The left side of the arrow is the operator name, here it is `S0` which means `Source Operator` with Id `0`. The right side of the arrow is the parallelism degree, here it is a parallelism of `10`.  
Remember, that the prediction is not 100% accurate, as no placement or other variable parameters gets adapted to the changing parallelism degrees. 


### `Predictions + Actual Comparison of all queries` Mode
To use predictions and compare every prediction with an actual run use this mode. This is the most time consuming mode, as every prediction also actually runs, but it gives  exhaustive results of all combinations.  
This option makes sense for an comparison of a smaller amount of `numTopos`, for example to test the `exhaustiv` enumeration strategy on a simple query.  
First, depending on `numTopos` (or all possible combinations if `exhaustiv` enumeration strategy is used), that amount of queries actually run. Based on their graph files, the zero-shot model inferes the costs. This means, that those costs are based on the right placement and other variable parameters, like in the `wet` prediction of the previous mode.  

To use this mode, this example *Plan Generator Flink* runtime parameter can be used:
`--logdir mnt/dsps/pgf-results --environment kubernetes --searchHeuristic --searchHeuristicZerotuneLearningPath /mnt/dsps/zerotune-learning/flink_learning --searchHeuristicModelPath ~/flink/models/random-template1+2+3 --searchHeuristicFile ~/flink/searchHeuristicQueries/linear_query.graph --enumerationStrategy EXHAUSTIV --exhaustiveParallelismStepSize 5 --sourceParallelism 10`

**Evaluation.** To evaluate this mode, use the `searchHeuristic.csv`. 

| name | predOverallCosts | predLatency | predThroughput | actOverallCosts | actLatency | actThroughput | qErrorLatency | qErrorThroughput | parallelismSet |
|------|------|------|------|------|------|------|------|------|------|
10.108.69.120-searchHeuristic-70 | 0.38679306338767894 | 542.4937 | 1647.4796 | 0.08142460081600342 | 201.53195 | 1470.1066 | 2.691849708557129 | 1.1206531524658203 | (S0->P10) (A1->P25) (F2->P25)
10.108.69.120-searchHeuristic-154 | 0.09875471910291789 | 497.70862 | 2311.2031 | 0.08245057716306833 | 238.88808 | 1515.1992 | 2.0834383964538574 | 1.525346040725708 | (S0->P10) (A1->P55) (F2->P55)

`name` contains the name of the actual .graph file. It's not relevant for an evaluation but can help in some cases.  
`predOverallCosts` contains the calculated overall costs of the zero-shot model. Thus the model only predicts latency and throughput, those two values get combined, weighted and normalized. Remember, that you cannot compare overall costs of different runs because of the normalization.  
`predLatency` contains the predicted latency of the zero-shot model of this query.  
`predThroughput` contains the predicted throughput of the zero-shot model of this query.  
`actOverallCosts` contains the actual overall costs of the query that has been run. 
`actLatency` contains the actual latency of the query that has been run.
`actThroughput` contains the actual throughput of the query that has been run.
`qErrorLatency` contains the q-error of the actual costs to the prediction value of latency.
`qErrorThroughput` contains the q-error of the actual costs to the prediction value of throughput.
`parallelismSet` displays the set of parallelism degrees of this query. This value important for an evaluation, as it shows which parallelism set leads to the mentioned costs.  
It contains multiple brackets with an arrow inside (e.g. `(S0->P10)`). Each bracket represents an operator. The left side of the arrow is the operator name, here it is `S0` which means `Source Operator` with Id `0`. The right side of the arrow is the parallelism degree, here it is a parallelism of `10`.  
Remember, that the prediction is not 100% accurate, as no placement or other variable parameters gets adapted to the changing parallelism degrees. 

<hr>

## Reproduce Evaluations Data<a name="reproduceEvaluationData"></a>
The steps to reproduce an evaluation end-to-end are explained in [ZeroTune-Management readme](https://github.com/anonymoussigmod24/dsps/blob/main/zerotune-management/README.md). In ZeroTune, we have used various training and testing range for data generation and inference to evaluate model performance. Here, we mentioned some example to vary different parameters to generate data from `zerotune-plan-generation`.

### Training Data Generation

**Random enumeration strategy, linear query structure**  
`--mode train --logdir ~/pgf-results --environment kubernetes --numTopos 3000 --templates template1`

**Random enumeration strategy, 2-way join query structure**  
`--mode train --logdir ~/pgf-results --environment kubernetes --numTopos 3000 --templates template2`

**Random enumeration strategy, 3-way join query structure**  
`--mode train --logdir ~/pgf-results --environment kubernetes --numTopos 3000 --templates template3`

**Rule-Based enumeration strategy, linear query structure**  
`--mode train --logdir ~/pgf-results --environment kubernetes --numTopos 3000 --enumerationStrategy RULEBASED --templates template1`

**Rule-Based enumeration strategy, 2-way join query structure**  
`--mode train --logdir ~/pgf-results --environment kubernetes --numTopos 3000 --enumerationStrategy RULEBASED --templates template2`

**Rule-Based enumeration strategy, 3-way join query structure**  
`--mode train --logdir ~/pgf-results --environment kubernetes --numTopos 3000 --enumerationStrategy RULEBASED --templates template3`
  
### Testing Data Generation

 **Cluster Sizes**  
 `--mode train --logdir ~/pgf-results --environment kubernetes --numTopos 3000`

 **Event Rates**  
 `--mode test --logdir ~/pgf-results --environment kubernetes --numTopos 50 --templates testB`

 **Window Durations**  
 `--mode test --logdir ~/pgf-results --environment kubernetes --numTopos 100 --templates testC`

 **Window Lengths**  
 `--mode test --logdir ~/pgf-results --environment kubernetes --numTopos 100 --templates testD`

 **Tuple Widths**  
 `--mode test --logdir ~/pgf-results --environment kubernetes --numTopos 100 --templates testA`

 **Unseen Synthetic Test Queries**  
 `--mode test --logdir ~/pgf-results --environment kubernetes --numTopos 600 --templates testE`

 **Benchmark: Advertisement**  
 `--mode advertisement --logdir ~/pgf-results --environment kubernetes --numTopos 7000`

 **Benchmark: Random Spike Detection**  
 `--mode randomspikedetection --logdir ~/pgf-results --environment kubernetes --numTopos 7000`

 **Benchmark: File Spike Detection**  
 `--mode filespikedetection --logdir ~/pgf-results --environment kubernetes --numTopos 3200`

 **Benchmark: Smartgrid**  
 `--mode smartgrid --logdir ~/pgf-results --environment kubernetes --numTopos 3200`

 **Unseen Hardware**  
 `--mode train --logdir ~/pgf-results --environment kubernetes --numTopos 1500`

<hr>

 ## Tips

 - For working with search heuristic, many queries and therefore a lot of data gets generated. This can lead to hard disk space problems on Cloudlab. Instead, try to use a separate mounted volume like it's explained in the setup description of Search Heuristic.
 
 - If you use the Search Heuristic, the path to clean the results folder has likely changed to /mnt/dsps/pgf-results. You can adapt the setup scripts by changing `resultLogsDirRemotePath` in `distributedUploadAndStart.sh` (L131).