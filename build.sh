cp ./lambda_graph.json ./src/main/resources/
mvn install:install-file -Dfile=./lib/appdynamics-aws-lambda-java-tracer-1.0.1.jar -DgroupId=com.appdynamics.serverless.tracers.aws.api -DartifactId=appdynamics-aws-lambda-java-tracer -Dversion=1.0.1 -Dpackaging=jar
mvn clean package