/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/

package com.collaterate.pcqueue;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class InfraApp {
    public static void main(final String[] args) {
        App app = new App();

        new InfraStack(app, "collateratepcqueuecdk", StackProps.builder()
                .build());

        app.synth();
    }
}

