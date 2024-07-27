package com.myorg;

import software.amazon.awscdk.App;

public class AwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();
        var vpcStack = new VpcStack(app, "mv-Vpc01");
        var clusterStack = new ClusterStack(app, "mv-Cluster01", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack);
        app.synth();
        //teste commit

    }
}

