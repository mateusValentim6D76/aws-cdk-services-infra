package com.myorg;


import software.amazon.awscdk.App;

public class AwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        var vpcStack = new VpcStack(app, "mv-vpc01");

        var clusterStack = new ClusterStack(app, "mv-cluster01", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack);

        var rdsStack = new RdsStack(app, "mv-rds01", vpcStack.getVpc());
        rdsStack.addDependency(vpcStack);

        var snsStack = new SnsCdkStack(app, "mv-sns01");

        var service01Stack = new Service01Stack(app, "mv-service01", clusterStack.getCluster(), snsStack.getProductEventsTopic());
        service01Stack.addDependency(clusterStack);
        service01Stack.addDependency(rdsStack);
        service01Stack.addDependency(snsStack);

        var service02Stack = new Service02Stack(app, "mv-service02", clusterStack.getCluster());
        service01Stack.addDependency(clusterStack);

        app.synth();
    }
}
