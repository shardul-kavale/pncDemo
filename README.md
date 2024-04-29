# SetUp Instructions:

### Pre-requisites:
Please have Docker Desktop intalled and running on your system

### Steps:
- Clone the repo
- Open command-prompt and navigate to the root of the project
- Run `docker compose up --build`
- Allow 2-3 mins for the setup to finish. If demo-1 container stops (grey logo, instead of green), then press run icon.
- Navigate to http://localhost:9021 on your browser. Once it successfully loads, the setup is complete
  
#### The kafka setup and topic creation will be automatically handled, and you can directly start sending messages using your CMD

##### Send message to Balance:
`curl --request POST --url http://localhost:8082/topics/Balance --header "accept: application/vnd.kafka.v2+json" --header "content-type: application/vnd.kafka.avro.v2+json" --data "{\"key_schema\": \"{\\\"name\\\":\\\"key\\\",\\\"type\\\": \\\"string\\\"}\", \"value_schema_id\": \"2\", \"records\": [{\"key\" : \"11\", \"value\": {\"balanceId\": \"123\", \"accountId\" : \"99\", \"balance\" : 2.34}}]}"`

##### Send message to Customer:
`curl --request POST --url http://localhost:8082/topics/Customer --header "accept: application/vnd.kafka.v2+json" --header "content-type: application/vnd.kafka.avro.v2+json" --data "{\"key_schema\": \"{\\\"name\\\":\\\"key\\\",\\\"type\\\": \\\"string\\\"}\", \"value_schema_id\": \"1\", \"records\": [{\"key\" : \"678\", \"value\": {\"customerId\": \"678\", \"name\": \"Nicole Anne Dime\", \"phoneNumber\": \"888-888-8888\", \"accountId\": \"99\"}}]}"`

###### If facing schema errors while sending, then change value_schema_id to match your platforms, as it may be different.

You can modify accountId for the above scripts and try more times, match will only happen if the account Ids match.
Sending message to Customer first and not Balance will result in no match and the message will be send to a Dead Letter Queue.
