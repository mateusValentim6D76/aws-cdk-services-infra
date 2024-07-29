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

       var service01Stack = new Service01Stack(app,"mv-service01", clusterStack.getCluster());
       service01Stack.addDependency(clusterStack);
        service01Stack.addDependency(rdsStack);
        app.synth();


    }
}

