# Node.JS Monitoring

This example demonstrates how to instrument a Node.JS lambda using AppDynamics Tracer. An example `serverless.yml` file has been provided if you're using the serverless framework for deployment.

## Instrumenting your Node.JS AWS Lambda

### 1. Adding AppDynamics Tracer to your Node project

Your `package.json` should include a dependency for AppDynamics Tracer. To add the dependency automatically, run ```npm install --save appdynamics-lambda-tracer@latest```. If you prefer to manage your package.json file manually, refer to the example below for what should be added.

```json
        "dependencies": {
            "appdynamics-lambda-tracer": "^1.0.0"
        }
```

If managing the package file manually, run ```npm install``` after updating package.json.

### 2. Changing your Lambda code

To instrument you Lambda you'll have to add the following lines, and wrap the main handler module of your Lambda as follow:

```javascript 1.8
const tracer = require('js-tracer');
tracer.init();

//Your handler
module.exports.handler = (event, context, callback) => {

    //your actual handler code

};


tracer.mainModule(module);
```

### 3. Provide controller info details in form of environment variables

Your tracer need to know where to send the metrics and event. Also, you need to set an application to report to.
This example is using Serverless.com framework to deploy this lambda. [For more details about these environment variable](https://docs.appdynamics.com/display/PRO45/Set+Up+the+Serverless+APM+Environment).
If you're using serverless to deploy, update `serverless.yml` to have your controller info:

 ```yml
      environment:
        # replace these env variables with your controller info values
        APPDYNAMICS_ACCOUNT_NAME: <Your AppD Account Name>
        APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY: <Your Controller Access Key>
        APPDYNAMICS_APPLICATION_NAME:  <Your AppD Application Name>
        APPDYNAMICS_CONTROLLER_HOST: <Your Controller Host  (No https:// or http://)>
        APPDYNAMICS_SERVERLESS_API_ENDPOINT: https://[pdx,syd,fra].saas.appdynamics.com
 ```
