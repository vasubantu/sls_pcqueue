/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/

package com.collaterate.pcqueue;

import software.amazon.awscdk.App;
import org.json.JSONObject;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class InfraApp {
    public static void main(final String[] args) {
        App app = new App();

        String context = System.getenv("CDK_CONTEXT_JSON");
        JSONObject obj = new JSONObject(context);
        String env = obj.getString("env");

        new InfraStack(app, "collateratepcqueuecdk-"+env, StackProps.builder()
                .env(Environment.builder().account("538493872512").region("us-east-1").build())
                .build());

//        new InfraStack(app, "collateratepcqueuecdk", StackProps.builder()
//                .build());
        app.synth();
    }
}

