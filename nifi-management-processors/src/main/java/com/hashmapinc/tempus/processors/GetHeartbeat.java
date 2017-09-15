/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hashmapinc.tempus.processors;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.AttributeExpression;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Tags({"Device Management, Heatbeat, CPU, Memory, Heap, Stack, Thread, Class, Virtual, Swap"})
@CapabilityDescription("Gets the current timestamp of the System as a heartbeat and writes it to flowFile." +
        "In addition to that you can add dynamic properties in Processor to fetch CPU usage of system, Memory usage of system," +
        " Virtual Memory committed, Swap usage of System, Heap Usage by JVM, Stack Usage by JVM, " +
        "Threads related details of JVM, Total Classes loaded in JVM.")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class GetHeartbeat extends AbstractProcessor {

    private final double KB = 1024;
    private final double MB = KB * 1024;
    private final double GB = MB * 1024;

    public static final PropertyDescriptor STORAGE_UNIT = new PropertyDescriptor
            .Builder().name("STORAGE_UNIT")
            .displayName("Storage Unit")
            .description("Specify the storage unit.")
            .required(true)
            .allowableValues("KB","MB","GB")
            .defaultValue("MB")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("SUCCESS")
            .description("Success")
            .build();

    public static final Relationship FAILURE = new Relationship.Builder()
            .name("FAILURE")
            .description("Failure")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;
    private volatile Set<String> dynamicPropertNames = new HashSet<>();

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(STORAGE_UNIT);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS);
        relationships.add(FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {
        return new PropertyDescriptor.Builder()
                .required(false)
                .name(propertyDescriptorName)
                .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
                .allowableValues("YES","NO")
                .defaultValue("YES")
                .dynamic(true)
                .build();
    }

    @Override
    public void onPropertyModified(final PropertyDescriptor descriptor, final String oldValue, final String newValue) {
        if (descriptor.isDynamic()) {
            final Set<String> newDynamicPropertNames = new HashSet<>(dynamicPropertNames);
            if (newValue == null) {
                newDynamicPropertNames.remove(descriptor.getName());
            } else if (oldValue == null) {
                newDynamicPropertNames.add(descriptor.getName());
            }
            this.dynamicPropertNames = Collections.unmodifiableSet(newDynamicPropertNames);
        }
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {

        JSONObject jsonObject = new JSONObject();
        com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();

        jsonObject.put("lastTimeDataReceived", getTimeStamp());

        double storageUnit = getStorageUnit(context);
        for (final PropertyDescriptor descriptor : context.getProperties().keySet()) {
            if (!descriptor.isDynamic()) {
                continue;
            }
            if (descriptor.getName().toUpperCase().contains("CPU") && context.getProperty(descriptor).getValue().equals("YES")) {
                jsonObject.put(descriptor.getName(), os.getSystemCpuLoad() * 100);
            } else if (descriptor.getName().toUpperCase().contains("MEMORY") && context.getProperty(descriptor).getValue().equals("YES")) {
                double memoryUsage = (os.getTotalPhysicalMemorySize() - os.getFreePhysicalMemorySize()) / storageUnit;
                jsonObject.put(descriptor.getName(), memoryUsage);
            } else if (descriptor.getName().toUpperCase().contains("SWAP") && context.getProperty(descriptor).getValue().equals("YES")) {
                double swapUsage = (os.getTotalSwapSpaceSize() - os.getFreeSwapSpaceSize()) / storageUnit;
                jsonObject.put(descriptor.getName(), swapUsage);
            } else if (descriptor.getName().toUpperCase().contains("HEAP") && context.getProperty(descriptor).getValue().equals("YES")) {
                double heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / storageUnit;
                jsonObject.put(descriptor.getName(), heapUsage);
            } else if (descriptor.getName().toUpperCase().contains("STACK") && context.getProperty(descriptor).getValue().equals("YES")) {
                double stackUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() / storageUnit;
                jsonObject.put(descriptor.getName(), stackUsage);
            } else if (descriptor.getName().toUpperCase().contains("VIRTUAL") && context.getProperty(descriptor).getValue().equals("YES")) {
                jsonObject.put(descriptor.getName(), os.getCommittedVirtualMemorySize()/storageUnit);
            } else if (descriptor.getName().toUpperCase().contains("THREAD") && context.getProperty(descriptor).getValue().equals("YES")) {
                jsonObject.put(descriptor.getName(), ManagementFactory.getThreadMXBean().getThreadCount());
            } else if (descriptor.getName().toUpperCase().contains("CLASS") && context.getProperty(descriptor).getValue().equals("YES")) {
                jsonObject.put(descriptor.getName(), ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
            } else {
                getLogger().error("Parameter " + context.getProperty(descriptor).getValue() + " is not supported");
            }
        }

        //Write to flowfile
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            flowFile = session.create();
        }
        try {
            flowFile = session.write(flowFile, new OutputStreamCallback() {
                @Override
                public void process(OutputStream outputStream) throws IOException {
                    outputStream.write(jsonObject.toString().getBytes());
                }
            });
            session.transfer(flowFile, SUCCESS);
        } catch (ProcessException ex) {
            getLogger().error("Error in Object Data : " + ex);
            session.transfer(flowFile, FAILURE);
        }
    }

    private double getStorageUnit(ProcessContext context) {
        switch (context.getProperty(STORAGE_UNIT).getValue()) {
            case "KB" :
                return KB;
            case "MB" :
                return MB;
            case "GB" :
                return GB;
        }
        return MB;
    }
    private String getTimeStamp() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        return df.format(new Date());
    }
}
