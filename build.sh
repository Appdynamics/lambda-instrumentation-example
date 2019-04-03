cp ./lambda_graph.json ./src/main/resources/
mvn install:install-file -Dfile=./lib/appdynamics-aws-lambda-java-tracer-4.5.0.jar -DgroupId=com.appdynamics.serverless.tracers.aws.api -DartifactId=appdynamics-aws-lambda-java-tracer -Dversion=4.5.0 -Dpackaging=jar
mvn clean package