# JMeter Test Scripts
The scripts and inputs in this project are used for performance testing of Keycloak.

## Usage guide

### Prerequisities
Download and install [JMeter](https://jmeter.apache.org/)

### Setup
1. Copy sample.properties for each environment being tested. E.g. dev.properties, test.properties and fill out required values
2. Copy sample.csv to users.csv and add a "user,password" entry for each user account in the test

### Test Plans
* authorization-code.jmx - Tests an Authorization Code flow
* client-credentials.jmx - Test a Client Credentials flow

### Test Execution
> execute-test.bat \<FILE> \<THREADS> \<ENV> \<RAMP_UP>

where:

* \<FILE> is the input file (without the .jmx extension). The output file will be named <FILE>.jtl
* \<THREADS> is the number of threads.
* \<ENV> is dev or test
* \<RAMP_UP> can be specified or defaults to the same value as \<THREADS> if not provided.


### Test Reporting
> generate-report.bat \<FILE> \<REPORT_DIRECTORY>

where:

* \<RESULTS_FILE> contains the results of the test.
* \<REPORT_DIRECTORY> is where reports will be generated to. The folder must be empty or not exist.