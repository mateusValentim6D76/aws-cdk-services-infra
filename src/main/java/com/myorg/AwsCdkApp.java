package com.myorg;


import software.amazon.awscdk.App;

public class AwsCdkApp {
    public static void main(final String[] args) {

        var app = new App();

        var vpcStack = new VpcStack(app, "mv-vpc01");

        var clusterStack = new ClusterStack(app, "mv-cluster01", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack);

        var rdsStack = new RdsStack(app, "mv-rds01", vpcStack.getVpc());
        rdsStack.addDependency(vpcStack);

        var snsStack = new SnsStack(app, "mv-sns01");

        var invoiceAppStack = new InvoiceAppStack(app, "mv-invoice-app01");

        var service01Stack = new Service01Stack(app, "mv-service01",
                clusterStack.getCluster(),
                snsStack.getProductEventsTopic(),
                invoiceAppStack.getBucket(),
                invoiceAppStack.getS3InvoiceQueue());
        service01Stack.addDependency(clusterStack);
        service01Stack.addDependency(rdsStack);
        service01Stack.addDependency(snsStack);
        service01Stack.addDependency(invoiceAppStack);


        var dynamoDbStack = new DynamoDbStack(app, "mv-dynamo-db");

        var service02Stack = new Service02Stack(app, "mv-service02", clusterStack.getCluster(), snsStack.getProductEventsTopic(), dynamoDbStack.getProductEventsDynamoDB());
        service02Stack.addDependency(clusterStack);
        service02Stack.addDependency(snsStack);
        service02Stack.addDependency(dynamoDbStack);

        app.synth();
    }
}

//cdk deploy --parameters mv-rds01:databasePassword=admin mv-rds01
